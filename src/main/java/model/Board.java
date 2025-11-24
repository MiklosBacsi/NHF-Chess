package model;

import model.pieces.*;

public class Board {
    private final Piece[][] squares = new Piece[8][8];

    public Board() {
        resetBoard();
    }

    public void resetBoard() {
        // Clear logic
        for(int r=0; r<8; r++) for(int c=0; c<8; c++) squares[r][c] = null;

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

    // Removes piece from old spot, puts in new spot
    public void executeMove(Move move) {
        squares[move.startRow()][move.startCol()] = null;
        squares[move.endRow()][move.endCol()] = move.piece();
        move.piece().move(move.endRow(), move.endCol());
    }
}
