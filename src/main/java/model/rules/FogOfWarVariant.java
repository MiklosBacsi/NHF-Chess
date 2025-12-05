package model.rules;

import model.*;
import model.pieces.King;
import java.util.List;

/**
 * This class implements the Fog of War variant with its rules.
 * Rules:
 * Only the squares occupied or attacked by our pieces are visible, the others are covered in fog.
 * There is no check or checkmate. Capture the opponent’s king to win!
 * @author Miklós Bácsi
 */
public class FogOfWarVariant extends ClassicalVariant {

    /**
     * There are no checks or checkmates in FoW.
     * @param board board to check given piece's legal moves on
     * @param piece piece to check its moves
     * @return legal moves
     */
    @Override
    public List<Move> getLegalMoves(Board board, Piece piece) {
        // Get Geometry Moves
        List<Move> moves = piece.getPseudoLegalMoves(board);

        // Add Special Moves (Castling, En Passant)
        if (piece.getType() == PieceType.PAWN) {
            addEnPassantMoves(board, piece, moves);
            checkPromotions(moves);
        }
        if (piece.getType() == PieceType.KING) {
            // Allow castling (we ignore the "through check" rule for simplicity in FoW
            addCastlingMoves(board, (King) piece, moves);
        }

        // NO SAFETY CHECK: Allow moving into check

        return moves;
    }

    /**
     * Always returns false, because there are no checks in FoW.
     * @param board board to check on
     * @param color color of the king
     * @return false
     */
    @Override
    public boolean isCheck(Board board, PieceColor color) {
        return false;
    }

    /**
     * Always returns false, because there are no checkmates in FoW.
     * @param board board to check on
     * @param color color of the king to check
     * @return false
     */
    @Override
    public boolean isCheckmate(Board board, PieceColor color) { return false; }
}
