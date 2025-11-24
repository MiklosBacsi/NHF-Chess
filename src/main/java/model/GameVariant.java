package model;

import java.util.List;

public interface GameVariant {
    // Returns the TRUE legal moves (pseudo-legal already filtered by rules)
    List<Move> getLegalMoves(Board board, Piece piece);

    // Checks game status
    boolean isCheck(Board board, PieceColor color);
    boolean isCheckmate(Board board, PieceColor color);
    boolean isStalemate(Board board, PieceColor color);
}
