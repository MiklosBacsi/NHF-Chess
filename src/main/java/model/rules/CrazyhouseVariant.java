package model.rules;

import model.*;
import model.pieces.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This class implements the Crazyhouse variant with its rules.
 * Rules:
 * Captured enemy pieces are placed in your reserve.
 * Instead of stepping with a piece, you can grab a piece from your reserve and drop it on the board to an empty square.
 * Pawns cannot be dropped to the first and last rank.
 * @see GameVariant
 * @see ClassicalVariant
 * @author Miklós Bácsi
 */
public class CrazyhouseVariant extends ClassicalVariant {

    /**
     * Standard legal moves + DROP move
     * @param board board to check given piece's legal moves on
     * @param piece piece to check its moves
     * @return legal moves
     */
    @Override
    public List<Move> getLegalMoves(Board board, Piece piece) {
        // Piece is on the board (Normal move)
        if (piece.getRow() != -1 && piece.getCol() != -1) {
            return super.getLegalMoves(board, piece);
        }

        // Piece is in Reserve (Drop move)
        List<Move> legalDrops = new ArrayList<>();

        // Iterate every square on the board
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                // Square must be empty
                if (board.getPiece(r, c) == null) {

                    // Pawn Restriction: Cannot drop on 1st or 8th rank
                    if (piece.getType() == PieceType.PAWN) {
                        if (r == 0 || r == 7) continue;
                    }

                    // Create Drop Move
                    Move drop = Move.createDropMove(piece, r, c);

                    // King Safety Check (Standard Logic)
                    // We must ensure dropping this piece doesn't leave our King in check
                    if (isMoveSafe(board, drop, piece.getColor())) {
                        legalDrops.add(drop);
                    }
                }
            }
        }
        return legalDrops;
    }

    /**
     * Checks whether the player has any legal moves (standard and drop), so that he is not checkmated.
     * @param board board to check on
     * @param color color of the player to check
     * @return if the player has any legal moves
     */
    @Override
    protected boolean hasNoLegalMoves(Board board, PieceColor color) {
        // --- Check On-Board Moves (Standard Logic) ---
        if (!super.hasNoLegalMoves(board, color)) {
            // If there is a move on the board, we are not mated.
            return false;
        }

        // --- Check Reserve Drops ---
        Map<PieceType, Integer> reserve = board.getReserve(color);
        List<PieceType> typesToCheck = new ArrayList<>(reserve.keySet()); // new because it will be modified

        // Check pieces in reserve: whether a piece can be dropped on the board so that it defends the check
        for (PieceType type : typesToCheck) {
            if (reserve.get(type) > 0) {
                // Create a temporary "virtual" piece to test moves
                Piece virtualPiece = createDummyPiece(color, type);

                // If this piece has ANY valid drop squares (that block check), we are safe!
                if (!getLegalMoves(board, virtualPiece).isEmpty()) {
                    return false;
                }
            }
        }

        // No board moves & no drop moves -> Checkmate
        return true;
    }

    /**
     * Helper to create the specific object type so getLegalMoves logic works (testing drop moves).
     * @param color color of defender
     * @param type type of the piece testing to be dropped
     * @return dummy piece created
     */
    private Piece createDummyPiece(PieceColor color, PieceType type) {
        // -1, -1 indicates it's off-board
        return switch (type) {
            case ROOK -> new Rook(color, -1, -1);
            case KNIGHT -> new Knight(color, -1, -1);
            case BISHOP -> new Bishop(color, -1, -1);
            case QUEEN -> new Queen(color, -1, -1);
            default -> new Pawn(color, -1, -1);
        };
    }
}
