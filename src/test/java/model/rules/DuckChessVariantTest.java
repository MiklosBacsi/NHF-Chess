package model.rules;

import model.*;
import model.pieces.Duck;
import model.pieces.Rook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests DuckChessVariant's basic methods.
 * @see DuckChessVariant
 * @author Miklós Bácsi
 */
class DuckChessVariantTest {

    private Board board;
    private DuckChessVariant rules;

    @BeforeEach
    void setUp() {
        board = new Board();
        board.resetBoard();
        rules = new DuckChessVariant();
    }

    // Phase Lock - Pieces cannot move when waiting for Duck
    @Test
    void testPieceLockWhenWaitingForDuck() {
        // Setup: White Rook at (0,0)
        Rook rook = new Rook(PieceColor.WHITE, 0, 0);
        board.placePiece(rook, 0, 0);

        // Flag set to TRUE (Player just moved, now must move duck)
        board.setWaitingForDuck(true);

        List<Move> moves = rules.getLegalMoves(board, rook);

        // Should be empty because it's the Duck's turn to move
        assertTrue(moves.isEmpty(), "Normal pieces should not move during Duck Phase");
    }

    // Duck Movement - Duck should be able to move to empty squares
    @Test
    void testDuckMovements() {
        // Setup: Duck at (4,4)
        Duck duck = new Duck(4, 4);
        board.placePiece(duck, 4, 4);
        board.setWaitingForDuck(true); // Enable Duck Phase

        List<Move> moves = rules.getLegalMoves(board, duck);

        // Board is empty except duck. 64 - 1 = 63 squares available (Duck can teleport anywhere)
        assertEquals(63, moves.size());
    }

    // Duck Invincibility - Pieces cannot capture the Duck
    @Test
    void testCannotCaptureDuck() {
        // Setup: White Rook at (0,0), Duck at (0,1)
        Rook rook = new Rook(PieceColor.WHITE, 0, 0);
        Duck duck = new Duck(0, 1);

        board.placePiece(rook, 0, 0);
        board.placePiece(duck, 0, 1);
        board.setWaitingForDuck(false); // Normal Phase

        List<Move> moves = rules.getLegalMoves(board, rook);

        // Rook should be blocked by Duck (cannot move to 0,1 or beyond)
        // Since Duck is at 0,1, Rook has NO moves on this rank.

        boolean capturesDuck = moves.stream().anyMatch(m ->
                m.endRow() == 0 && m.endCol() == 1
        );

        assertFalse(capturesDuck, "Rook should not be able to capture the Duck");
    }
}