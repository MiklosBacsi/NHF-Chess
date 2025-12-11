package model;

import model.pieces.*;
import java.util.Stack;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;

/**
 * This class represents the chess board and stores its properties.
 * @author Miklós Bácsi
 */
public class Board {
    private final Piece[][] squares = new Piece[8][8];

    // We need history to undo moves and check for En Passant availability
    private final Stack<Move> moveHistory = new Stack<>();

    // For Crazyhouse variant
    private final Map<PieceColor, Map<PieceType, Integer>> reserves = new HashMap<>();
    private boolean crazyhouseMode = false;

    // Track whose turn it is
    private PieceColor currentPlayer;

    // Important for making moves and turns correctly in Duck Chess
    private boolean waitingForDuck = false;

    // For Chaturaji
    private final Map<PieceColor, Integer> scores = new HashMap<>();
    private final Set<PieceColor> deadPlayers = new HashSet<>();

    /**
     * Constructor that sets up the board.
     */
    public Board() {
        // Initialize reserves structure
        reserves.put(PieceColor.WHITE, new HashMap<>());
        reserves.put(PieceColor.BLACK, new HashMap<>());

        resetBoard();
    }

    /**
     * Resets the board.
     */
    public void resetBoard() {
        // Reset Crazyhouse mode
        this.crazyhouseMode = false;

        // Reset player
        currentPlayer = PieceColor.WHITE;

        // Clear History
        moveHistory.clear();

        // Reset phase
        waitingForDuck = false;

        // Clear squares
        for(int r=0; r<8; r++)
            for(int c=0; c<8; c++)
                squares[r][c] = null;

        // Clear Reserves
        reserves.get(PieceColor.WHITE).clear();
        reserves.get(PieceColor.BLACK).clear();

        // --- For Chaturaji ---
        scores.clear();
        deadPlayers.clear();

        // Initialize scores
        scores.put(PieceColor.RED, 0);
        scores.put(PieceColor.BLUE, 0);
        scores.put(PieceColor.YELLOW, 0);
        scores.put(PieceColor.GREEN, 0);
    }

    /**
     * Adds standard pieces to the board.
     */
    public void addStandardPieces() {
        // Pawns
        for (int i = 0; i < 8; i++) {
            placePiece(new Pawn(PieceColor.BLACK, 1, i), 1, i);
            placePiece(new Pawn(PieceColor.WHITE, 6, i), 6, i);
        }

        // Heavy Pieces
        placeHeavyPieces(PieceColor.BLACK, 0);
        placeHeavyPieces(PieceColor.WHITE, 7);
    }

    /**
     * Places heavy pieces (every piece except for pawns) on the board
     * @param color color the pieces to place on the board
     * @param row row index where the pieces will be placed
     */
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

    /**
     * @param row row index of the piece to get
     * @param col column index of the piece to get
     * @return the piece at the given coordinate (null if out of bounds or piece found at the given square)
     */
    public Piece getPiece(int row, int col) {
        if (row < 0 || row >= 8 || col < 0 || col >= 8) return null;
        return squares[row][col];
    }

    /**
     * Places a piece to the given coordinate
     * @param piece piece to place
     * @param row row index of the board
     * @param col column index of the board
     */
    public void placePiece(Piece piece, int row, int col) {
        squares[row][col] = piece;
    }

    /**
     * @return last move (null if no move has happened)
     */
    public Move getLastMove() {
        if (moveHistory.isEmpty()) return null;
        return moveHistory.peek();
    }

    /**
     * @param count how many last moves we want to get
     * @return the last N moves from history, ordered from newest to oldest.
     */
    public java.util.List<Move> getLastMoves(int count) {
        java.util.List<Move> recentMoves = new java.util.ArrayList<>();
        int historySize = moveHistory.size();

        // We cannot get more moves than there are
        int limit = Math.min(count, historySize);

        // Iterate backwards from the top of the stack
        for (int i = 0; i < limit; i++) {
            // Stack index: size - 1 is the top
            recentMoves.add(moveHistory.get(historySize - 1 - i));
        }

        return recentMoves;
    }

    /**
     * @return whether the player has moved a piece, but hasn't moved the duck yet in this turn (in Duck Chess)
     */
    public boolean isWaitingForDuck() {
        return waitingForDuck;
    }

    /**
     * Sets phase whether player hasn't moved the duck yet.
     * @param waiting true, if player hasn't moved the duck yet (false otherwise)
     */
    public void setWaitingForDuck(boolean waiting) {
        this.waitingForDuck = waiting;
    }

