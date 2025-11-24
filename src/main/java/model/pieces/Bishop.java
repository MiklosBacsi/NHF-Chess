package model.pieces;

import model.*;
import java.util.List;

public class Bishop extends Piece {

    public Bishop(PieceColor color, int row, int col) {
        super(PieceType.BISHOP, color, row, col);
    }

    @Override
    public List<Move> getPseudoLegalMoves(Board board) {
        // Diagonals
        int[][] directions = { {-1, -1}, {-1, 1}, {1, -1}, {1, 1} };
        return getSlidingMoves(board, directions);
    }
}
