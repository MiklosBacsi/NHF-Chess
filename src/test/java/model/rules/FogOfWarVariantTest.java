package model.rules;

import model.*;
import model.pieces.King;
import model.pieces.Rook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests FogOfWarVariant's basic methods.
 * @see FogOfWarVariant
 * @author Miklós Bácsi
 */
class FogOfWarVariantTest {

    private Board board;
    private FogOfWarVariant rules;

    @BeforeEach
    void setUp() {
        board = new Board();
        board.resetBoard();
        rules = new FogOfWarVariant();
    }

    // Can move King into check
    @Test
    void testAllowMovingIntoCheck() {
        // Setup: White King at (0,4); Black Rook at (0,7)
        King king = new King(PieceColor.WHITE, 0, 4);
        Rook enemyRook = new Rook(PieceColor.BLACK, 0, 7);

        board.placePiece(king, 0, 4);
        board.placePiece(enemyRook, 0, 7);

        // Verify FoW ignores check
        assertFalse(rules.isCheck(board, PieceColor.WHITE), "Fog of War should not report Check status");

        // Verify King can move to (0,5), which is attacked by the Rook
        // In Classical, this is illegal. In FoW, it is legal.
        var moves = rules.getLegalMoves(board, king);

        boolean containsSuicide = moves.stream()
                .anyMatch(m -> m.endRow() == 0 && m.endCol() == 5);

        assertTrue(containsSuicide, "King should be allowed to move into the Rook's file");
    }
}