    /**
     * Sets whether the variant is Crazyhouse.
     * @param active true if variant is Crazyhouse, false otherwise
     */
    public void setCrazyhouseMode(boolean active) {
        this.crazyhouseMode = active;
    }

    /**
     * Crazyhouse: after capturing a piece, we add it to the reserve of the capturer.
     * @param owner the player who captured the piece
     * @param type type of the piece that was captured
     */
    public void addToReserve(PieceColor owner, PieceType type) {
        Map<PieceType, Integer> inventory = reserves.get(owner);
        inventory.put(type, inventory.getOrDefault(type, 0) + 1);
    }

    /**
     * Crazyhouse: after capturing a piece, we can place in on the board and for that we need to remove it from the reserve.
     * @param owner the player who captured a piece and now places it on the board
     * @param type type of the pieces captured and placed on the board
     */
    public void removeFromReserve(PieceColor owner, PieceType type) {
        Map<PieceType, Integer> inventory = reserves.get(owner);
        if (inventory.containsKey(type)) {
            int count = inventory.get(type);
            if (count > 1) {
                inventory.put(type, count - 1);
            } else {
                inventory.remove(type);
            }
        }
    }

    /**
     * Crazyhouse: we can capture pieces and then place them on the board.
     * @param owner player who captured pieces, and we want to know how many pieces he captured of certain type.
     * @param type type of the piece we want to get the count of
     * @return count of a certain type of piece a given player captured
     */
    public int getReserveCount(PieceColor owner, PieceType type) {
        return reserves.get(owner).getOrDefault(type, 0);
    }

    /**
     * Crazyhouse: we can capture pieces and then place them on the board.
     * @param owner the player who captured pieces, and we want to get the content of his reserve
     * @return the reserve (of pieces) of the given player
     */
    public Map<PieceType, Integer> getReserve(PieceColor owner) {
        return reserves.get(owner);
    }

    /**
     * Chaturaji: Adds points to player's points.
     * @param player player who gets points
     * @param points how many points the player gets
     */
    public void addScore(PieceColor player, int points) {
        if (!scores.containsKey(player)) return;
        scores.put(player, scores.get(player) + points);
    }

    /**
     * Chaturaji: get player's points.
     * @param player whose points we want to know
     * @return points of the player
     */
    public int getScore(PieceColor player) {
        return scores.getOrDefault(player, 0);
    }

    /**
     * Helper to get highest score.
     * @return highest score
     */
    public int getHighestScore() {
        return scores.values()
                .stream()
                .mapToInt(Integer::intValue)
                .max()
                .orElse(0);
    }

