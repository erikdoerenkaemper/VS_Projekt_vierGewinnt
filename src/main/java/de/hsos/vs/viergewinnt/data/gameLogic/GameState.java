package de.hsos.vs.viergewinnt.data.gameLogic;
/**
 *  Repräsentiert die möglichen Zustände eines Spiel-Menüs.
 *  <ul>
 *      <li>{@link #ONGOING} – Das Spiel ist nicht vorbei</li>
 *      <li>{@link #USER2WON} – Das Spiel wurde verloren</li>
 *      <li>{@link #USER1WON} – Das Spiel wurde erfolgreich beendet</li>
 *      <li>{@link #DRAW} - Das Spiel wurde unentschieden beendet</li>
 *  </ul>
 *  @author Daniel Röwekamp
 */
public enum GameState {
    ONGOING , USER2WON , USER1WON, DRAW
}