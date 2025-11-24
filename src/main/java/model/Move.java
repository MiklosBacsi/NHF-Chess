package model;

public record Move(Piece piece, int startRow, int startCol, int endRow, int endCol) {

    // Helper for debugging
    public String toString() {
        return toChessCoordinate(startCol, startRow) + " -> " + toChessCoordinate(endCol, endRow);
    }

    private String toChessCoordinate(int col, int row) {
        char letter = (char) ('a' + col);
        int number = 8 - row; // Row 0 is rank 8, Row 7 is rank 1
        return "" + letter + number;
    }
}
