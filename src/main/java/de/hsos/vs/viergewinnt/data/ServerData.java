package de.hsos.vs.viergewinnt.data;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.hash.Hashing;
import de.hsos.vs.viergewinnt.data.gameLogic.GameState;
import de.hsos.vs.viergewinnt.data.gameLogic.ViergewinntModel;
import de.hsos.vs.viergewinnt.data.userData.Account;
import jakarta.websocket.Session;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.File;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.security.*;

/**
 * Hält globale Daten auf die Sowohl Servlet als auch WebSockets zugreifen
 */
public final class ServerData {
    private static ServerData instance;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private Map<String, Account> accounts = new HashMap<>();
    private final  ConcurrentHashMap<String, String> httpSessionToAccount = new ConcurrentHashMap<>();
    private final  ConcurrentHashMap<String, String> accountToHttpSession = new ConcurrentHashMap<>();
    private final List<String> loggedInAccounts = new ArrayList<>(); // Accounts die angemledet sind
    private final static Map<String, Long> lastPings = new ConcurrentHashMap<>();


    // Websocktdata
    private final Map<String, Session> websocketSessions = new HashMap<>();
    private final Map<String, String> webSocketSessionToUsername = new HashMap<>();
    private final Map<String, String> usernameToWebSocketSession = new HashMap<>();

    // Gamedata
    private final Queue<String> queue =  new LinkedList<>(); // Warteschlange
    private final  Map<Integer, ViergewinntModel> games = new HashMap<>();
    private final Map<String, Integer> usernameToGameID = new HashMap<>();
    private int gameIDCounter;

    private final ScheduledExecutorService pingChecker = Executors.newSingleThreadScheduledExecutor();
    private final ScheduledExecutorService botChecker = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> botCheckers;

    private PrivateKey privateKey;
    private PublicKey publicKey;





    // Instanzierung
    private ServerData() {

    }

    public static ServerData getInstance() {
        if (instance == null) {
            instance = new ServerData();
        }
        return instance;
    }

    public void init() {
        generateRsa();
        loadData();
        pingChecker.scheduleAtFixedRate(this::checkPing,0,1,TimeUnit.SECONDS);
    }

    public void shutdown() {
        pingChecker.shutdown();
        botChecker.shutdown();
    }





    // Account Management
    public Account getAccount(String username) {
        return accounts.get(username);
    }

    public void putAccount(String username, Account account) {
        accounts.put(username, account);
        saveData();

        // Debug ausgaben
        System.out.println("--- Neuer Account ---"
                + "\nusername: "+ username
                + "\npasswordHash: "+ account.getPasswordHash()
                +"\n");
    }

    public boolean accountExists(String username) {
        return accounts.containsKey(username);
    }




    // Session Management
    public String getUsernameFromSession(String sessionID) {
        return httpSessionToAccount.get(sessionID);
    }


    /**
     * Anmeldung eines Accounts
     * @param sessionID SessionID des Accounts der sich anmelden möchte
     * @param username Usernamedes Accounts der sich anmelden möchte
     */
    public void addSessionToAccount(String sessionID, String username) {
        ping(username, -1);
        httpSessionToAccount.put(sessionID, username);
        accountToHttpSession.put(username, sessionID);
        loggedInAccounts.add(username);
        System.out.println("Session -> Account:"
                +"\nSession ID:" + sessionID
                +"\nusername: "+ username
                +"\n");
    }


    public boolean isAccountsLoggedIn(String username) {
        return loggedInAccounts.contains(username);
    }

    public void logoutUser(String username) {
        // Aktives Game beenden
        int gameID = usernameToGameID.get(username);
        if (gameID != -1) {
            System.out.println("Laufendes Spiel automatisch beenden...");
            String[] usernames = getUsernamesOfGame(gameID);
            String loggenInUsername;
            if (usernames[0].equals(username)) {
                loggenInUsername = usernames[1];
            } else{
                loggenInUsername = usernames[0];
            }
            String messageJson = giveUp(username, gameID);

            try {
                System.out.println("Nachricht an den verbleibenden user im Game senden");
                System.out.println(messageJson);
                String sessionID1 = getWebSocketSessionID(loggenInUsername);
                Session session1 = getWebSocketSession(sessionID1);
                session1.getBasicRemote().sendText(messageJson);

            } catch (IOException e) {
                System.out.println(e.getMessage());
                throw new RuntimeException(e);
            }
        }

        // Zuordnungen löschen
        try {
            String sessionID = accountToHttpSession.get(username);
            accountToHttpSession.remove(username);
            httpSessionToAccount.remove(sessionID);
            loggedInAccounts.remove(username);
            lastPings.remove(username);

            if (queue.remove(username)){
                System.out.println("Durch Logout aus Warteschlange entfernt: " + username);
            }
        } catch (NullPointerException e) {
            System.out.println("Logout Null");
            System.out.println(e.getMessage());
        }
    }



