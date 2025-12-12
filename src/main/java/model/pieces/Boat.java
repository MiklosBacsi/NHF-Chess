package model.pieces;

import model.*;
import java.util.List;

/**
 * This class implements the piece boat in chess, which moves the same way as the rook, but cannot castle.
 * @see Piece
 * @see model.rules.ChaturajiVariant
 * @author Miklós Bácsi
 */
public class Boat extends Piece {

    /**
     * Constructor that initializes values.
     * @param color color of the piece
     * @param row row index of the piece
     * @param col column index of the piece
     */
    public Boat(PieceColor color, int row, int col) {
        super(PieceType.BOAT, color, row, col);
    }

    /**
     * @param board chess board (necessary for determining legal moves)
     * @return pseudo-legal moves of this pawn (without the restrictions of variant rules)
     */
    @Override
    public List<Move> getPseudoLegalMoves(Board board) {
        // Behaves exactly like a Rook
        int[][] directions = { {-1, 0}, {1, 0}, {0, -1}, {0, 1} };
        return getSlidingMoves(board, directions);
    }
}
