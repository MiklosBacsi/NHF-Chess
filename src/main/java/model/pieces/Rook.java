package model.pieces;

import model.*;
import java.util.List;

/**
 * This class implements the piece rook in chess.
 * @see Piece
 * @author Miklós Bácsi
 */
public class Rook extends Piece {

    public Rook(PieceColor color, int row, int col) {
        super(PieceType.ROOK, color, row, col);
    }

    /**
     * @param board chess board (necessary for determining legal moves)
     * @return pseudo-legal moves of this rook (without the restrictions of variant rules)
     */
    @Override
    public List<Move> getPseudoLegalMoves(Board board) {
        // Single "geometric" moves stored as vectors
        int[][] directions = { {-1, 0}, {1, 0}, {0, -1}, {0, 1} }; // Straight
        return getSlidingMoves(board, directions);
    }
}
