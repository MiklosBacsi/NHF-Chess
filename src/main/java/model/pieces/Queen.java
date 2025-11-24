package model.pieces;

import model.*;
import java.util.List;

public class Queen extends Piece {

    public Queen(PieceColor color, int row, int col) {
        super(PieceType.QUEEN, color, row, col);
    }

    @Override
    public List<Move> getPseudoLegalMoves(Board board) {
        // Combine Rook and Bishop directions
        int[][] directions = {
                {-1, 0}, {1, 0}, {0, -1}, {0, 1},   // Straight
                {-1, -1}, {-1, 1}, {1, -1}, {1, 1}  // Diagonal
        };
        return getSlidingMoves(board, directions);
    }
}
