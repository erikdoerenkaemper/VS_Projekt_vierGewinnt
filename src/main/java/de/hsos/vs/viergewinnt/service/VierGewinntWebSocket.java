package de.hsos.vs.viergewinnt.service;

import de.hsos.vs.viergewinnt.data.NewGameReturn;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.hsos.vs.viergewinnt.data.ServerData;
import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import jakarta.websocket.Session;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Quelle
 */
@ServerEndpoint("/game")
public class VierGewinntWebSocket {
    /**
     * ObjectMapper zum konvertieren zwischen Objekten und JSON-Strings.
     */
    private final ObjectMapper objectMapper = new ObjectMapper();
    /**
     * Singelton-Instanz von ServerData
     */
    private final static ServerData serverData = ServerData.getInstance();

    /**
     * Wird bei einer neuen Websocket-Verbindung aufgerufen.
     * Speichert die neue Session in ServerData.
     */
    @OnOpen
    public void onOpen(Session session){ // Neue Verbindung
        System.out.println("\nOnOpen...");
        serverData.putWebSocketSession(session.getId(), session);
        System.out.println("Neue WebSocketSessionID: " + session.getId());
    }

    /**
     * Nimmt eingehende Websocket-Nachrichten entgegen.
     * Die Nachrichten müssen im JSON-Formatiert sein
     * und ein Feld mit dem Key "type" beinhalten.
     * Anhand des Types wird die Verarbeitung der Nachricht bestimmt.
     * Verfügbare Types:<br>
     * 1. LOBBY_CONNECT: <br>
     * Die Nachricht muss einen Benutzernamen enthalten.
     * Die Websocket-Verbindung wird diesem Benutzernamen zugeordnet.<br>
     * <br>
     * 2. GAME_CONNECT:<br>
     * Die Nachricht muss einen Benutzernamen und eine GameID enthalten.
     * Die Websocket-Verbindung wird diesem Benutzernamen zugeordnet.
     * Die GameID wird verwendet, um den Gegnerischen Client zu ermitteln.
     * Es wird eine Nachricht des Typs USERNAMES zurpckgesendet
     * die beide Usernames des Spiels enthält.<br>
     * <br>
     * 3. SEARCH_GAME:<br>
     * Trägt den Client in die Warteschlange ein.
     * Wenn ein Gegner gefunden wurde wird eine Nachricht
     * des Typs GAME_FOUND zurückgesendet.<br>
     * <br>
     * 4. GAME_TURN:<br>
     * Enthält einen Spielzug eines Clients.
     * Das neue Spielfeld wird an beide Clients die
     * an dem entsprechenden Spiel teilnehmen gesendet.<br>
     * <br>
     * 5. GIVE_UP:<br>
     * Signalisiert das Aufgeben eines Clients.<br>
     * <br>
     * 6. PING:<br>
     * Nachricht, die jeder angemeldete Client regelmäßig senden
     * muss, um nicht automatisch abgemeldet zu werden.
     */
    @OnMessage
    public void onMessage(Session session, String message) {
        //System.out.println("onMessage...");
        // Message auswerten
        Map<?, ?> validatedData;
        String type;
        try {
            Map<?, ?> data = objectMapper.readValue(message, Map.class);
            validatedData = objectMapper.readValue(serverData.getData(data), Map.class);
            type = validatedData.get("type").toString();
        } catch (Exception e) {
            System.out.println("Websocket Daten nicht korrekt formatiert!");
            return;
        }

        if(!(type.equals("PING") || type.equals("GAME_TURN"))) {
            System.out.println("\nMESSAGE: " + message);
        }
        String username;
        int gameID;


        switch (type){
            // Sessionanmeldung - Lobby
            case "LOBBY_CONNECT":
                username = validatedData.get("username").toString();
                connectLobbySession(username, session.getId());
                break;

            // Sessionanmeldung - Game
            case "GAME_CONNECT":
                username = validatedData.get("username").toString();
                gameID = Integer.parseInt(validatedData.get("gameID").toString());
                connectGameSession(username, session.getId(), gameID);
                break;

            // Spielsuche
            case "SEARCH_GAME":
                searchGame(session.getId());
                break;

            // Spielzug
            case "GAME_TURN":
                gameTurn(session.getId(), validatedData);
                break;

            // Spieler gibt auf
            case "GIVE_UP":
                gameID = Integer.parseInt(validatedData.get("gameID").toString());
                giveUpGame(session.getId(), gameID);
                break;

            case "PING":
                gameID = Integer.parseInt(validatedData.get("gameID").toString());
                ping(session.getId(), gameID);

            default:
                break;
        }
    }


    /**
     * Zuordung von Websocket zu Username beim betreten der Lobby.
     * @param username Username des Clients
     * @param sessionID SessionID des Websockets
     */
    private void connectLobbySession(String username, String sessionID){
        serverData.connectWebSocketToUsername(username, sessionID);
        System.out.println("Connected Username to SessionID: " + username + " -> " + sessionID);
    }


    /**
     * Zuordung von Websocket zu Username bei einem neuen Game.
     * @param username Username des Clients
     * @param sessionID SessionID des Websockets
     * @param gameID ID des Games um andere Mitspieler zu ermitteln
     */
    private void connectGameSession(String username, String sessionID, int gameID){
        // Alten LobbyWebSocket löschen
        serverData.removeWebSocketSessionMapping(username, serverData.getWebSocketSessionID(username));

        // Neuen GameWebSocket speichern
        serverData.connectWebSocketToUsername(username, sessionID);

        // Beide User des Games ermittleln
        String[] usernames = serverData.getUsernamesOfGame(gameID);
        String activePlayer = serverData.getActivePlayer(gameID);
        String[] spielFeld =  serverData.getSpielfeld(gameID);

        Session session = serverData.getWebSocketSession(sessionID);

        Map<String, String[]> map = new HashMap<>();
        map.put("type", new String[] {"USERNAMES"});
        map.put("usernames", usernames);
        map.put("active_player", new String[] {activePlayer});
        map.put("matrix", spielFeld);

        try {
            String messageJson = objectMapper.writeValueAsString(map);
            session.getBasicRemote().sendText(messageJson);
        } catch (JsonProcessingException e) {
            System.out.println("JsonProcessingException!");
        } catch (IOException e) {
            System.out.println("IOException!");
        }
    }

