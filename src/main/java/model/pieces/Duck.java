package model.pieces;

import model.*;
import java.util.ArrayList;
import java.util.List;

/**
 * This class implements the piece duck in Duck Chess variant.
 * @author Miklós Bácsi
 */
public class Duck extends Piece {

    /**
     * Constructor initializes values.
     * @param row row index of the piece
     * @param col column index of the piece
     */
    public Duck(int row, int col) {
        // The Duck has DUCK type and SPECIAL color
        super(PieceType.DUCK, PieceColor.SPECIAL, row, col);
    }

    /**
     * @param board chess board (necessary for determining legal moves)
     * @return pseudo-legal moves where the duck could be placed
     */
    @Override
    public List<Move> getPseudoLegalMoves(Board board) {
        List<Move> moves = new ArrayList<>();

        // The Duck can move to ANY (but different) empty square on the board
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                // Cannot stay in the same place
                if (r == this.row && c == this.col) continue;

                // Square must be empty
                if (board.getPiece(r, c) == null) {
                    moves.add(new Move(this, row, col, r, c, MoveType.DUCK, null));
                }
            }
        }
        return moves;
    }
}
