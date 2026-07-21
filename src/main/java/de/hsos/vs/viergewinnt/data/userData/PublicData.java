package de.hsos.vs.viergewinnt.data.userData;

public class PublicData {
    private String username;
    private int wins;
    private int losses;
    private int draws;

    public PublicData(String username) {
        this.username = username;
    }

    public PublicData() {}


    public String getUsername() {
        return username;
    }

    public int getWins() {
        return wins;
    }

    public int getLosses() {
        return losses;
    }

    public int getDraws() {
        return draws;
    }

    public void setUsername(String username) {
        this.username = username;
    }
    public void setWins(int wins) {
        this.wins = wins;
    }
    public void setLosses(int losses) {
        this.losses = losses;
    }
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
