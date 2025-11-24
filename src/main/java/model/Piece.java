package model;

import java.util.ArrayList;
import java.util.List;

public abstract class Piece {

    protected int row;
    protected int col;
    protected final PieceColor color;
    protected final PieceType type;

    // Essential for Castling and Pawn double-push
    protected boolean hasMoved = false;

    public Piece(PieceType type, PieceColor color, int row, int col) {
        this.type = type;
        this.color = color;
        this.row = row;
        this.col = col;
    }

    // It returns geometric moves (ignoring "Check" or "King Safety"), every piece must implement it
    public abstract List<Move> getPseudoLegalMoves(Board board);


    // --- SMART HELPERS (Strategy Reuse) ---

    /**
     * Logic for Rook, Bishop, and Queen.
     * Iterates in specific directions until it hits a wall or piece.
     * @param directions 2D vectors, representing the directions
     */
    protected List<Move> getSlidingMoves(Board board, int[][] directions) {
        List<Move> moves = new ArrayList<>();

        for (int[] dir : directions) {
            int dRow = dir[0];
            int dCol = dir[1];

            for (int i = 1; i < 8; i++) { // Max distance is 7 squares
                int targetRow = this.row + (dRow * i);
                int targetCol = this.col + (dCol * i);

                // Check Board Bounds
                if (!isValidSquare(targetRow, targetCol)) {
                    break;
                }

                Piece targetPiece = board.getPiece(targetRow, targetCol);

                if (targetPiece == null) {
                    // Empty square: Add move and Keep going
                    moves.add(new Move(this, row, col, targetRow, targetCol));
                } else {
                    // Occupied square
                    if (targetPiece.getColor() != this.color) {
                        // Capture! Add move, but Stop sliding
                        moves.add(new Move(this, row, col, targetRow, targetCol));
                    }
                    // If friendly piece, just Stop sliding
                    break;
                }
            }
        }
        return moves;
    }

    /**
     * Logic for Knight and King.
     * Checks specific offsets (jumps/steps) exactly once.
     */
    protected List<Move> getSingleStepMoves(Board board, int[][] offsets) {
        List<Move> moves = new ArrayList<>();

        for (int[] offset : offsets) {
            int targetRow = this.row + offset[0];
            int targetCol = this.col + offset[1];

            if (isValidSquare(targetRow, targetCol)) {
                Piece targetPiece = board.getPiece(targetRow, targetCol);

                // Move if empty OR capture enemy
                if (targetPiece == null || targetPiece.getColor() != this.color) {
                    moves.add(new Move(this, row, col, targetRow, targetCol));
                }
            }
        }
        return moves;
    }


    // --- UTILITIES ---

    public boolean isValidSquare(int r, int c) {
        return r >= 0 && r < 8 && c >= 0 && c < 8;
    }

    public void move(int newRow, int newCol) {
        this.row = newRow;
        this.col = newCol;
        this.hasMoved = true;
    }

    // --- GETTERS (Used by View) ---

    public String getFilename() {
        return color.name().toLowerCase() + "-" + type.name().toLowerCase();
    }

    public int getRow() { return row; }
    public int getCol() { return col; }
    public PieceColor getColor() { return color; }
    public PieceType getType() { return type; }
    public boolean hasMoved() { return hasMoved; }
}
