package de.hsos.vs.viergewinnt.data;

/**
 * Datencontainer der gameID und einen Benutzernamen enthält.
 * Wird genutzt um der Websocketklasse bei einem neu erstellten
 * Spiel die GameID und den Gegnerspieler mitzuteilen, damit
 * dieser eine GAME_FOUND Nachricht an beide Clients schicken kann.
 * @param gameID GameID des neu erstellten Spiels.
 * @param username Gegnerischer Client der bereits in der Warteschlange wartet.
 */
public record NewGameReturn(
        int gameID,
        String username
) {}