    /**
     * Ein Client lässt sich in die Warteschlange eintragen.
     * Falls bereits ein anderer CLient wartet,
     * werden die beiden Clients in ein gemeinsamen Match übergeleitet
     * @param sessionID SessionID des Clients der sich in die Warteschlange eintragen will
     */
    private void searchGame(String sessionID) {
        String username = serverData.getWebSocketUsername(sessionID);
        NewGameReturn result = serverData.addQueue(username);

        if (result != null) { // Neues Spiel wurde erstellt
            int gameID = result.gameID();
            String user2 = result.username();
            String sessionID2 = serverData.getWebSocketSessionID(user2);



            // Antwort verfassen
            Map<String, String> map = new HashMap<>();
            map.put("type", "GAME_FOUND");
            map.put("gameID", Integer.toString(gameID));
            String messageJson;
            try {
                messageJson = objectMapper.writeValueAsString(map);
            } catch (Exception e){
                return;
            }


            Session session1 = serverData.getWebSocketSession(sessionID);
            Session session2 = serverData.getWebSocketSession(sessionID2);


            if (session1 == null) {
                giveUpGame(sessionID, gameID);
                System.out.println(username + " nicht mehr erreichbar. Spiel abgebrochen");
                return;
            } else if (session2 == null) {
                giveUpGame(sessionID2, gameID);
                System.out.println(user2 + " nicht mehr erreichbar. Spiel abgebrochen");
                return;
            }

            RemoteEndpoint.Basic session1Remote =  session1.getBasicRemote();
            RemoteEndpoint.Basic session2Remote =  session2.getBasicRemote();

            try {
                session1Remote.sendText(messageJson);
                session2Remote.sendText(messageJson);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }



        } else { // Zur Warteschalange hinzugefügt
            System.out.println("Wartet: " + sessionID);
        }
    }


    /**
     * Verarbeitung eines Spielzugs
     * @param sessionID SessionID des Clients der den Zug gemacht hat
     * @param data Message des Clients mit allen Informationen zum Spielzug
     */
    private void gameTurn(String sessionID, Map<?, ?> data) {
        // Daten auslesen
        String username = serverData.getWebSocketUsername(sessionID);
        int column = Integer.parseInt(data.get("column").toString());
        int gameID = Integer.parseInt(data.get("gameID").toString());

        String[] usernames = serverData.getUsernamesOfGame(gameID);
        String messageJson = serverData.gameTurn(username, gameID, column);

        System.out.println("GAME TURN:" + " Username: " + username + " GameID: " + gameID + " COLUMN: " + column);
        if (messageJson.equals("FALSCHER SPIELER AM ZUG")) {
            System.out.println("FALSCHER SPIELER AM ZUG\n");
        } else {
            try {
                System.out.println(messageJson);
                sendMessagetoTwoClients(messageJson, usernames[0], usernames[1]);

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void giveUpGame(String sessionID, int gameID) {
        String username = serverData.getWebSocketUsername(sessionID);
        System.out.println("Gibt auf: "+ username);

        String[] usernames = serverData.getUsernamesOfGame(gameID);
        String messageJson = serverData.giveUp(username, gameID);
        try {
            sendMessagetoTwoClients(messageJson, usernames[0], usernames[1]);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void sendMessagetoTwoClients(String messageJson, String user1, String user2) throws IOException {
        String sessionID1 = serverData.getWebSocketSessionID(user1);
        String sessionID2 = serverData.getWebSocketSessionID(user2);

        Session session1 = serverData.getWebSocketSession(sessionID1);
        Session session2 = serverData.getWebSocketSession(sessionID2);

        if(session1 == null && session2 == null){
            return;
        } else if (session1 == null) {
            System.out.println("Websocket Verbindung zu " + user1 + " nicht bestehend!");
            session2.getBasicRemote().sendText(messageJson);
        } else if (session2 == null) {
            System.out.println("Websocket Verbindung zu " + user2 + " nicht bestehend!");
            session1.getBasicRemote().sendText(messageJson);
        } else {
            session1.getBasicRemote().sendText(messageJson);
            session2.getBasicRemote().sendText(messageJson);
        }
    }

    private void ping(String sessionID, int gameID){
        String username = serverData.getWebSocketUsername(sessionID);
        serverData.ping(username, gameID);
    }


    /**
     * Wird aufgerufen wenn ein Websocket geschlossen wird.
     * Entfernt die Zuordnung von Websocket Session und Username
     * und entfernt die Session aus den gespeicherten Websocket Sessions
     * @param session Websocket Session des Clients
     */
    @OnClose
    public void onClose(Session session){
        System.out.println("OnClose...");

        // Session löschen
        String username = serverData.getWebSocketUsername(session.getId());
        serverData.removeWebSocketSession(session.getId());
        serverData.removeWebSocketSessionMapping(username, session.getId());
        System.out.println("Gelöscht: " + session.getId());
    }


    /**
     * Wird aufgerufen wenn ein Fehler im Websocket auftritt
     * und gibt die Fehlermeldgung aus.
     * @param session Session
     */
    @OnError
    public void onError(Session session, Throwable throwable) {
        System.out.println("Websocket Error: " + throwable.getMessage());
    }
}
