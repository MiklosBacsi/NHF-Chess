package model;

/**
 * This record represents a chess move.
 * @param piece piece that was moved
 * @param startRow start row
 * @param startCol start column
 * @param endRow end row
 * @param endCol end column
 * @param type type of the piece that was moved
 * @param capturedPiece piece that was captured (null if wasn't)
 * @param isFirstMove stores if it was the first move of the game
 * @param promotionType piece that the pawn promoted to, null if there was no promotion
 * @see MoveType
 * @see Piece
 * @see Board
 * @see view.game.BoardPanel
 * @see GameVariant
 * @see SavedMove
 * @author Miklós Bácsi
 */
public record Move(
        Piece piece,
        int startRow, int startCol,
        int endRow, int endCol,
        MoveType type,
        Piece capturedPiece, // for Undo logic!
        boolean isFirstMove,
        PieceType promotionType
) {

    /**
     * Constructor for simple moves
     * @param piece piece that made the move
     * @param startRow row index of starting position
     * @param startCol column index of starting position
     * @param endRow row index of destination position
     * @param endCol column index of destination position
     */
    public Move(Piece piece, int startRow, int startCol, int endRow, int endCol) {
        this(piece, startRow, startCol, endRow, endCol, MoveType.NORMAL, null, !piece.hasMoved(), null);
    }

    /**
     * Constructor for captures
     * @param piece piece that made the move
     * @param startRow row index of starting position
     * @param startCol column index of starting position
     * @param endRow row index of destination position
     * @param endCol column index of destination position
     * @param capturedPiece piece that was captured
     */
    public Move(Piece piece, int startRow, int startCol, int endRow, int endCol, Piece capturedPiece) {
        this(piece, startRow, startCol, endRow, endCol, MoveType.NORMAL, capturedPiece,  !piece.hasMoved(), null);
    }

    /**
     * Constructor for Special Moves
     * @param piece piece that made the move
     * @param startRow row index of starting position
     * @param startCol column index of starting position
     * @param endRow row index of destination position
     * @param endCol column index of destination position
     * @param type type of the move
     * @param capturedPiece piece that was captured
     */
    public Move(Piece piece, int startRow, int startCol, int endRow, int endCol, MoveType type, Piece capturedPiece) {
        this(piece, startRow, startCol, endRow, endCol, type, capturedPiece, !piece.hasMoved(), null);
    }

    /**
     * Constructor for the "Answered" Promotion Move (Used after user selects a piece).
     * @param original original move
     * @param chosenType type of the piece chosen
     */
    public Move(Move original, PieceType chosenType) {
        this(original.piece, original.startRow, original.startCol, original.endRow, original.endCol,
                original.type, original.capturedPiece, original.isFirstMove, chosenType);
    }

    /**
     * Constructor for DROP moves (within Crazyhouse).
     * @param piece pieces we want to drop
     * @param row row index where we want to drop the piece
     * @param col column index where we want to drop the piece
     * @return the move of the piece dropped
     */
    public static Move createDropMove(Piece piece, int row, int col) {
        return new Move(piece, -1, -1, row, col, MoveType.DROP, null, true, null);
    }
}
