package model.pieces;

import model.*;
import java.util.ArrayList;
import java.util.List;

/**
 * This class implements the piece pawn in chess.
 * @author Miklós Bácsi
 */
public class Pawn extends Piece {

    /**
     * Constructor that initializes values
     * @param color color of the piece
     * @param row row index of the piece
     * @param col column index of the piece
     */
    public Pawn(PieceColor color, int row, int col) {
        super(PieceType.PAWN, color, row, col);
    }

    /**
     * @param board chess board (necessary for determining legal moves)
     * @return pseudo-legal moves of this pawn (without the restrictions of variant rules)
     */
    @Override
    public List<Move> getPseudoLegalMoves(Board board) {
        List<Move> moves = new ArrayList<>();

        // White moves UP (-1), Black moves DOWN (+1)
        int direction = (color == PieceColor.WHITE) ? -1 : 1;

        // Single Step Forward
        int forwardRow = row + direction;
        if (isValidSquare(forwardRow, col) && board.getPiece(forwardRow, col) == null) {
            moves.add(new Move(this, row, col, forwardRow, col));

            // Double Step (Only, if it hasn't moved and path is clear)
            int doubleRow = row + (direction * 2);
            if (!hasMoved && isValidSquare(doubleRow, col) && board.getPiece(doubleRow, col) == null) {
                moves.add(new Move(this, row, col, doubleRow, col));
            }
        }

        // Captures (Diagonals)
        int[] captureCols = {col - 1, col + 1};
        for (int captureCol : captureCols) {
            if (isValidSquare(forwardRow, captureCol)) {
                Piece target = board.getPiece(forwardRow, captureCol);

                if (target != null && target.getColor() != this.color) {
                    moves.add(new Move(this, row, col, forwardRow, captureCol, target));
                }
            }
        }

        return moves;
    }
}
