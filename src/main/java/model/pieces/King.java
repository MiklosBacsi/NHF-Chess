package model.pieces;

import model.*;
import java.util.List;

/**
 * This class represents the piece king in chess.
 * @author Miklós Bácsi
 */
public class King extends Piece {

    /**
     * Constructor that initializes values
     * @param color color of the piece
     * @param row row index of the piece
     * @param col column index of the piece
     */
    public King(PieceColor color, int row, int col) {
        super(PieceType.KING, color, row, col);
    }

    /**
     * @param board chess board (necessary for determining legal moves)
     * @return pseudo-legal moves of this king (without the restrictions of variant rules)
     */
    @Override
    public List<Move> getPseudoLegalMoves(Board board) {
        // Single "geometric" moves stored as vectors
        int[][] steps = {
                {-1, -1}, {-1, 0}, {-1, 1},
                {0, -1},           {0, 1},
                {1, -1},  {1, 0},  {1, 1}
        };
        return getSingleStepMoves(board, steps);
    }
}
