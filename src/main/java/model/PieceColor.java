package model;

/**
 * This enum represents the color of a player, thus the color of his pieces.
 * @see Piece
 * @see view.game.BoardPanel
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
    GREY,
    SPECIAL;

    /**
     * @return the color of the next player to move (order: clockwise)
     */
    public PieceColor next() {
        return switch (this) {
            case RED -> BLUE;
            case BLUE -> YELLOW;
            case YELLOW -> GREEN;
            case GREEN -> RED;
            case WHITE -> BLACK;
            default -> WHITE;
        };
    }
}
