package model.rules;

import model.*;
import model.pieces.Boat;
import model.pieces.King;
import model.pieces.Pawn;
import model.pieces.Rook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests ChaturajiVariant's basic methods mostly for collecting points.
 * @see ChaturajiVariant
 * @author Miklós Bácsi
 */
class ChaturajiVariantTest {
    private Board board;
    private ChaturajiVariant rules;

    @BeforeEach
    void setUp() {
        board = new Board();
        board.resetBoard(); // Start empty
        rules = new ChaturajiVariant();
    }

    // Scoring - Capturing a piece
    @Test
    void testScoreOnCapture() {
        // Setup: Red Rook captures Blue Pawn
        Rook redRook = new Rook(PieceColor.RED, 4, 4);
        Pawn bluePawn = new Pawn(PieceColor.BLUE, 4, 5);

        // Red rook captures blue pawn
        Move captureMove = new Move(redRook, 4, 4, 4, 5,
                MoveType.NORMAL, bluePawn, false, null
        );

        // Execute logic (simulate what BoardPanel calls)
        rules.handlePostMoveLogic(board, captureMove);

        // Pawn is worth 1 point
        assertEquals(1, board.getScore(PieceColor.RED), "Red's point should be incremented by 1");
    }

    // Scoring - Capturing a Dead King
    @Test
    void testScoreOnDeadKingCapture() {
        Rook redRook = new Rook(PieceColor.RED, 4, 4);
        King greyKing = new King(PieceColor.GREY, 4, 5); // Dead king

        // Red rook captures grey king
        Move captureMove = new Move(redRook, 4, 4, 4, 5,
                MoveType.NORMAL, greyKing, false, null
        );

        // Execute logic
        rules.handlePostMoveLogic(board, captureMove);

        // Dead King is worth 3 points
        assertEquals(3, board.getScore(PieceColor.RED), "Red's point should be incremented by 3");
    }

    // Check Detection
    @Test
    void testIsCheck() {
        // Setup: Red King at 0,0. Blue Rook at 0,7 (attacking row 0).
        King redKing = new King(PieceColor.RED, 0, 0);
        Rook blueRook = new Rook(PieceColor.BLUE, 0, 7);

        King blueKing = new King(PieceColor.BLUE, 5, 5);

        board.placePiece(redKing, 0, 0);
        board.placePiece(blueRook, 0, 7);
        board.placePiece(blueKing, 5, 5);

        assertTrue(rules.isCheck(board, PieceColor.RED), "Red King should be in check from Blue Rook");
        assertFalse(rules.isCheck(board, PieceColor.BLUE), "Blue King should not be in check");
    }

    // Impossible to Catch Up
    @Test
    void testImpossibleToCatchUp1() {
        // Setup: Red has 20 points. Blue has 0. Only Blue King remains.
        board.addScore(PieceColor.RED, 20);
        board.addScore(PieceColor.BLUE, 5);

        King redKing = new King(PieceColor.RED, 0, 0);
        King blueKing = new King(PieceColor.BLUE, 5, 5);
        King yellowKing = new King(PieceColor.YELLOW, 2, 3);
        King greenKing = new King(PieceColor.GREEN, 5, 4);

        Boat redBoat = new Boat(PieceColor.RED, 1, 1);
        Boat yellowBoat = new Boat(PieceColor.YELLOW, 6, 6);

        // Place 4 kings and 2 boats on board
        board.placePiece(redKing, 0, 0);
        board.placePiece(blueKing, 5, 5);
        board.placePiece(yellowKing, 2, 3); // can still be captured for 3 points if dead
        board.placePiece(greenKing, 5, 4);  // can still be captured for 3 points if dead
        board.placePiece(redBoat, 1, 1);    // worth 5 points
        board.placePiece(yellowBoat, 6, 6); // worth 0 points if dead

        // Kill Yellow & Green so only 2 alive
        board.killPlayer(PieceColor.YELLOW);
        board.killPlayer(PieceColor.GREEN);

        /*
         * If there is a dead king on the board, it's still worth 3 point.
         * Therefore, blue could collect: 3 + 3 + 3 + 5 points (1 alive and 2 dead kings, 1 boat)
         * So, blue's maximum potancial points: 5 + 14 < 20 --> blue has no chance of catching up to red
         */

        assertTrue(rules.isImpossibleToCatchUp(board), "Blue cannot possibly catch up to Red");
    }

    // Possible to Catch Up
    @Test
    void testImpossibleToCatchUp2() {
        // Setup: Red has 20 points. Blue has 0. Only Blue King remains.
        board.addScore(PieceColor.RED, 20);
        board.addScore(PieceColor.BLUE, 6);

        King redKing = new King(PieceColor.RED, 0, 0);
        King blueKing = new King(PieceColor.BLUE, 5, 5);
        King yellowKing = new King(PieceColor.YELLOW, 2, 3);
        King greenKing = new King(PieceColor.GREEN, 5, 4);

        Boat redBoat = new Boat(PieceColor.RED, 1, 1);
        Boat yellowBoat = new Boat(PieceColor.YELLOW, 6, 6);

        // Place 4 kings and 2 boats on board
        board.placePiece(redKing, 0, 0);
        board.placePiece(blueKing, 5, 5);
        board.placePiece(yellowKing, 2, 3); // can still be captured for 3 points if dead
        board.placePiece(greenKing, 5, 4);  // can still be captured for 3 points if dead
        board.placePiece(redBoat, 1, 1);    // worth 5 points
        board.placePiece(yellowBoat, 6, 6); // worth 0 points if dead

        // Kill Yellow & Green so only 2 alive
        board.killPlayer(PieceColor.YELLOW);
        board.killPlayer(PieceColor.GREEN);

        /*
         * If there is a dead king on the board, it's still worth 3 point.
         * Therefore, blue could collect: 3 + 3 + 3 + 5 points (1 alive and 2 dead kings, 1 boat)
         * So, blue's maximum potancial points: 6 + 14 >= 20 --> blue can catch up to red
         */

        assertFalse(rules.isImpossibleToCatchUp(board), "Blue should be able to catch up to Red");
    }
}