package model;

import java.util.ArrayList;
import java.util.List;

/**
 * This abstract base class represents a chess piece.
 * @author Miklós Bácsi
 */
public abstract class Piece {

    protected int row;
    protected int col;
    protected final PieceColor color;
    protected final PieceType type;

    // Essential for Castling and Pawn double-push
    protected boolean hasMoved = false;

    /**
     * Constructor that initializes values.
     * @param type type of the piece
     * @param color color of the piece
     * @param row row index of the piece
     * @param col column index of the piece
     */
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
     * @param board board to check on
     * @param directions 2D vectors, representing the directions
     * @return the "geometrically" possible sliding moves
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
                        moves.add(new Move(this, row, col, targetRow, targetCol, targetPiece));
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
     * @param board board to check on
     * @param offsets offsets (direction vectors) of a piece that determine where a piece can move relative to its stating position
     * @return "geometrically" possible moves for piece
     */
    protected List<Move> getSingleStepMoves(Board board, int[][] offsets) {
        List<Move> moves = new ArrayList<>();

        for (int[] offset : offsets) {
            int targetRow = this.row + offset[0];
            int targetCol = this.col + offset[1];

            if (isValidSquare(targetRow, targetCol)) {
                Piece targetPiece = board.getPiece(targetRow, targetCol);

                if (targetPiece == null) {
                    // Empty square: Normal Move
                    moves.add(new Move(this, row, col, targetRow, targetCol));
                } else if (targetPiece.getColor() != this.color) {
                    // Enemy piece: Capture Move!
                    // IMPORTANT: We must pass 'targetPiece' to the Move constructor
                    moves.add(new Move(this, row, col, targetRow, targetCol, targetPiece));
                }
            }
        }
        return moves;
    }


    // --- UTILITIES ---

    /**
     * Helper to check if a square valid
     * @param row row index of square
     * @param col column index of square
     * @return whether the square (defined by row and col) is within the board or not
     */
    public boolean isValidSquare(int row, int col) {
        return row >= 0 && row < 8 && col >= 0 && col < 8;
    }

    /**
     * Moves the piece into the new position.
     * @param newRow row index of new position
     * @param newCol column index of new position
     */
    public void move(int newRow, int newCol) {
        this.row = newRow;
        this.col = newCol;
        this.hasMoved = true;
    }

    // --- GETTERS (Used by View) ---

    /**
     * @return file name of piece for the texture
     */
    public String getFilename() {
        return color.name().toLowerCase() + "-" + type.name().toLowerCase();
    }

    /**
     * @return row index of piece
     */
    public int getRow() { return row; }

    /**
     * @return column index of piece
     */
    public int getCol() { return col; }

    /**
     * @return color of piece
     */
    public PieceColor getColor() { return color; }

    /**
     * @return type of piece
     */
    public PieceType getType() { return type; }

    /**
     * @return whether piece has moved
     */
    public boolean hasMoved() { return hasMoved; }

    /**
     * Sets piece's state whether it has moved.
     * @param hasMoved change piece's state to this
     */
    public void setHasMoved(boolean hasMoved) { this.hasMoved = hasMoved; }
}
