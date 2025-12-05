package model.pieces;

import model.*;
import java.util.List;

/**
 * This class implements the piece knight in chess.
 * @author Miklós Bácsi
 */
public class Knight extends Piece {

    /**
     * Constructor that initializes values
     * @param color color of the piece
     * @param row row index of the piece
     * @param col column index of the piece
     */
    public Knight(PieceColor color, int row, int col) {
        super(PieceType.KNIGHT, color, row, col);
    }

    /**
     * @param board chess board (necessary for determining legal moves)
     * @return pseudo-legal moves of this knight (without the restrictions of variant rules)
     */
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