    /**
     * Kills player (e.g. his king has been captured).
     * @param color color of the player we want to "kill"
     */
    public void killPlayer(PieceColor color) {
        deadPlayers.add(color);
        // Convert all pieces to GREY
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = squares[r][c];
                if (p != null && p.getColor() == color) {
                    // Create a grey copy (simplest way is to modify the piece or replace it)
                    // Since Piece is mutable in our design (row/col), but color is final...
                    // We must replace it. Ideally we clone logic, but for now:
                    squares[r][c] = convertToGrey(p);
                }
            }
        }
    }

    /**
     * Chaturaji: after the kings is captured, its piece die (convert to grey).
     * @param piece piece that dies
     * @return piece converted into dead piece
     */
    private Piece convertToGrey(Piece piece) {
        return switch (piece.getType()) {
            case PAWN -> new Pawn(PieceColor.GREY, piece.getRow(), piece.getCol());
            case KING -> new King(PieceColor.GREY, piece.getRow(), piece.getCol());
            case QUEEN -> new Queen(PieceColor.GREY, piece.getRow(), piece.getCol());
            case ROOK -> new Rook(PieceColor.GREY, piece.getRow(), piece.getCol());
            case BOAT -> new Boat(PieceColor.GREY, piece.getRow(), piece.getCol());
            case KNIGHT -> new Knight(PieceColor.GREY, piece.getRow(), piece.getCol());
            case BISHOP -> new Bishop(PieceColor.GREY, piece.getRow(), piece.getCol());
            default -> null;
        };
    }

    /**
     * @param color color of the player
     * @return if player is dead
     */
    public boolean isPlayerDead(PieceColor color) {
        return deadPlayers.contains(color);
    }

    /**
     * Returns the players who are still alive.
     * @return list of the players alive (their colors)
     */
    public List<PieceColor> getAlivePlayers() {
        List<PieceColor> alive = new ArrayList<>();
        PieceColor[] all = {PieceColor.RED, PieceColor.BLUE, PieceColor.YELLOW, PieceColor.GREEN};

        for (PieceColor p : all) {
            // Check if player is part of the game (has a score entry) and is not dead
            if (scores.containsKey(p) && !deadPlayers.contains(p)) {
                alive.add(p);
            }
        }
        return alive;
    }

    /**
     * @return how many players are alive
     */
    public int getAlivePlayerCount() {
        int total = (scores.containsKey(PieceColor.RED)) ? 4 : 2; // Detect game mode size roughly
        return total - deadPlayers.size();
    }

    /**
     * @return piece color of the current player
     */
    public PieceColor getCurrentPlayer() {
        return currentPlayer;
    }

    /**
     * Sets current player to given one.
     * @param currentPlayer player that will become the current player
     */
    public void  setCurrentPlayer(PieceColor currentPlayer) {
        this.currentPlayer = currentPlayer;
    }

    /**
     * Switches turn (after each move) to the next player alive.
     */
    public void switchTurn() {
        PieceColor next = currentPlayer.next();

        // Loop until we find an alive player (or loop forever if everyone dead - handled by game over)
        int safety = 0;
        while (deadPlayers.contains(next) && safety < 5) {
            next = next.next();
            ++safety;
        }
        currentPlayer = next;
    }

    /**
     * --- EXECUTE MOVE (with Special Logic) ---
     * @param move move to execute
     */
    public void executeMove(Move move) {

        // Handle TIMEOUT
        if (move.type() == MoveType.TIMEOUT) {
            // The piece in the move object tells us WHO ran out of time
            killPlayer(move.piece().getColor());
            moveHistory.push(move);
            return;
        }
        
        // Handle DROP (Crazyhouse)
        if (move.type() == MoveType.DROP) {
            squares[move.endRow()][move.endCol()] = move.piece();

            // We MUST update the piece's internal coordinates otherwise it still thinks it is at (-1, -1)
            move.piece().move(move.endRow(), move.endCol());

            removeFromReserve(move.piece().getColor(), move.piece().getType());
            moveHistory.push(move);
            return;
        }

        // Handle RESIGN
        if (move.type() == MoveType.RESIGN) {
            killPlayer(move.piece().getColor());
            moveHistory.push(move);
            return;
        }

        // Handle DRAW
        if (move.type() == MoveType.DRAW) {
            // Kill everyone (Visual effect for "Game Over / Draw")
            killPlayer(PieceColor.WHITE);
            killPlayer(PieceColor.BLACK);
            moveHistory.push(move);
            return;
        }

        // Remove piece from start
        squares[move.startRow()][move.startCol()] = null;

        // Handle Captures & Reserves
        Piece captured = move.capturedPiece();
        if (captured != null) {
            // Remove from board
            if (move.type() == MoveType.EN_PASSANT) {
                int direction = (move.piece().getColor() == PieceColor.WHITE) ? 1 : -1;
                squares[move.endRow() + direction][move.endCol()] = null;
            }

            // --- CRAZYHOUSE LOGIC ---
            if (crazyhouseMode) {
                PieceType type = captured.getType();

                // Add to the capturer's reserve
                addToReserve(move.piece().getColor(), type);
            }
        }

        // Handle Captures (En Passant is special)
        if (move.type() == MoveType.EN_PASSANT) {
            int direction = (move.piece().getColor() == PieceColor.WHITE) ? 1 : -1;
            squares[move.endRow() + direction][move.endCol()] = null;
        } else {
            // Normal capture is implicitly handled by overwriting the end square
        }

        // Place piece at end
        squares[move.endRow()][move.endCol()] = move.piece();
        move.piece().move(move.endRow(), move.endCol());

        // Handle Castling (Moving the Rook)
        if (move.type() == MoveType.CASTLING) {
            boolean kingside = move.endCol() > move.startCol();
            int rookStartCol = kingside ? 7 : 0;
            int rookEndCol = kingside ? 5 : 3;
            int row = move.startRow();

            Piece rook = squares[row][rookStartCol];
            if (rook != null) {
                squares[row][rookStartCol] = null;
                squares[row][rookEndCol] = rook;
                rook.move(row, rookEndCol);
            }
        }

        // Handle Promotion
        if (move.type() == MoveType.PROMOTION) {
            Piece promotedPiece;

            PieceColor c = move.piece().getColor();
            boolean isChaturaji = (c == PieceColor.RED || c == PieceColor.BLUE || c == PieceColor.YELLOW || c == PieceColor.GREEN);

            if (isChaturaji) {
                // Always promote to BOAT
                promotedPiece = new Boat(move.piece().getColor(), move.endRow(), move.endCol());
            } else {
                // --- STANDARD PROMOTION WITH DIALOG ---
                // Check what the user chose (Default to Queen if null, for safety)
                PieceType type = (move.promotionType() != null) ? move.promotionType() : PieceType.QUEEN;

                // Create promoted piece
                switch (type) {
                    case BOAT -> promotedPiece = new Boat(move.piece().getColor(), move.endRow(), move.endCol());
                    case ROOK -> promotedPiece = new Rook(move.piece().getColor(), move.endRow(), move.endCol());
                    case BISHOP -> promotedPiece = new Bishop(move.piece().getColor(), move.endRow(), move.endCol());
                    case KNIGHT -> promotedPiece = new Knight(move.piece().getColor(), move.endRow(), move.endCol());
                    default -> promotedPiece = new Queen(move.piece().getColor(), move.endRow(), move.endCol());
                }
            }

            squares[move.endRow()][move.endCol()] = promotedPiece;
        }

        moveHistory.push(move);
    }

    /**
     * --- UNDO MOVE (Crucial for validating Checks) ---
     */
    public void undoMove() {
        if (moveHistory.isEmpty()) return;
        Move move = moveHistory.pop();

        // --- HANDLE DROP UNDO ---
        if (move.type() == MoveType.DROP) {
            squares[move.endRow()][move.endCol()] = null;
            // Put back in reserve
            addToReserve(move.piece().getColor(), move.piece().getType());
            return;
        }

        // --- NORMAL UNDO ---
        // Move the acting piece back to the start
        squares[move.startRow()][move.startCol()] = move.piece();
        move.piece().move(move.startRow(), move.startCol());

        // Handle the Destination Square (Where the piece was just now)
        Piece captured = move.capturedPiece();
        if (captured != null) {
            // Capture
            if (move.type() == MoveType.EN_PASSANT) {
                // In En Passant, the destination square is actually empty
                squares[move.endRow()][move.endCol()] = null;

                // The captured pawn is "behind" the destination
                int direction = (move.piece().getColor() == PieceColor.WHITE) ? 1 : -1;
                squares[move.endRow() + direction][move.endCol()] = captured;
            } else {
                // Normal Capture: Put the dead piece back on the target square
                squares[move.endRow()][move.endCol()] = captured;
            }

            // CRAZYHOUSE UNDO: Remove from reserve
            if (crazyhouseMode) {
                removeFromReserve(move.piece().getColor(), captured.getType());
            }
        } else {
            // No Capture (Normal move)
            // The destination square must be cleared because the piece went back to start
            squares[move.endRow()][move.endCol()] = null;
        }

        // Revert Castling (If necessary)
        if (move.type() == MoveType.CASTLING) {
            boolean kingside = move.endCol() > move.startCol();
            int rookStartCol = kingside ? 7 : 0;
            int rookEndCol = kingside ? 5 : 3;
            int row = move.startRow();

            Piece rook = squares[row][rookEndCol];

            // Move rook back from end to start
            if (rook != null) {
                squares[row][rookEndCol] = null;
                squares[row][rookStartCol] = rook;

                rook.move(row, rookStartCol);

                // Since we only allow castling if the rook hasn't moved, undoing a castle means the rook hasn't moved
                rook.setHasMoved(false);
            }
        }

        // Restore 'hasMoved' state
        if (move.isFirstMove()) {
            move.piece().setHasMoved(false);
        }
    }

    /**
     * Helper to find King for check validation
     * @param color color of the king to find
     * @return the king of the given color (null if the king is not found on the board)
     */
    public Piece findKing(PieceColor color) {
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = squares[r][c];
                if (p != null && p.getType() == PieceType.KING && p.getColor() == color) {
                    return p;
                }
            }
        }
        return null;
    }

    /**
     * For the 50-Move Rule: Counts half-moves since the last Pawn move or Capture.
     * 50 moves = 100 half-moves.
     */
    public int getHalfMoveClock() {
        int count = 0;
        // Iterate backwards through history
        for (int i = moveHistory.size() - 1; i >= 0; i--) {
            Move move = moveHistory.get(i);

            // Reset counter if Pawn moved or Piece captured
            if (move.piece().getType() == PieceType.PAWN || move.capturedPiece() != null) {
                break;
            }

            // Reset if it was a Drop move (Crazyhouse specific rule varies,
            // but usually drops reset the clock because they change material on board)
            if (move.type() == MoveType.DROP) {
                break;
            }

            ++count;
        }
        return count;
    }

    /**
     * Used by GameSaver to get the list of moves to save.
     * @return moves that have been made in the match
     */
    public Stack<Move> getMoveHistory() {
        return moveHistory;
    }
}
