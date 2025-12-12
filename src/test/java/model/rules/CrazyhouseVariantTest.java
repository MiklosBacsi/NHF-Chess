package model.rules;

import model.*;
import model.pieces.King;
import model.pieces.Knight;
import model.pieces.Pawn;
import model.pieces.Rook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests CrazyhouseVariant's basic methods.
 * @see CrazyhouseVariant
 * @author Miklós Bácsi
 */
class CrazyhouseVariantTest {

    private Board board;
    private CrazyhouseVariant rules;

    @BeforeEach
    void setUp() {
        board = new Board();
        board.resetBoard();
        // Crazyhouse mode must be enabled on board to use reserves
        board.setCrazyhouseMode(true);
        rules = new CrazyhouseVariant();
    }

    // Drop Logic - Can drop a piece from reserve
    @Test
    void testDropFromReserve() {
        // Give White a Knight in reserve
        board.addToReserve(PieceColor.WHITE, PieceType.KNIGHT);

        // Create a virtual Knight to represent the hand
        Knight virtualKnight = new Knight(PieceColor.WHITE, -1, -1);

        List<Move> moves = rules.getLegalMoves(board, virtualKnight);

        // Should be able to drop on any empty square (64 squares)
        assertEquals(64, moves.size());
        assertEquals(MoveType.DROP, moves.getFirst().type());
    }

    // Pawn Restrictions - Cannot drop Pawn on rank 1 or 8 (row 0 and 7)
    @Test
    void testPawnDropRestrictions() {
        board.addToReserve(PieceColor.WHITE, PieceType.PAWN);
        Pawn virtualPawn = new Pawn(PieceColor.WHITE, -1, -1);

        List<Move> moves = rules.getLegalMoves(board, virtualPawn);

        // Full board 64, Minus Row 0 (8 squares) and Row 7 (8 squares) = 48 valid squares.
        assertEquals(48, moves.size());

        // Ensure no moves target row 0 or 7
        boolean badDrop = moves.stream().anyMatch(m -> m.endRow() == 0 || m.endRow() == 7);
        assertFalse(badDrop, "Should not allow dropping Pawn on back ranks");
    }

    // Pocket Defense - Avoid checkmate by dropping a piece
    @Test
    void testAvoidMateByDrop() {
        // Setup: White King at 0,0. Black Rook at (0,7) and (1,6) --> Checkmate
        // White has NO moves on board.
        King whiteKing = new King(PieceColor.WHITE, 0, 0);
        Rook blackRook1 = new Rook(PieceColor.BLACK, 0, 7);
        Rook blackRook2 = new Rook(PieceColor.BLACK, 1, 6);

        board.placePiece(whiteKing, 0, 0);
        board.placePiece(blackRook1, 0, 7);
        board.placePiece(blackRook2, 1, 6);

        // Confirm it LOOKS like mate initially (Standard logic)
        ClassicalVariant standardRules = new ClassicalVariant();
        assertTrue(standardRules.isCheckmate(board, PieceColor.WHITE));

        // Give White a Knight in reserve
        board.addToReserve(PieceColor.WHITE, PieceType.KNIGHT);

        // White can drop a piece on the board to defend the check
        assertFalse(rules.isCheckmate(board, PieceColor.WHITE),
                "Should NOT be checkmate if we can drop a piece to block");
    }
}