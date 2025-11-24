package model;

public record Move(
        Piece piece,
        int startRow, int startCol,
        int endRow, int endCol,
        MoveType type,
        Piece capturedPiece, // for Undo logic!
        boolean isFirstMove
) {
    // Constructor for simple moves
    public Move(Piece piece, int startRow, int startCol, int endRow, int endCol) {
        this(piece, startRow, startCol, endRow, endCol, MoveType.NORMAL, null, !piece.hasMoved());
    }

    // Constructor for captures
    public Move(Piece piece, int startRow, int startCol, int endRow, int endCol, Piece capturedPiece) {
        this(piece, startRow, startCol, endRow, endCol, MoveType.NORMAL, capturedPiece,  !piece.hasMoved());
    }

    // Constructor for Special Moves
    public Move(Piece piece, int startRow, int startCol, int endRow, int endCol, MoveType type, Piece capturedPiece) {
        this(piece, startRow, startCol, endRow, endCol, type, capturedPiece, !piece.hasMoved());
    }
}
