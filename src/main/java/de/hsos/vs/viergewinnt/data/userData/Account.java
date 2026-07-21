package de.hsos.vs.viergewinnt.data.userData;

/**
 * Hält alle Informationen zu einem Account die persistiert werden.
 */
public class Account {
    /**
     * Hashwert der aus dem Passwort, welches der Client bei der Registrierung des Accounts eingegeben hat, generiert wird.
     */
    private String passwordHash;
    /**
     * Öffentliche Daten die gebündelt an einen Client verschickt werden können.
     */
    private PublicData publicData;

    /**
     * Parameterloser Konstruktor wird für JSON-Serialisierung benötigt.
     */
    public Account(){
        publicData = null;
        passwordHash = null;
    }

    /**
     * Konstruktor der für Erstellung neuer Accounts verwendet wird.
     * @param username Benutzername des neuen Accounts
     * @param passwordHash Passworthash des neuen Accounts.
     */
    public Account(String username, String passwordHash) {
        this.publicData = new PublicData(username);
        this.passwordHash = passwordHash;
    }

    /**
     * Fügt dem Account einen Sieg hinzu.
     */
    public void addWin() {
        publicData.addWin();
    }
    /**
     * Fügt dem Account eine Niederlage hinzu.
     */
    public void addLoss() {
        publicData.addLoss();
    }
    /**
     * Fügt dem Account ein Unentschieden hinzu.
     */
    public void addDraw() {
        publicData.addDraw();
    }


    public String getPasswordHash() {
        return passwordHash;
    }

    public PublicData getPublicData() {
        return publicData;
    }

    /**
     * Wird für JSON-Serialisierung benötigt.
     */
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }
    /**
     * Wird für JSON-Serialisierung benötigt.
     */
    public void setPublicData(PublicData publicData) {
        this.publicData = publicData;
    }
}