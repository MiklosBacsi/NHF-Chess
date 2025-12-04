package model;

/**
 * This record represents a chess move.
 * @author Miklós Bácsi
 */
public record Move(
        Piece piece,
        int startRow, int startCol,
        int endRow, int endCol,
        MoveType type,
        Piece capturedPiece, // for Undo logic!
        boolean isFirstMove
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
        this(piece, startRow, startCol, endRow, endCol, MoveType.NORMAL, null, !piece.hasMoved());
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
        this(piece, startRow, startCol, endRow, endCol, MoveType.NORMAL, capturedPiece,  !piece.hasMoved());
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
        this(piece, startRow, startCol, endRow, endCol, type, capturedPiece, !piece.hasMoved());
    }
}
