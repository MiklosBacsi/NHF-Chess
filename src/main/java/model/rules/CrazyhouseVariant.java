package model.rules;

import model.*;
import model.pieces.*;

import java.util.ArrayList;
import java.util.List;

/**
 * This class implements the Crazyhouse variant with its rules.
 * Rules:
 * Captured enemy pieces are placed in your reserve.
 * Instead of stepping with a piece, you can grab a piece from your reserve and drop it on the board to an empty square.
 * Pawns cannot be dropped to the first and last rank.
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

    // We reuse isMoveSafe, isCheck, etc. from ClassicalVariant
}
