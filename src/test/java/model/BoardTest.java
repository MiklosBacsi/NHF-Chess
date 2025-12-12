package model;

import model.pieces.Pawn;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests Board's basic methods.
 * @see Board
 * @author Miklós Bácsi
 */
class BoardTest {

    private Board board;

    @BeforeEach
    void setUp() {
        board = new Board();
        board.resetBoard(); // Clear everything
    }

    // Verify placing a piece
    @Test
    void testPlacePiece() {
        Pawn pawn = new Pawn(PieceColor.WHITE, 3, 3);
        board.placePiece(pawn, 3, 3);

        assertEquals(pawn, board.getPiece(3, 3));
        assertEquals(PieceType.PAWN, board.getPiece(3, 3).getType());
    }

    // Execute a simple move and verify coordinates update
    @Test
    void testExecuteMove() {
        // Setup: Pawn at 1,1
        Pawn pawn = new Pawn(PieceColor.BLACK, 1, 1);
        board.placePiece(pawn, 1, 1);

        // Action: Move from (1,1) to (2,1)
        Move move = new Move(pawn, 1, 1, 2, 1,
                MoveType.NORMAL, null, true, null
        );
        board.executeMove(move);

        assertNull(board.getPiece(1, 1), "Old square should be empty");
        assertEquals(pawn, board.getPiece(2, 1), "New square should contain piece");
        assertEquals(2, pawn.getRow(), "Piece's row should update");
        assertEquals(1, pawn.getCol(), "Piece's column should not change");
    }

    // Player Death Logic
    @Test
    void testKillPlayer() {
        // Setup: Place a Red Pawn
        Pawn pawn = new Pawn(PieceColor.RED, 5, 5);
        board.placePiece(pawn, 5, 5);

        // Action: Kill Red
        board.killPlayer(PieceColor.RED);

        assertTrue(board.isPlayerDead(PieceColor.RED), "Red should be marked dead");
        assertEquals(PieceColor.GREY, board.getPiece(5, 5).getColor(), "Red pieces should turn Grey");
    }
}