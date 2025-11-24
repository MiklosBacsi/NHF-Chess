package model.pieces;

import model.*;
import java.util.List;

public class King extends Piece {

    public King(PieceColor color, int row, int col) {
        super(PieceType.KING, color, row, col);
    }

    @Override
    public List<Move> getPseudoLegalMoves(Board board) {
        int[][] steps = {
                {-1, -1}, {-1, 0}, {-1, 1},
                {0, -1},           {0, 1},
                {1, -1},  {1, 0},  {1, 1}
        };
        return getSingleStepMoves(board, steps);
    }
}
