package model.rules;

import model.*;
import model.pieces.*;

import java.util.ArrayList;
import java.util.List;

/**
 * This class implements the Chaturaji variant with its rules.
 * Rules:
 * 4-player variant with players: red, blue, yellow and green.
 * Collect most points to win. Points can be collected by capturing pieces and checking kings.
 * When a king is captured, his pieces will be dead (grey) and can be captured.
 * The rook is replaced by the sail boat, which moves the same way, but cannot castle.
 * For more details, open the help dialog!
 * @author Miklós Bácsi
 */
public class ChaturajiVariant implements GameVariant {

    /**
     * There are no checks or checkmates in Chaturaji.
     * @param board board to check given piece's legal moves on
     * @param piece piece to check its moves
     * @return legal moves
     */
    @Override
    public List<Move> getLegalMoves(Board board, Piece piece) {
        // Grey pieces cannot move
        if (piece.getColor() == PieceColor.GREY) return new ArrayList<>();

        // Get Geometry Moves
        List<Move> moves = piece.getPseudoLegalMoves(board);

        // Handle Promotion (Pawn -> Boat)
        // The Pawn class already marks PROMOTION type based on row/col.
        // We just need to ensure the Board executes it as BOAT.

        // NO KING SAFETY (Kings can be captured)

        return moves;
    }

    /**
     * Scoring Logic (should be called by the UI/Controller after a move).
     * @param board board to check on
     * @param move move that was made
     */
    public void handlePostMoveLogic(Board board, Move move) {
        PieceColor player = move.piece().getColor();

        // Capture Points
        if (move.capturedPiece() != null) {
            // Capturing Living Pieces
            int points = getPieceValue(move.capturedPiece().getType());

            // Capturing Dead Pieces
            if (move.capturedPiece().getColor() == PieceColor.GREY) {
                // Dead pieces worth 0 points
                points = 0;

                // Dead Kings still worth 3 points
                if (move.capturedPiece().getType() == PieceType.KING) {
                    points = 3;
                }
            }

            board.addScore(player, points);

            // Player Death Logic (Live King Capture)
            if (move.capturedPiece().getType() == PieceType.KING &&
                    move.capturedPiece().getColor() != PieceColor.GREY) {
                board.killPlayer(move.capturedPiece().getColor());
            }
        }

        // Check Bonus (Double/Triple Check)
        int checks = countChecks(board, player);
        if (checks == 2) board.addScore(player, 1);
        else if (checks == 3) board.addScore(player, 5);

        // PROMOTION: handled by Board.executeMove (pawns always promote to boats)
    }

    /**
     * @param type type of the piece
     * @return how many points the piece is worth
     */
    private int getPieceValue(PieceType type) {
        return switch (type) {
            case PAWN -> 1;
            case KING, KNIGHT -> 3;
            case BISHOP, BOAT, ROOK -> 5;
            default -> 0;
        };
    }

    /**
     * Counts how many kings the attacker checks.
     * @param board board to check on
     * @param attackerColor color of the attacker
     * @return number of kings checked by the attacker
     */
    private int countChecks(Board board, PieceColor attackerColor) {
        int count = 0;
        PieceColor[] enemies = {PieceColor.RED, PieceColor.BLUE, PieceColor.YELLOW, PieceColor.GREEN};
        for (PieceColor enemy : enemies) {
            if (enemy != attackerColor && !board.isPlayerDead(enemy)) {
                if (isCheck(board, enemy)) count++;
            }
        }
        return count;
    }

    /**
     * We need to implement it here instead of inheriting from Classical because we would need to overwrite isSquareAttacked.
     * @param board board to check on
     * @param color color of the player
     * @return whether the player is in check
     */
    @Override
    public boolean isCheck(Board board, PieceColor color) {
        Piece king = board.findKing(color);
        if (king == null) return false;

        // Check all enemies
        for (int r=0; r<8; r++) {
            for (int c=0; c<8; c++) {
                Piece p = board.getPiece(r, c);
                if (p != null && p.getColor() != color && p.getColor() != PieceColor.SPECIAL) {
                    for(Move m : p.getPseudoLegalMoves(board)) {
                        if (m.endRow() == king.getRow() && m.endCol() == king.getCol()) return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * There is no checkmate in Chaturaji.
     * @param board board to check on
     * @param color color of the player
     * @return false
     */
    @Override
    public boolean isCheckmate(Board board, PieceColor color) { return false; }

    /**
     * There is no stalemate in Chaturaji.
     * @param board board to check on
     * @param color color of the player
     * @return false
     */
    @Override
    public boolean isStalemate(Board board, PieceColor color) { return false; }
}
