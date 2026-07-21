package de.hsos.vs.viergewinnt.data.userData;

public class Account {
    private String passwordHash;
    private PublicData publicData;

    public Account(){
        publicData = null;
        passwordHash = null;
    }

    public Account(String username, String passwordHash) {
        this.publicData = new PublicData(username);
        this.passwordHash = passwordHash;
    }

    public void addWin() {
        publicData.addWin();
    }

    public void addLoss() {
        publicData.addLoss();
    }

    public void addDraw() {
        publicData.addDraw();
    }


    public String getPasswordHash() {
        return passwordHash;
    }

    public PublicData getPublicData() {
        return publicData;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public void setPublicData(PublicData publicData) {
        this.publicData = publicData;
    }
}