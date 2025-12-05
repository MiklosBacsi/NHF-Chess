package model.pieces;

import model.*;
import java.util.List;

/**
 * This class implements the piece bishop in chess.
 * @author Miklós Bácsi
 */
public class Bishop extends Piece {

    /**
     * Constructor that initializes values
     * @param color color of the piece
     * @param row row index of the piece
     * @param col column index of the piece
     */
    public Bishop(PieceColor color, int row, int col) {
        super(PieceType.BISHOP, color, row, col);
    }

    /**
     * @param board chess board (necessary for determining legal moves)
     * @return pseudo-legal moves of this bishop (without the restrictions of variant rules)
     */
    @Override
    public List<Move> getPseudoLegalMoves(Board board) {
        // Single "geometric" moves stored as vectors
        int[][] directions = { {-1, -1}, {-1, 1}, {1, -1}, {1, 1} }; // Diagonals
        return getSlidingMoves(board, directions);
    }
}
