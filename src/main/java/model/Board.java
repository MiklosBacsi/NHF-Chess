package model;

import model.pieces.*;
import java.util.Stack;

public class Board {
    private final Piece[][] squares = new Piece[8][8];

    // We need history to undo moves and check for En Passant availability
    private final Stack<Move> moveHistory = new Stack<>();

    public Board() {
        resetBoard();
    }

    public void resetBoard() {
        // Clear History
        moveHistory.clear();

        // Clear squares
        for(int r=0; r<8; r++)
            for(int c=0; c<8; c++)
                squares[r][c] = null;

        // Pawns
        for (int i = 0; i < 8; i++) {
            placePiece(new Pawn(PieceColor.BLACK, 1, i), 1, i);
            placePiece(new Pawn(PieceColor.WHITE, 6, i), 6, i);
        }

        // Heavy Pieces
        placeHeavyPieces(PieceColor.BLACK, 0);
        placeHeavyPieces(PieceColor.WHITE, 7);
    }

    private void placeHeavyPieces(PieceColor color, int row) {
        placePiece(new Rook(color, row, 0), row, 0);
        placePiece(new Knight(color, row, 1), row, 1);
        placePiece(new Bishop(color, row, 2), row, 2);
        placePiece(new Queen(color, row, 3), row, 3);
        placePiece(new King(color, row, 4), row, 4);
        placePiece(new Bishop(color, row, 5), row, 5);
        placePiece(new Knight(color, row, 6), row, 6);
        placePiece(new Rook(color, row, 7), row, 7);
    }

    public Piece getPiece(int row, int col) {
        if (row < 0 || row >= 8 || col < 0 || col >= 8) return null;
        return squares[row][col];
    }

    public void placePiece(Piece piece, int row, int col) {
        squares[row][col] = piece;
    }

    public Move getLastMove() {
        if (moveHistory.isEmpty()) return null;
        return moveHistory.peek();
    }

    // --- EXECUTE MOVE (with Special Logic) ---
    public void executeMove(Move move) {
        // Remove piece from start
        squares[move.startRow()][move.startCol()] = null;

        // Handle Captures (En Passant is special)
        if (move.type() == MoveType.EN_PASSANT) {
            int direction = (move.piece().getColor() == PieceColor.WHITE) ? 1 : -1;
            squares[move.endRow() + direction][move.endCol()] = null;
        } else {
            // Normal capture is implicitly handled by overwriting the end square
        }

        // Place piece at end
        squares[move.endRow()][move.endCol()] = move.piece();
        move.piece().move(move.endRow(), move.endCol());

        // Handle Castling (Moving the Rook)
        if (move.type() == MoveType.CASTLING) {
            boolean kingside = move.endCol() > move.startCol();
            int rookStartCol = kingside ? 7 : 0;
            int rookEndCol = kingside ? 5 : 3;
            int row = move.startRow();

            Piece rook = squares[row][rookStartCol];
            if (rook != null) {
                squares[row][rookStartCol] = null;
                squares[row][rookEndCol] = rook;
                rook.move(row, rookEndCol);
            }
        }

        // Handle Promotion (Simple Auto-Queen for now, UI handles choice later) !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
        if (move.type() == MoveType.PROMOTION) {
            Piece promotedPiece = new Queen(move.piece().getColor(), move.endRow(), move.endCol());
            squares[move.endRow()][move.endCol()] = promotedPiece;
        }

        moveHistory.push(move);
    }

    // --- UNDO MOVE (Crucial for validating Checks) ---
    public void undoMove() {
        if (moveHistory.isEmpty()) return;
        Move move = moveHistory.pop();

        // Move the acting piece back to the start
        squares[move.startRow()][move.startCol()] = move.piece();
        move.piece().move(move.startRow(), move.startCol());

        // Handle the Destination Square (Where the piece was just now)
        Piece captured = move.capturedPiece();
        if (captured != null) {
            // Capture
            if (move.type() == MoveType.EN_PASSANT) {
                // In En Passant, the destination square is actually empty
                squares[move.endRow()][move.endCol()] = null;

                // The captured pawn is "behind" the destination
                int direction = (move.piece().getColor() == PieceColor.WHITE) ? 1 : -1;
                squares[move.endRow() + direction][move.endCol()] = captured;
            } else {
                // Normal Capture: Put the dead piece back on the target square
                squares[move.endRow()][move.endCol()] = captured;
            }
        } else {
            // No Capture (Normal move)
            // The destination square must be cleared because the piece went back to start
            squares[move.endRow()][move.endCol()] = null;
        }

        // Revert Castling (If necessary)
        if (move.type() == MoveType.CASTLING) {
            boolean kingside = move.endCol() > move.startCol();
            int rookStartCol = kingside ? 7 : 0;
            int rookEndCol = kingside ? 5 : 3;
            int row = move.startRow();

            Piece rook = squares[row][rookEndCol];

            // Move rook back from end to start
            if (rook != null) {
                squares[row][rookEndCol] = null;
                squares[row][rookStartCol] = rook;

                rook.move(row, rookStartCol);

                // Since we only allow castling if the rook hasn't moved, undoing a castle means the rook hasn't moved
                rook.setHasMoved(false);
            }
        }

        // Restore 'hasMoved' state
        if (move.isFirstMove()) {
            move.piece().setHasMoved(false);
        }
    }

    // Helper to find King for check validation
    public Piece findKing(PieceColor color) {
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = squares[r][c];
                if (p != null && p.getType() == PieceType.KING && p.getColor() == color) {
                    return p;
                }
            }
        }
        return null; // Should never happen
    }
}
