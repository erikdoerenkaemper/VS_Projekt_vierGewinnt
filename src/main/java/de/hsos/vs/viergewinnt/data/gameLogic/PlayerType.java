package de.hsos.vs.viergewinnt.data.gameLogic;

/**
 *  Münzen in Spielfeld
 *  @author Daniel Röwekamp
 */
public enum PlayerType {
    RED(0), YELLOW(1),  NO_PLAYER(2);
    private final int idx;
    PlayerType(int idx) { this.idx = idx; }
    public int get() { return idx; }

     @Override
     public String toString() { return name(); }
}
