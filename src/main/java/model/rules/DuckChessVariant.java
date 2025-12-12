package model.rules;

import model.*;
import model.pieces.King;

import java.util.Collections;
import java.util.List;

/**
 * This class implements the Duck Chess variant with its rules.
 * Rules:
 * The duck is a barrier that pieces cannot capture or move through.
 * Each player’s turn has 2 steps: After moving a piece, the duck must be moved to a different and empty square.
 * There is no check or checkmate. Capture the opponent’s king to win!
 * The stalemated player wins!
 * Invented by: Dr. Tim Paulden
 * @see GameVariant
 * @see ClassicalVariant
 * @author Miklós Bácsi
 */
public class DuckChessVariant extends ClassicalVariant {

    /**
     * There are no checks or checkmates in Duck Chess.
     * @param board board to check given piece's legal moves on
     * @param piece piece to check its moves
     * @return legal moves
     */
    @Override
    public List<Move> getLegalMoves(Board board, Piece piece) {

        // DUCK MOVE
        if (board.isWaitingForDuck()) {
            // Only the Duck can move!
            if (piece.getType() == PieceType.DUCK) {
                return piece.getPseudoLegalMoves(board);
            } else {
                return Collections.emptyList(); // No other piece can move
            }
        }

        // Get Geometry Moves
        List<Move> moves = piece.getPseudoLegalMoves(board);

        // Add Special Moves (Reusing logic from ClassicalVariant)
        if (piece.getType() == PieceType.PAWN) {
            addEnPassantMoves(board, piece, moves);
            checkPromotions(moves);
        }
        if (piece.getType() == PieceType.KING) {
            // Castling logic uses isCheck(), which we return false below.
            // It also uses isSquareAttacked(), which we inherited.
            // This allows castling normally, but ignores "Castling out of check".
            addCastlingMoves(board, (King) piece, moves);
        }

        // REMOVED: isMoveSafe check!
        // In Duck Chess, you can move your king into check, or leave it in check.

        // Filter: Cannot capture the Duck
        moves.removeIf(m ->
                m.capturedPiece() != null &&
                m.capturedPiece().getType() == PieceType.DUCK
        );

        return moves;
    }


    // --- DISABLE STANDARD CHECK/MATE ---

    /**
     * Always returns false, because there are no checks in Duck Chess.
     * @param board board to check on
     * @param color color of the king
     * @return false
     */
    @Override
    public boolean isCheck(Board board, PieceColor color) {
        // No visual "Red" warning in Duck Chess
        return false;
    }

    /**
     * Always returns false, because there are no checkmates in Duck Chess.
     * @param board board to check on
     * @param color color of the king to check
     * @return false
     */
    @Override
    public boolean isCheckmate(Board board, PieceColor color) {
        // The game ends by King Capture, not Checkmate
        return false;
    }

    /**
     * In Duck Chess, the stalemated player wins.
     * @param board board to check on
     * @param color color of the king to check
     * @return whether there is a stalemate
     */
    @Override
    public boolean isStalemate(Board board, PieceColor color) {
        return !hasAnySafeMove(board, color);
    }

    /**
     * Helper for isStalemate, because we disabled checks.
     * @param board board to check on
     * @param color color of player
     * @return if player has any safe moves
     */
    private boolean hasAnySafeMove(Board board, PieceColor color) {
        // Iterate over all pieces of the current player
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece piece = board.getPiece(r, c);
                if (piece != null && piece.getColor() == color && piece.getType() != PieceType.DUCK) {

                    // Get potential moves (includes unsafe ones)
                    // We use getLegalMoves() so we respect Duck constraints and En Passant
                    List<Move> candidates = getLegalMoves(board, piece);

                    for (Move move : candidates) {
                        // Simulate Move
                        board.executeMove(move);

                        // Check Safety manually (We cannot use isCheck() because we overrode it to return false)
                        Piece king = board.findKing(color);
                        boolean isSafe = true;

                        // If King is on board (not captured by simulation logic), check attacks
                        if (king != null) {
                            // We use the protected helper from ClassicalVariant
                            if (isSquareAttacked(board, king.getRow(), king.getCol(), color)) {
                                isSafe = false;
                            }
                        }

                        // Undo Move
                        board.undoMove();

                        // If we found at least one safe move, it's NOT stalemate
                        if (isSafe) return true;
                    }
                }
            }
        }

        // No safe moves found -> Stalemate
        return false;
    }
}
