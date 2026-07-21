package de.hsos.vs.viergewinnt.data.userData;

/**
 * Hält alle Daten eines Accounts bis auf den Passworthash. Die Daten können gebündelt an einen Client gesendet werden.
 */
public class PublicData {
    private String username;
    private int wins;
    private int losses;
    private int draws;

    public PublicData(String username) {
        this.username = username;
    }

    /**
     * Wird für JSON-Serialisierung benötigt.
     */
    public PublicData() {}

    /**
     * Wird für JSON-Serialisierung benötigt.
     */
    public String getUsername() {
        return username;
    }
    /**
     * Wird für JSON-Serialisierung benötigt.
     */
    public int getWins() {
        return wins;
    }
    /**
     * Wird für JSON-Serialisierung benötigt.
     */
    public int getLosses() {
        return losses;
    }
    /**
     * Wird für JSON-Serialisierung benötigt.
     */
    public int getDraws() {
        return draws;
    }
    /**
     * Wird für JSON-Serialisierung benötigt.
     */
    public void setUsername(String username) {
        this.username = username;
    }
    /**
     * Wird für JSON-Serialisierung benötigt.
     */
    public void setWins(int wins) {
        this.wins = wins;
    }
    /**
     * Wird für JSON-Serialisierung benötigt.
     */
    public void setLosses(int losses) {
        this.losses = losses;
    }
    /**
     * Wird für JSON-Serialisierung benötigt.
     */
    public void setDraws(int draws) {
        this.draws = draws;
    }

    public void addWin() {
        this.wins++;
    }
    public void addLoss() {
        this.losses++;
    }

    public void addDraw() {
        this.draws++;
    }
}
