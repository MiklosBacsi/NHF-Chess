package model.pieces;

import model.*;
import java.util.ArrayList;
import java.util.List;

/**
 * This class implements the piece pawn in chess.
 * @see Piece
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

        int dRow = 0;
        int dCol = 0;

        // Define Forward Vector based on Color
        switch (color) {
            case WHITE, RED:    dRow = -1; break; // Up
            case BLACK, YELLOW: dRow = 1;  break; // Down
            case BLUE:          dCol = 1;  break; // Right (Blue starts Left)
            case GREEN:         dCol = -1; break; // Left  (Green starts Right)
            default: return moves;
        }

        int nextRow = row + dRow;
        int nextCol = col + dCol;

        // Single Step
        if (isValidSquare(nextRow, nextCol) && board.getPiece(nextRow, nextCol) == null) {
            boolean promotes = isChaturajiPromotion(nextRow, nextCol);
            MoveType type = promotes ? MoveType.PROMOTION : MoveType.NORMAL;
            moves.add(new Move(this, row, col, nextRow, nextCol, type, null));

            // Double Step (ONLY for Classical Colors)
            if (color == PieceColor.WHITE || color == PieceColor.BLACK) {
                int doubleRow = row + (dRow * 2);
                // Standard double step logic
                if (!hasMoved && isValidSquare(doubleRow, col) && board.getPiece(doubleRow, col) == null) {
                    moves.add(new Move(this, row, col, doubleRow, col));
                }
            }
        }

        // --- Captures ---
        // Calculate the two "forward diagonal" squares relative to the movement vector
        int[][] captureTargets = new int[2][2];
        if (dRow != 0) { // Vertical Movement
            captureTargets[0] = new int[]{row + dRow, col - 1};
            captureTargets[1] = new int[]{row + dRow, col + 1};
        } else { // Horizontal Movement
            captureTargets[0] = new int[]{row - 1, col + dCol};
            captureTargets[1] = new int[]{row + 1, col + dCol};
        }

        for (int[] target : captureTargets) {
            int r = target[0];
            int c = target[1];
            if (isValidSquare(r, c)) {
                Piece p = board.getPiece(r, c);
                if (p != null && p.getColor() != this.color && p.getColor() != PieceColor.SPECIAL) {
                    boolean promotes = isChaturajiPromotion(r, c);
                    MoveType type = promotes ? MoveType.PROMOTION : MoveType.NORMAL;
                    moves.add(new Move(this, row, col, r, c, type, p));
                }
            }
        }
        return moves;
    }

    /**
     * @param row row index
     * @param col column index
     * @return whether the square is a promotion square
     */
    private boolean isChaturajiPromotion(int row, int col) {
        return switch (color) {
            case RED -> row == 0;
            case YELLOW -> row == 7;
            case BLUE -> col == 7;
            case GREEN -> col == 0;
            default -> false;
        };
    }
}
