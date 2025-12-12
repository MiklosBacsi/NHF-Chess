package model.pieces;

import model.Board;
import model.Move;
import model.PieceColor;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests Pawn's basic moves: single-step, double-step.
 * @see Pawn
 * @author Miklós Bácsi
 */
class PawnTest {

    // Classical White Pawn (Double Step)
    @Test
    void testWhitePawnInitialMoves() {
        Board board = new Board();
        board.resetBoard(); // Clears everything

        Pawn pawn = new Pawn(PieceColor.WHITE, 6, 4); // Standard e2 position (row 6)
        board.placePiece(pawn, 6, 4);

        List<Move> moves = pawn.getPseudoLegalMoves(board);

        // Should have 2 moves: Single step (5,4) and Double step (4,4)
        assertEquals(2, moves.size());
        assertTrue(moves.stream().anyMatch(m -> m.endRow() == 5));
        assertTrue(moves.stream().anyMatch(m -> m.endRow() == 4));
    }

    // Chaturaji Blue Pawn (Moves Right)
    @Test
    void testBluePawnDirection() {
        Board board = new Board();
        board.resetBoard();

        // Blue moves RIGHT (col + 1)
        Pawn p = new Pawn(PieceColor.BLUE, 4, 4);
        board.placePiece(p, 4, 4);

        List<Move> moves = p.getPseudoLegalMoves(board);

        // Should have 1 move: 4,5
        assertEquals(1, moves.size());
        assertEquals(5, moves.getFirst().endCol());
    }
}