    public String getIPAddress(){
        try {
            return Inet4Address.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }


    public void ping(String username, int gameID){
        long lastPing = System.currentTimeMillis();
        lastPings.put(username,lastPing);
        usernameToGameID.put(username,gameID);
    }



    private void checkPing(){
        System.out.println("Ping checken...");
        List<String> removedAccounts = new ArrayList<>();
        for (String username : lastPings.keySet()) {
            long lastPing = lastPings.get(username);
            if (System.currentTimeMillis() - lastPing > 10 * 1000) {
                System.out.println("Inaktiver User gefunden: " + username);
                logoutUser(username);
                removedAccounts.add(username);
                System.out.println("Automatisch abgemeldet: " + username);
            }
        }
        for  (String username : removedAccounts) {
            lastPings.remove(username);
        }
    }



    // Websocketmanagement
    public void putWebSocketSession(String sessionID, Session session) {
        websocketSessions.put(sessionID, session);
    }

    public void removeWebSocketSessionMapping(String username, String sessionID) {
        usernameToWebSocketSession.remove(usernameToWebSocketSession.get(username));
        webSocketSessionToUsername.remove(usernameToWebSocketSession.get(sessionID));
    }

    public void connectWebSocketToUsername(String username, String sessionID) {
        webSocketSessionToUsername.put(sessionID,username);
        usernameToWebSocketSession.put(username,sessionID);
    }

    public String getWebSocketSessionID(String username) {
        return usernameToWebSocketSession.get(username);
    }

    public String getWebSocketUsername(String sessionID) {
        return webSocketSessionToUsername.get(sessionID);
    }

    public Session getWebSocketSession(String sessionID) {
        return websocketSessions.get(sessionID);
    }

    public void removeWebSocketSession(String sessionID) {
        websocketSessions.remove(sessionID);
    }


    // Game Management
    public NewGameReturn addQueue(String username){
        if (queue.isEmpty()) {
            queue.add(username);
            //System.out.println("Zur WARTESCHLANGE hinzugefügt: " + username);
            startBotChecker(username);
            return null;
        }
        else {
            if (botCheckers != null) {
                botCheckers.cancel(false);
                botCheckers = null;
            }
            String user2 =  queue.poll();
            int gameID = newGame(username, user2);
            //System.out.println("Neues GAME mit: " + username + " und " + user2);
            return new NewGameReturn(gameID, user2);
        }
    }


    /**
     * Erstellt ein neues Spiel mit den übergebenen Usernamen
     * @param user1 Erster User
     * @param user2 Zweiter User
     * @return GameID des neu erstellten Spiels
     */
    private int newGame(String user1, String  user2) {
        int gameID = ++gameIDCounter;
        ViergewinntModel game = new ViergewinntModel(user1, user2);
        games.put(gameID,game);
        System.out.println("Neues Game mit " + user1 + " und " + user2);
        return gameID;
    }


    /**
     * Führt den Ablauf eines Spielzugs eines Spielers durch.
     * @param username Username des Spielers der aufgegeben hat
     * @param gameID GameID des Spiels
     * @param column Spalte auf dem Spielfeld die der Spieler ausgewählt hat
     * @return Message als JSON formatiert, der an beide Clients des Spiels gesendet werden kann
     */
    public String gameTurn(String username, int gameID, int column){
        ViergewinntModel game = games.get(gameID);

        if (game.getUsernameAktuell().equals(username)){
            game.einsetzen(column);

            // Gamestate checken
            GameState gameState = game.getGameState();
            if (gameState != GameState.ONGOING){
                String[] users = getUsernamesOfGame(gameID);
                Account account1 = getAccount(users[0]);
                Account account2 = getAccount(users[1]);

                usernameToGameID.put(users[0], -1);
                usernameToGameID.put(users[1], -1);

                if (gameState == GameState.DRAW){
                    account1.addDraw();
                    account2.addDraw();
                }
                else if (gameState == GameState.USER1WON){
                    account1.addWin();
                    account2.addLoss();
                }
                else if (gameState == GameState.USER2WON){
                   account1.addLoss();
                   account2.addWin();
                }
                games.remove(gameID);
                saveData();
            }

            String[] feld = game.getFeld();
            String[] activePlayer = new  String[1];
            activePlayer[0] = game.getUsernameAktuell();
            String[] gameStateString = new String[1];
            gameStateString[0] = game.getGameState().toString();

            Map<String, String[]> map = new HashMap<>();
            map.put("type" , new String[]{"GAME_TURN"});
            map.put("matrix", feld);
            map.put("active_player", activePlayer);
            map.put("game_state", gameStateString);

            try {
                return objectMapper.writeValueAsString(map);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        } else  {
            return "FALSCHER SPIELER AM ZUG";
        }
    }


    /**
     * Führt den Ablauf des Aufgebens eines Spielers durch.
     * @param username Username des Spielers der aufgegeben hat
     * @param gameID GameID des Spiels
     * @return Message als JSON formatiert, der an beide Clients des Spiels gesendet werden kann
     */
    public String giveUp(String username, int gameID){
        String[] users = getUsernamesOfGame(gameID);
        Account account1 = getAccount(users[0]);
        Account account2 = getAccount(users[1]);
        String gameState;


        usernameToGameID.put(users[0], -1);
        usernameToGameID.put(users[1], -1);

        if (username.equals(users[0])){
            account1.addLoss();
            account2.addWin();
            gameState = GameState.USER2WON.toString();
        } else {
            account1.addWin();
            account2.addLoss();
            gameState = GameState.USER1WON.toString();
        }
        games.remove(gameID);
        saveData();



        Map<String, String[]> map = new HashMap<>();
        map.put("type" , new String[]{"GIVE_UP"});
        map.put("game_state", new String[] {gameState});

        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * Gibt die beiden Usernames der Accounts zurück die in einem Spiel gegeneinander spielen.
     * @param gameID GameID des Spiels
     * @return Usernames der Accounts des Spiels
     */
    public String[] getUsernamesOfGame(int gameID){
        String[] usernames = new String[2];
        ViergewinntModel game = games.get(gameID);
        usernames[0] = game.getUser1();
        usernames[1] = game.getUser2();
        return usernames;
    }

    /**
     * Gibt den Username des Spielers zurück der aktuell am Zug ist.
     * @param gameID GameID des Spiels
     * @return Username des Spielers der aktuell am Zug ist
     */
    public String getActivePlayer(int gameID){
        return games.get(gameID).getUsernameAktuell();
    }


    /**
     * Gibt das Spielfeld eines Spiels in Form eines String Arrays zurück
     * @param gameID GameID des Spiels
     * @return Spielfeld des Spiels
     */
    public String[] getSpielfeld(int gameID){
        return games.get(gameID).getFeld();
    }


    private void startBotChecker(String username){
        Runnable botCheckerRunnable = () -> checkForBot(username);
        botCheckers = botChecker.schedule(botCheckerRunnable, 10, TimeUnit.SECONDS);
        System.out.println("Bot checker gestartet...");
    }

    private void checkForBot(String username){
        if(queue.contains(username)){
            startBot();
        }
    }


    // Botgegner

    /**
     * Überprüft welcher Bot aktuell nicht bereits
     * in Verwendung ist und startet den ersten verfügbaren Bot.
     */
    private void startBot(){
        System.out.println("Verfügbaren Bot suchen...");
        if(!loggedInAccounts.contains("Bot1")){
            startBot("Bot1");
        } else if(!loggedInAccounts.contains("Bot2")){
            startBot("Bot2");
        } else if(!loggedInAccounts.contains("Bot3")){
            startBot("Bot3");
        } else {
            System.out.println("Kein Bot verfügbar");
        }
    }

    /**
     * Startet einen Botgegner.
     * @param botName Name des Bots
     */
    private void startBot(String botName){
        System.out.println("Arbeitsverzeichnis: " + System.getProperty("user.dir"));
        System.out.println(botName + " starten...");
        ProcessBuilder pb = new ProcessBuilder(
                "java",
                "-jar",
                "VierGewinntNativerClientBot-1.0-jar-with-dependencies.jar",
                "localhost:8080/VS_Projekt_vierGewinnt/",
                botName
        );
        try {
            pb.start();
            System.out.println(botName + " gestartet");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }



    // Validierung von Nachrichten
    public String getData(Map<?,?> message){
        // Felder auslesen
        String sessionID = message.get("sessionid").toString();
        String data = message.get("data").toString();
        String hash = message.get("hash").toString();

        if (validate(sessionID,data,hash)){
            return data;
        } else {
            return null;
        }
    }


    // Überprüfung des Hashes
    public boolean validate(String sessionID, String data, String hash) {
        //System.out.println("Erhaltene SessionID: " + sessionID);
        String username = httpSessionToAccount.get(sessionID);
        if (username == null){ // Es handelt sich vermutlich um eine Anmeldung, da noch keine Session dem Nutzer zugeordnet ist

            // in der Nachricht nach Username suchen
            Map<?, ?> map;
            try {
                map = objectMapper.readValue(data, Map.class);
                username = map.get("username").toString();
            } catch (Exception e) {
                return false;
            }
            System.out.println(data);
            if (username == null || accounts.get(username) == null) {
                System.out.println("Es wurde kein Username gefunden");
                return false;
            } else  {
                username = map.get("username").toString();
            }
        }

        // Hash generieren
        String passwordHash = accounts.get(username).getPasswordHash();
        String basisString = sessionID + data + passwordHash;

        // Quelle: https://www.baeldung.com/sha-256-hashing-java
        String sha256 = Hashing.sha256()
                .hashString(basisString, StandardCharsets.UTF_8)
                .toString();

        return hash.equals(sha256);
    }

    // Generierung der RSA Schlüssel
    private void generateRsa(){
        //https://www.baeldung.com/java-rsa
        KeyPairGenerator generator;
        try {
            generator = KeyPairGenerator.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {
            System.out.println("Fehler beim Initialisieren des RSA Generators");
            return;
        }
        generator.initialize(2048);
        KeyPair pair = generator.generateKeyPair();
        privateKey = pair.getPrivate();
        publicKey = pair.getPublic();
    }

    public String getPublicKey(){
        String base64 =
                Base64.getEncoder().encodeToString(publicKey.getEncoded());

        return "-----BEGIN PUBLIC KEY-----\n" +
                base64 +
                "\n-----END PUBLIC KEY-----";
    }

    public String decryptMessage(String encryptedMessage){
        try {
            Cipher decryptCipher = Cipher.getInstance("RSA");
            decryptCipher.init(Cipher.DECRYPT_MODE, privateKey);

            encryptedMessage = encryptedMessage.replace("\"", "");
            byte[] encryptedMessageBytes = Base64.getDecoder().decode(encryptedMessage);
            byte[] decryptedMessageBytes = decryptCipher.doFinal(encryptedMessageBytes);


            return new String(decryptedMessageBytes, StandardCharsets.UTF_8);

        } catch (NoSuchAlgorithmException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException | NoSuchPaddingException e) {
            System.out.println("decryptMessage: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }




    // Persistierung der Accounts
    /**
     * Läd Accountdaten aus der Datei accounts.json
     */
    private void loadData(){
        File accountFile = new File("accounts.json");
        if  (accountFile.exists()){
            // Accounts
            try {
                accounts = objectMapper.readValue((accountFile), new TypeReference<HashMap<String, Account>>() {});
            } catch (IOException e) {
                System.out.println("Fehler beim Lesen der Accounts:");
            }
        } else {
            // File mit Bot Accounts erstellen
            System.out.println("Bot Accounts erstellen...");
            Account bot1 = new Account("Bot1", "d482ba4b7d3218f3e841038c407ed1f94e9846a4dd68e56bab7718903962aa98");
            Account bot2 = new Account("Bot2", "8588310a98676af6e22563c1559e1ae20f85950792bdcd0c8f334867c54581cd");
            Account bot3 = new Account("Bot3", "825d56f6767ea9562139e08caf3e82e66e568b64c5408a501c0855bb928dc7ae");

            Map<String, Account> botAccounts = new HashMap<>();
            botAccounts.put("Bot1", bot1);
            botAccounts.put("Bot2", bot2);
            botAccounts.put("Bot3", bot3);

            try {
                objectMapper.writerWithDefaultPrettyPrinter().writeValue((accountFile), botAccounts);
                System.out.println("Bot Accounts gespeichert (?)");
            } catch (IOException e) {
                System.out.println("Fehler beim Speichern der Botaccounts:");
            }
        }
    }

    /**
     * Speichert alle Accountdaten in eine Datei accounts.json
     */
    private void saveData(){
        // Accounts
        File accountFile = new File("accounts.json");

        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue((accountFile), accounts);
        } catch (IOException e) {
            System.out.println("Fehler beim speichern der Accounts:");
        }
    }
}
