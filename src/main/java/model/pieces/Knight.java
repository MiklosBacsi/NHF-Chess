package model.pieces;

import model.*;
import java.util.List;

public class Knight extends Piece {

    public Knight(PieceColor color, int row, int col) {
        super(PieceType.KNIGHT, color, row, col);
    }

    @Override
    public List<Move> getPseudoLegalMoves(Board board) {
        int[][] jumps = {
                {-2, -1}, {-2, 1}, // Up 2
                {-1, -2}, {-1, 2}, // Up 1
                {1, -2}, {1, 2},   // Down 1
                {2, -1}, {2, 1}    // Down 2
        };
        return getSingleStepMoves(board, jumps);
    }
}
