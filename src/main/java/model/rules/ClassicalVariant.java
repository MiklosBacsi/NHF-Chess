package model.rules;

import model.*;
import model.pieces.King;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ClassicalVariant implements GameVariant {

    @Override
    public List<Move> getLegalMoves(Board board, Piece piece) {
        List<Move> legalMoves = new ArrayList<>();

        // Get Geometry Moves (Pseudo-legal)
        List<Move> pseudoMoves = piece.getPseudoLegalMoves(board);

        // Add Special Moves (Castling, En Passant)
        if (piece.getType() == PieceType.KING) {
            addCastlingMoves(board, (King) piece, pseudoMoves);
        }
        if (piece.getType() == PieceType.PAWN) {
            addEnPassantMoves(board, piece, pseudoMoves);
            checkPromotions(pseudoMoves); // Mark promotion moves
        }

        // FILTER: The "King Safety" Rule (a piece cannot move, if it leaves the king in check)
        for (Move move : pseudoMoves) {
            if (isMoveSafe(board, move, piece.getColor())) {
                legalMoves.add(move);
            }
        }

        return legalMoves;
    }

    private boolean isMoveSafe(Board board, Move move, PieceColor myColor) {
        // Simulate Move
        board.executeMove(move);

        // Check if King is under attack
        boolean isSafe = !isCheck(board, myColor);

        // Undo Move
        board.undoMove();

        return isSafe;
    }

    @Override
    public boolean isCheck(Board board, PieceColor color) {
        Piece king = board.findKing(color);
        if (king == null) return false; // Should not happen

        // Check if the King's current position is attacked
        return isSquareAttacked(board, king.getRow(), king.getCol(), color);
    }

    // Helper: Checks if a specific square is under attack by the enemy
    private boolean isSquareAttacked(Board board, int targetRow, int targetCol, PieceColor defenderColor) {
        PieceColor enemyColor = (defenderColor == PieceColor.WHITE) ? PieceColor.BLACK : PieceColor.WHITE;

        // Iterate over all squares to find enemy pieces
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = board.getPiece(r, c);
                // If it's an enemy piece
                if (p != null && p.getColor() == enemyColor) {
                    // Check its pseudo-legal moves (Geometry only)
                    List<Move> attacks = p.getPseudoLegalMoves(board);
                    for (Move m : attacks) {
                        if (m.endRow() == targetRow && m.endCol() == targetCol) {
                            return true; // The square is under attack!
                        }
                    }
                }
            }
        }
        return false;
    }


    // --- SPECIAL MOVES LOGIC ---

    private void addCastlingMoves(Board board, King king, List<Move> moves) {
        if (king.hasMoved() || isCheck(board, king.getColor())) return;

        int row = king.getRow();
        // Kingside (Target column: 6)
        if (canCastle(board, row, 7, 5, 6)) {
            moves.add(new Move(king, row, 4, row, 6, MoveType.CASTLING, null));
        }
        // Queenside (Target column: 2)
        if (canCastle(board, row, 0, 3, 2)) {
            moves.add(new Move(king, row, 4, row, 2, MoveType.CASTLING, null));
        }
    }

    private boolean canCastle(Board board, int row, int rookCol, int empty1, int empty2) {
        Piece rook = board.getPiece(row, rookCol);
        // Rook must exist, allow castling, and not have moved
        if (rook == null || rook.getType() != PieceType.ROOK || rook.hasMoved()) return false;

        // Path must be empty of pieces
        if (board.getPiece(row, empty1) != null || board.getPiece(row, empty2) != null) return false;

        // The square the King passes through cannot be under attack!
        Piece king = board.getPiece(row, 4); // The King is at column 4 (e-file)
        if (isSquareAttacked(board, row, empty1, king.getColor())) {
            return false;
        }

        // Also, for Queenside castling, there is an extra square (b1/b8) that must be empty, even though the King doesn't step on it.
        // kingside distance is 2 squares, queenside is 3.
        if (Math.abs(rookCol - 4) > 3) {
            // This is queenside. Check the extra square (b-file, col 1)
            if (board.getPiece(row, 1) != null) return false;
        }

        return true;
    }

    private void addEnPassantMoves(Board board, Piece pawn, List<Move> moves) {
        Move lastMove = board.getLastMove();
        if (lastMove == null) return;

        Piece lastPiece = lastMove.piece();
        // Condition: Last move was a Pawn moving 2 squares
        if (lastPiece.getType() == PieceType.PAWN && Math.abs(lastMove.startRow() - lastMove.endRow()) == 2) {
            // Condition: Enemy pawn is adjacent to my pawn
            if (lastMove.endRow() == pawn.getRow() && Math.abs(lastMove.endCol() - pawn.getCol()) == 1) {
                int direction = (pawn.getColor() == PieceColor.WHITE) ? -1 : 1;
                moves.add(new Move(
                        pawn,
                        pawn.getRow(), pawn.getCol(),
                        pawn.getRow() + direction, lastMove.endCol(), // Move to empty square behind
                        MoveType.EN_PASSANT,
                        lastPiece // We capture the piece that just moved
                ));
            }
        }
    }

    private void checkPromotions(List<Move> moves) {
        // If a pawn move reaches rank 0 or 7, tag it as Promotion, for that we need to iterate and replace them
        for (int i = 0; i < moves.size(); i++) {
            Move m = moves.get(i);
            if ((m.endRow() == 0 || m.endRow() == 7)) {
                // Replace with Promotion Move
                moves.set(i, new Move(m.piece(), m.startRow(), m.startCol(), m.endRow(), m.endCol(), MoveType.PROMOTION, m.capturedPiece()));
            }
        }
    }

    @Override
    public boolean isCheckmate(Board board, PieceColor color) {
        if (!isCheck(board, color)) return false;
        return hasNoLegalMoves(board, color);
    }

    @Override
    public boolean isStalemate(Board board, PieceColor color) {
        if (isCheck(board, color)) return false;
        return hasNoLegalMoves(board, color);
    }

    private boolean hasNoLegalMoves(Board board, PieceColor color) {
        // Iterate all pieces of this color, see if ANY can move
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = board.getPiece(r, c);
                if (p != null && p.getColor() == color) {
                    if (!getLegalMoves(board, p).isEmpty()) return false;
                }
            }
        }
        return true;
    }
}
