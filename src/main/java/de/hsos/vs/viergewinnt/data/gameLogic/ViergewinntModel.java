package de.hsos.vs.viergewinnt.data.gameLogic;

/**
 *  Das zentrale Modell des Vier-Gewinnt-Spiels.
 *  <p>
 *  Verwaltet die Spiellogik in einem festen Koordinatensystem von 7x6 Einheiten (-> 7:6).
 *  </p>
 *  @author Daniel Röwekamp
 */
public class ViergewinntModel {
    private final PlayerType[][] feld;
    private  PlayerType spielerAktuell;
    private  GameState gameState;
    private final int gameID;
    private final String user1;
    private final String user2;
    //  ==============  INITIALIZATION  ==============  //
    /**
     *  Initialisiert ein neues Spiel
     */ // Create
    public ViergewinntModel(int gameID,  String user1, String user2) {
        feld=new PlayerType[6][7];
        for (int y=0;y<6;y++){
            for (int x=0;x<7;x++){
                feld[y][x]= PlayerType.NO_PLAYER;
            }
        }
        spielerAktuell=PlayerType.RED;
        gameState=GameState.ONGOING;
        this.gameID=gameID;
        this.user1=user1;
        this.user2=user2;
    }


    private static GameState readGameState(int state){
        switch (state){
            case 1:return GameState.USER1WON;
            case 2:return GameState.USER2WON;
            default:return GameState.ONGOING;
        }
    }
    /**
     * Liest den Spieler aus
     * @param spieler Spieler als int
     * @return das Spieler als Figur
     */
    private static PlayerType readSpieler(int spieler){
        switch (spieler){
            case 1:return PlayerType.YELLOW;
            case 0:return PlayerType.RED;
            default:return PlayerType.NO_PLAYER;
        }
    }
    /**
     *Liest den Spielstand aus
     * @param gameData Spielstand als String
     * @return das ausgewertete Spielfeld
     */
    private static PlayerType[][] readGameData(String gameData){
        PlayerType[][] readFeld = new PlayerType[6][7];
        if (gameData == null || gameData.isEmpty()) {
            for (int x = 0; x < 6; x++) {
                for (int y = 0; y < 7; y++) {
                    readFeld[x][y] = PlayerType.NO_PLAYER;
                }
            }
            return readFeld;
        }
        String[] reihe = gameData.split(";");
        for (int i = 0; i < reihe.length; i++){
            String[] daten = reihe[i].split(",");
            for (int x=0;x<7;x++){
                readFeld[i][x]=readSpieler(Integer.decode(daten[x]));
            }
        }
        return readFeld;
    }
    /**
     * Spielzug einleiten
     * @param x eingabe
     * @param time  addierte Zeit
     */
    public void  update(Integer x,float time){
        if(x!=null) einsetzen(x);
    }

    /**
     * Spielzug
     * @param x Spalte
     */
    public void einsetzen(int x){
        for (int y = feld.length - 1; y >= 0; y--)
            if (feld[y][x] == PlayerType.NO_PLAYER) {
                feld[y][x] = spielerAktuell;
                if(!checkWin()){
                    spielerAktuell = (spielerAktuell == PlayerType.RED)
                            ? PlayerType.YELLOW
                            : PlayerType.RED;

                }else{
                    if (spielerAktuell==PlayerType.RED){
                        gameState=GameState.USER1WON;
                    }else {
                        gameState=GameState.USER2WON;
                    }
                    if(voll()){
                        gameState=GameState.DRAW;
                    }
                }
                break;
            }
    }

    /**
     * Siegesbedingung
     * @return Vier in einer Reihe
     */
    public boolean checkWin() {
        //  === HORIZONTAL (-) ===  //
        for (PlayerType[] playerTypes : feld)
            for (int x = 0; x <= feld[0].length - 4; x++) {
                PlayerType cell = playerTypes[x];
                if (cell != PlayerType.NO_PLAYER &&
                        cell == playerTypes[x + 1] &&
                        cell == playerTypes[x + 2] &&
                        cell == playerTypes[x + 3]) {
                    return true;
                }
            }
        //  === VERTIKAL (|) ===  //
        for (int x = 0; x < feld[0].length; x++)
            for (int y = 0; y <= feld.length - 4; y++) {
                PlayerType cell = feld[y][x];
                if (cell != PlayerType.NO_PLAYER &&
                        cell == feld[y + 1][x] &&
                        cell == feld[y + 2][x] &&
                        cell == feld[y + 3][x]) {
                    return true;
                }
            }
        //  === DIAGONAL (\) ===  //
        for (int y = 0; y <= feld.length - 4; y++)
            for (int x = 0; x <= feld[0].length - 4; x++) {
                PlayerType cell = feld[y][x];
                if (cell != PlayerType.NO_PLAYER &&
                        cell == feld[y + 1][x + 1] &&
                        cell == feld[y + 2][x + 2] &&
                        cell == feld[y + 3][x + 3]) {
                    return true;
                }
            }
        //  === DIAGONAL (/) ===  //
        for (int y = 0; y <= feld.length - 4; y++)
            for (int x = 3; x < feld[0].length; x++) {
                PlayerType cell = feld[y][x];
                if (cell != PlayerType.NO_PLAYER &&
                        cell == feld[y + 1][x - 1] &&
                        cell == feld[y + 2][x - 2] &&
                        cell == feld[y + 3][x - 3]) {
                    return true;
                }
            }
        return voll();
    }

    /**
     * Unentschieden
     * @return Ist Feld voll
     */

    private boolean voll() {
        int count=0;
        for (PlayerType[] playerTypes : feld) {
            for (int x = 0; x < feld[0].length; x++) {
                if (playerTypes[x] != PlayerType.NO_PLAYER) {
                    count++;
                }
            }
        }
        return count == feld.length * feld[0].length;
    }
    // ======== Getter ========//
    public String[] getFeld() {
        String[] stringFeld = new String[42];
        int index = 0;
        for (int y=0;y<6;y++){
            for (int x=0;x<7;x++){
                stringFeld[index] = feld[y][x].toString();
                index++;
            }
        }
        return stringFeld;
    }

    private PlayerType getSpielerAktuell() {
        return spielerAktuell;
    }

    public String getUsernameAktuell() {
        if (spielerAktuell == PlayerType.RED) {
            return user1;
        } else  {
            return user2;
        }
    }

    public GameState getGameState(){
        return gameState;
    }

    public int getGameID() {
        return gameID;
    }

    public String getUser1() {
        return user1;
    }

    public String getUser2() {
        return user2;
    }
}
