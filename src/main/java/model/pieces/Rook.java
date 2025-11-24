package model.pieces;

import model.*;
import java.util.List;

public class Rook extends Piece {

    public Rook(PieceColor color, int row, int col) {
        super(PieceType.ROOK, color, row, col);
    }

    @Override
    public List<Move> getPseudoLegalMoves(Board board) {
        // Up, Down, Left, Right
        int[][] directions = { {-1, 0}, {1, 0}, {0, -1}, {0, 1} };
        return getSlidingMoves(board, directions);
    }
}
