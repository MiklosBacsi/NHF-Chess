package model;

/**
 * This enum represents the color of a player, thus the color of his pieces.
 * @author Miklós Bácsi
 */
public enum PieceColor {
    WHITE,
    BLACK,
    NONE,
    RED,
    BLUE,
    YELLOW,
    GREEN,
    SPECIAL;

    /**
     * @return the color of the next player to move (order: clockwise)
     */
    public PieceColor next() {
        // Simple toggle for 2 players, later for 4 players (Chaturaji), we will expand this logic!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!11
        return (this == WHITE) ? BLACK : WHITE;
    }
}
