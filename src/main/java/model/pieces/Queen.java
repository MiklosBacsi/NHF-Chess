package model.pieces;

import model.*;
import java.util.List;

/**
 * This class implements the piece queen in chess.
 * @see Piece
 * @author Miklós Bácsi
 */
public class Queen extends Piece {

    /**
     * Constructor that initializes values
     * @param color color of the piece
     * @param row row index of the piece
     * @param col column index of the piece
     */
    public Queen(PieceColor color, int row, int col) {
        super(PieceType.QUEEN, color, row, col);
    }

    /**
     * @param board chess board (necessary for determining legal moves)
     * @return pseudo-legal moves of this queen (without the restrictions of variant rules)
     */
    @Override
    public List<Move> getPseudoLegalMoves(Board board) {
        // Single "geometric" moves stored as vectors
        int[][] directions = {
                {-1, 0}, {1, 0}, {0, -1}, {0, 1},   // Straight
                {-1, -1}, {-1, 1}, {1, -1}, {1, 1}  // Diagonal
        };
        return getSlidingMoves(board, directions);
    }
}
