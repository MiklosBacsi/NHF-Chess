package view.game;

import model.*;
import model.pieces.*;
import model.rules.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.List;

/**
 * This class is responsible for the visual representation of the chess board and its pieces.
 * Furthermore, it handles user input (e.g. moving pieces).
 * @author Miklós Bácsi
 */
public class BoardPanel extends JPanel {

    // --- State ---
    private final Board board;
    private final Map<String, BufferedImage> pieceImages = new HashMap<>();
    private boolean isGameOver = false;

    // --- Drag & Drop State ---
    private Piece draggedPiece = null;
    private int dragX, dragY; // Current pixel coordinates of the mouse
    private int dragOffsetX, dragOffsetY; // To keep the mouse relative to the piece corner
    private Point hoverSquare = null; // Stores x=col, y=row of the square under mouse
    private List<Move> currentLegalMoves = null; // All the squares a piece can legally move to
    private Piece selectedPiece = null;
    private List<Move> selectedLegalMoves = null;

    // --- Visuals ---
    private BoardTheme currentTheme = BoardTheme.BROWN;
    private  int boardPadding = 20;
    private final Color moveHighlightColor = new Color(255, 255, 0, 96); // Source (Yellow)
    private final Color targetHighlightColor = new Color(255, 255, 255, 100); // Target (White overlay)
    private final Color moveHintColor = new Color(0, 0, 0, 50); // Dark dot for valid squares
    private final Color checkHighlightColor = new Color(255, 0, 0, 128);
    private PieceColor viewPerspective = PieceColor.WHITE; // Determines which side is at the bottom of the screen
    private final Set<Point> visibleSquares = new HashSet<>();
    private boolean isBlindMode = false; // "Curtain" for passing turn
    private static final int RESERVE_SLOT_SIZE = 60;
    private static final int RESERVE_GAP = 10;
    // Order of pieces in reserve display
    private final PieceType[] RESERVE_ORDER = {
            PieceType.PAWN, PieceType.KNIGHT, PieceType.BISHOP, PieceType.ROOK, PieceType.QUEEN
    };

    // Cached calculation values to share between paint() and mouse listeners
    private int startX, startY, squareSize;

    private GameVariant gameRules; // The Strategy


    /**
     * Constructor of BoardPanel which creates a new BoardPanel
     */
    public BoardPanel() {
        this.board = new Board();
        setOpaque(false);
        loadResources();

        // Default startup
        this.gameRules = new ClassicalVariant();

        // Initialize Mouse Listeners
        MouseAdapter mouseHandler = new DragController();
        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);
    }

    /**
     * It creates a new game with the chosen variant.
     * @param mode determines the type of the game variant
     */
    public void setupGame(String mode) {
        // Reset Model
        board.resetBoard();

        this.viewPerspective = PieceColor.WHITE;

        // Reset padding
        this.boardPadding = 20;

        // Select Rules & Special Setup
        switch (mode) {
            case "Classical":
                this.gameRules = new ClassicalVariant();
                board.addStandardPieces();
                break;

            case "Fog of War":
                this.gameRules = new FogOfWarVariant();
                board.addStandardPieces();
                break;

            case "Duck Chess":
                this.gameRules = new DuckChessVariant();
                board.addStandardPieces();
                board.placePiece(new Duck(3, 3), 3, 3); // d5
                break;

            case "Crazyhouse":
                this.gameRules = new CrazyhouseVariant();
                board.addStandardPieces();
                board.setCrazyhouseMode(true); // ENABLE RESERVES
                this.boardPadding = 90;
                break;

            case "Chaturaji":
                this.gameRules = new ChaturajiVariant();
                setupChaturajiBoard();
                this.viewPerspective = PieceColor.RED;
                board.setCurrentPlayer(PieceColor.RED);
                this.boardPadding = 120;
                break;

            default:
                this.gameRules = new ClassicalVariant(); // Fallback
                board.addStandardPieces();
                break;
        }

        // Calculate initial visibility for White
        if (gameRules instanceof FogOfWarVariant) {
            calculateVisibility();
        }

        isGameOver = false;

        // Clear UI state
        isBlindMode = false;
        draggedPiece = null;
        hoverSquare = null;
        currentLegalMoves = null;
        selectedPiece = null;
        selectedLegalMoves = null;

        // Redraw
        repaint();
    }

    /**
     * Sets up board and pieces for Chaturaji.
     */
    private void setupChaturajiBoard() {
        board.resetBoard(); // Clears everything

        // --- RED (Bottom-Left) ---
        // Pieces row 7, Pawns row 6 (Cols 0-3)
        board.placePiece(new Boat(PieceColor.RED, 7, 0), 7, 0);   // a1
        board.placePiece(new Knight(PieceColor.RED, 7, 1), 7, 1); // b1
        board.placePiece(new Bishop(PieceColor.RED, 7, 2), 7, 2); // c1
        board.placePiece(new King(PieceColor.RED, 7, 3), 7, 3);   // d1
        board.placePiece(new Pawn(PieceColor.RED, 6, 0), 6, 0);   // a2
        board.placePiece(new Pawn(PieceColor.RED, 6, 1), 6, 1);   // b2
        board.placePiece(new Pawn(PieceColor.RED, 6, 2), 6, 2);   // c2
        board.placePiece(new Pawn(PieceColor.RED, 6, 3), 6, 3);   // d2

        // --- GREEN (Bottom-Right) ---
        // Pieces col 7, Pawns col 6 (Rows 4-7)
        // Order from bottom to top: Boat, Knight, Bishop, King
        board.placePiece(new Boat(PieceColor.GREEN, 7, 7), 7, 7);   // h1
        board.placePiece(new Knight(PieceColor.GREEN, 6, 7), 6, 7); // h2
        board.placePiece(new Bishop(PieceColor.GREEN, 5, 7), 5, 7); // h3
        board.placePiece(new King(PieceColor.GREEN, 4, 7), 4, 7);   // h4
        board.placePiece(new Pawn(PieceColor.GREEN, 7, 6), 7, 6);   // g1
        board.placePiece(new Pawn(PieceColor.GREEN, 6, 6), 6, 6);   // g2
        board.placePiece(new Pawn(PieceColor.GREEN, 5, 6), 5, 6);   // g3
        board.placePiece(new Pawn(PieceColor.GREEN, 4, 6), 4, 6);   // g4

        // --- YELLOW (Top-Right) ---
        // Pieces row 0, Pawns row 1 (Cols 4-7)
        // Order Right-to-Left: Boat, Knight, Bishop, King
        board.placePiece(new Boat(PieceColor.YELLOW, 0, 7), 0, 7);   // h8
        board.placePiece(new Knight(PieceColor.YELLOW, 0, 6), 0, 6); // g8
        board.placePiece(new Bishop(PieceColor.YELLOW, 0, 5), 0, 5); // f8
        board.placePiece(new King(PieceColor.YELLOW, 0, 4), 0, 4);   // e8
        board.placePiece(new Pawn(PieceColor.YELLOW, 1, 7), 1, 7);   // h7
        board.placePiece(new Pawn(PieceColor.YELLOW, 1, 6), 1, 6);   // g7
        board.placePiece(new Pawn(PieceColor.YELLOW, 1, 5), 1, 5);   // f7
        board.placePiece(new Pawn(PieceColor.YELLOW, 1, 4), 1, 4);   // e7

        // --- BLUE (Top-Left) ---
        // Pieces col 0, Pawns col 1 (Rows 0-3)
        // Order Top-to-Bottom: Boat, Knight, Bishop, King
        board.placePiece(new Boat(PieceColor.BLUE, 0, 0), 0, 0);   // a8
        board.placePiece(new Knight(PieceColor.BLUE, 1, 0), 1, 0); // a7
        board.placePiece(new Bishop(PieceColor.BLUE, 2, 0), 2, 0); // a6
        board.placePiece(new King(PieceColor.BLUE, 3, 0), 3, 0);   // a5
        board.placePiece(new Pawn(PieceColor.BLUE, 0, 1), 0, 1);   // b8
        board.placePiece(new Pawn(PieceColor.BLUE, 1, 1), 1, 1);   // b7
        board.placePiece(new Pawn(PieceColor.BLUE, 2, 1), 2, 1);   // b6
        board.placePiece(new Pawn(PieceColor.BLUE, 3, 1), 3, 1);   // b5
    }

    /**
     * Loads the textures in memory
     */
    private void loadResources() {
        String[] colors = {"white", "black"};
        String[] types = {"pawn", "rook", "knight", "bishop", "queen", "king"};

        // Standard black & white pieces
        for (String c : colors) {
            for (String t : types) {
                // The key will be "white-pawn" (matches Piece.getFilename())
                String key = c + "-" + t;

                // The path matches your structure: /piece/white/pawn.png
                String path = "/piece/" + c + "/" + t + ".png";

                try {
                    URL url = getClass().getResource(path);
                    if (url != null) {
                        pieceImages.put(key, ImageIO.read(url));
                    } else {
                        System.err.println("Image not found: " + path);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        // Duck
        try {
            URL url = getClass().getResource("/piece/special/duck.png");
            if (url != null) pieceImages.put("special-duck", ImageIO.read(url));
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Chaturaji
        String[] chaturajiColors = {"red", "blue", "yellow", "green", "grey"};
        String[] chaturajiTypes = {"pawn", "boat", "knight", "bishop", "king"};

        for (String c : chaturajiColors) {
            for (String t : chaturajiTypes) {
                // The key will be "red-boat" (matches Piece.getFilename())
                String key = c + "-" + t;

                // The path matches your structure: /piece/red/boat.png
                String path = "/piece/" + c + "/" + t + ".png";

                try {
                    URL url = getClass().getResource(path);
                    if (url != null) {
                        pieceImages.put(key, ImageIO.read(url));
                    } else {
                        System.err.println("Image not found: " + path);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Sets the theme to the chosen one
     * @param theme chosen theme
     */
    public void setTheme(BoardTheme theme) {
        this.currentTheme = theme;
        repaint();
    }

    /**
     * This method is responsible for drawing the board with its pieces
     * @param g Graphics (drawing onto this)
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        // --- HIGH QUALITY SETTINGS ---
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        // --- RESPONSIVE LAYOUT ---
        // Calculate available space
        int width = getWidth();
        int height = getHeight();

        int minimumDimension = Math.min(width, height) - (boardPadding * 2);
        if (minimumDimension < 0) minimumDimension = 0;

        // Calculate Square Size dynamically, and take the smaller dimension so the board always fits inside the window
        this.squareSize = minimumDimension / 8;

        // Calculate centering (to keep board in middle)
        int boardPixelSize = squareSize * 8;
        this.startX = (width - boardPixelSize) / 2;
        this.startY = (height - boardPixelSize) / 2;

        // Determine how many last moves to highlight
        int movesToShow = 1; // Default (Classical)

        if (gameRules instanceof DuckChessVariant) {
            movesToShow = 2; // Piece Move + Duck Move
        } else if (gameRules instanceof  ChaturajiVariant) {
            movesToShow = Math.max(board.getAlivePlayerCount() - 1, 1);
        }

        List<Move> recentMoves = board.getLastMoves(movesToShow);

        // Checks
        boolean whiteInCheck = gameRules.isCheck(board, PieceColor.WHITE);
        boolean blackInCheck = gameRules.isCheck(board, PieceColor.BLACK);

        // We need to know WHERE the kings are to highlight them
        Piece whiteKing = board.findKing(PieceColor.WHITE);
        Piece blackKing = board.findKing(PieceColor.BLACK);

        Move lastMove = board.getLastMove();

        boolean isFoggyGame = (gameRules instanceof FogOfWarVariant);

        // Helper: What are we focusing on? Dragged takes priority, then Selected.
        Piece activePiece = (draggedPiece != null) ? draggedPiece : selectedPiece;
        List<Move> activeMoves = (draggedPiece != null) ? currentLegalMoves : selectedLegalMoves;

        // Draw the Board & Pieces
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                // TRANSFORM TO VISUAL COORDS
                int visualRow = getVisualRow(row, col);
                int visualCol = getVisualCol(row, col);

                int x = startX + (visualCol * squareSize);
                int y = startY + (visualRow * squareSize);

                // VISIBILITY CHECK
                // Point uses x=col, y=row
                boolean isVisible = visibleSquares.contains(new Point(col, row));

                // If Blind Mode is ON, everything is hidden
                if (isBlindMode) isVisible = false;

                // If not a Fog game, everything is visible
                if (!isFoggyGame) isVisible = true;


                // Determine color of square
                if ((row + col) % 2 == 0) {
                    g2d.setColor(currentTheme.getLight());
                } else {
                    g2d.setColor(currentTheme.getDark());
                }

                // Draw the square (Background)
                g2d.fillRect(x, y, squareSize, squareSize);

                // IF NOT VISIBLE -> DRAW FOG AND SKIP CONTENT
                if (!isVisible) {
                    // Use the GREY theme colors to represent fog
                    if ((row + col) % 2 == 0) {
                        g2d.setColor(BoardTheme.GREY.getLight());
                    } else {
                        g2d.setColor(BoardTheme.GREY.getDark());
                    }
                    g2d.fillRect(x, y, squareSize, squareSize);
                    continue; // STOP HERE for this square (Don't draw pieces/highlights)
                }


                // --- Highlights/Hints logic uses MODEL coords to check, but draws at VISUAL coords (x,y) ---

                // Draw Last Move Highlights (Recent Moves) (only draw if NOT dragging, and there is a history)
                if (draggedPiece == null) {
                    // Loop through the list of recent moves
                    for (Move move : recentMoves) {
                        boolean isStart = (move.startRow() == row && move.startCol() == col);
                        boolean isEnd = (move.endRow() == row && move.endCol() == col);

                        if (isStart || isEnd) {
                            g2d.setColor(moveHighlightColor);
                            g2d.fillRect(x, y, squareSize, squareSize);
                            // We don't break here, because we can draw multiple times on the same square
                        }
                    }
                }

                // Draw Source Highlight (if it's the dragged piece or selected piece)
                if (activePiece != null && activePiece.getRow() == row && activePiece.getCol() == col) {
                    g2d.setColor(moveHighlightColor);
                    g2d.fillRect(x, y, squareSize, squareSize);
                }

                // Draw Target Highlight (White)
                if (draggedPiece != null && hoverSquare != null && hoverSquare.y == row && hoverSquare.x == col) {
                    g2d.setColor(targetHighlightColor);
                    g2d.fillRect(x, y, squareSize, squareSize);
                }

                // --- Check Highlight (Red) ---
                // If White is in Check AND this is the White King's square -> Paint Red
                if (whiteInCheck && whiteKing != null && whiteKing.getRow() == row && whiteKing.getCol() == col) {
                    g2d.setColor(checkHighlightColor);
                    g2d.fillRect(x, y, squareSize, squareSize);
                }
                // If Black is in Check AND this is the Black King's square -> Paint Red
                if (blackInCheck && blackKing != null && blackKing.getRow() == row && blackKing.getCol() == col) {
                    g2d.setColor(checkHighlightColor);
                    g2d.fillRect(x, y, squareSize, squareSize);
                }

                // --- Draw Piece (if it's not being dragged) ---
                Piece piece = board.getPiece(row, col);
                if (piece != null && piece != draggedPiece) {
                    drawPiece(g2d, piece, x, y);
                }
            }
        }

        // Draw Move Hints (Dots and Rings)
        if (activePiece != null && activeMoves != null) {
            g2d.setColor(moveHintColor);

            // Save the original stroke (thin line) so we can restore it later
            Stroke originalStroke = g2d.getStroke();

            // Calculate dot size
            int dotSize = squareSize / 3;
            int offset = (squareSize - dotSize) / 2; // To center the dot

            // Calculate thickness
            float ringThickness = squareSize * 0.1f;

            // Calculate size
            int ringDiameter = (int) (squareSize - ringThickness);
            int ringOffset = (int) (ringThickness / 2);

            for (Move move : activeMoves) {
                // TRANSFORM DOT POSITION
                int visualRow = getVisualRow(move.endRow(), move.endCol());
                int visualCol = getVisualCol(move.endRow(), move.endCol());

                int gx = startX + (visualCol * squareSize);
                int gy = startY + (visualRow * squareSize);

                // Check: Is there a piece at the target square?
                Piece target = board.getPiece(move.endRow(), move.endCol());

                if (target == null) {
                    // --- QUIET MOVE: Draw DOT ---
                    g2d.fillOval(gx + offset, gy + offset, dotSize, dotSize);
                } else {
                    // --- CAPTURE MOVE: Draw RING ---
                    g2d.setStroke(new BasicStroke(ringThickness));
                    g2d.drawOval(gx + ringOffset, gy + ringOffset, ringDiameter, ringDiameter);

                    // Restore stroke for next iteration
                    g2d.setStroke(originalStroke);
                }
            }
        }

        // Draw a border around the whole board for a cleaner look
        g2d.setColor(Color.BLACK);
        g2d.setStroke(new BasicStroke(2));
        g2d.drawRect(startX, startY, boardPixelSize, boardPixelSize);

        // --- Draw Dragged Piece (On Top) ---
        if (draggedPiece != null) {
            // Draw at mouse position (adjusted by offset)
            drawPiece(g2d, draggedPiece, dragX - dragOffsetX, dragY - dragOffsetY);
        }

        // --- DRAW RESERVES (Only in Crazyhouse) ---
        if (gameRules instanceof CrazyhouseVariant) {
            drawReserves(g2d);
        }

        // --- DRAW SCORES (Chaturaji) ---
        drawScores(g2d);
    }

    /**
     * Recalculates visible squares.
     */
    private void calculateVisibility() {
        visibleSquares.clear();
        PieceColor myColor = board.getCurrentPlayer();

        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = board.getPiece(r, c);

                // I can see my own pieces
                if (p != null && p.getColor() == myColor) {
                    visibleSquares.add(new Point(c, r)); // x=c, y=r

                    // I can see where my pieces can move
                    List<Move> moves = p.getPseudoLegalMoves(board);
                    for (Move m : moves) {
                        visibleSquares.add(new Point(m.endCol(), m.endRow()));
                    }
                }
            }
        }
    }

    /**
     * Helper method for drawing pieces on the board
     * @param g2d Graphics2D (drawing onto this)
     * @param piece pieces to draw
     * @param x x coordinate
     * @param y y coordinate
     */
    private void drawPiece(Graphics2D g2d, Piece piece, int x, int y) {
        String key = piece.getFilename();
        BufferedImage img = pieceImages.get(key);
        if (img != null) {
            g2d.drawImage(img, x, y, squareSize, squareSize, this);
        }
    }

    /**
     * Helper to draw both reserves (top & bottom).
     * @param g2d Graphics2D
     */
    private void drawReserves(Graphics2D g2d) {
        // Determine who is Top (Opponent) and Bottom (Self)
        PieceColor bottomPlayer = viewPerspective;
        PieceColor topPlayer = bottomPlayer.next();

        // Draw Bottom Reserve (Self)
        drawSingleReserve(g2d, bottomPlayer, false);

        // Draw Top Reserve (Opponent)
        drawSingleReserve(g2d, topPlayer, true);
    }

    /**
     * Helper to draw a single reserve.
     * @param g2d Graphics2D
     * @param color color of the player
     * @param isTop true for opponent (top), false for self (bottom)
     */
    private void drawSingleReserve(Graphics2D g2d, PieceColor color, boolean isTop) {
        Map<PieceType, Integer> reserve = board.getReserve(color);

        for (int i = 0; i < RESERVE_ORDER.length; i++) {
            PieceType type = RESERVE_ORDER[i];
            int count = reserve.getOrDefault(type, 0);

            Rectangle bounds = getReserveSlotBounds(isTop, i);

            // Draw Slot Background (faint box)
            g2d.setColor(new Color(0, 0, 0, 50));
            g2d.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 10, 10);

            // Draw Piece Icon (if count > 0)
            if (count > 0) {
                String key = color.name().toLowerCase() + "-" + type.name().toLowerCase();
                BufferedImage img = pieceImages.get(key);
                if (img != null) {
                    // Draw centered in slot
                    int padding = 5;
                    g2d.drawImage(img, bounds.x + padding, bounds.y + padding,
                            bounds.width - 2*padding, bounds.height - 2*padding, this);
                }

                // Draw Count Badge
                g2d.setColor(Color.WHITE);
                g2d.fillOval(bounds.x + bounds.width - 20, bounds.y + bounds.height - 20, 20, 20);

                g2d.setColor(Color.BLACK);
                g2d.setFont(new Font("Arial", Font.BOLD, 12));
                // Center text roughly
                g2d.drawString(String.valueOf(count), bounds.x + bounds.width - 14, bounds.y + bounds.height - 5);
            }
        }
    }

    /**
     * Converts screen pixel coordinate into a chess board one while applying inverse transformation.
     * @param x board's visual x coordinate
     * @param y board's visual y coordinate
     * @return models's coordinate
     */
    private Point getBoardCoordinates(int x, int y) {
        if (squareSize == 0) return null;

        // Calculate which Visual Square was clicked
        int visualCol = (x - startX) / squareSize;
        int visualRow = (y - startY) / squareSize;

        // Validate bounds
        if (visualCol >= 0 && visualCol < 8 && visualRow >= 0 && visualRow < 8) {
            // Convert to Model Coordinates
            return getModelCoordinatesFromVisual(visualRow, visualCol);
        }
        // Outside the board
        return null;
    }

    /**
     * Converts MODEL (row,col) -> VISUAL (vrow,vcol).
     * @param row row index in model
     * @param col column index in model
     * @return visual row index
     */
    private int getVisualRow(int row, int col) {
        return switch (viewPerspective) {
            case RED, WHITE -> row;              // No rotation
            case YELLOW, BLACK -> 7 - row;       // 180 degrees
            case BLUE -> 7 - col;         // 90 degrees CW (Left becomes Bottom)
            case GREEN -> col;            // 270 degrees CW (Right becomes Bottom)
            default -> row;
        };
    }

    /**
     * Converts MODEL (row,col) -> VISUAL (vrow,vcol).
     * @param row row index in model
     * @param col column index in model
     * @return visual column index
     */
    private int getVisualCol(int row, int col) {
        return switch (viewPerspective) {
            case RED, WHITE -> col;       // No rotation
            case YELLOW, BLACK -> 7 - col;    // 180 degrees
            case BLUE -> row;             // 90 degrees CW
            case GREEN -> 7 - row;        // 270 degrees CW
            default -> col;
        };
    }

    /**
     * Converts VISUAL (vrow,vcol) -> MODEL (row,col). (Used for Mouse Clicking)
     * @param vrow visual row index
     * @param vcol visual column index
     * @return square in model
     */
    private Point getModelCoordinatesFromVisual(int vrow, int vcol) {
        int row = 0, col = 0;
        switch (viewPerspective) {
            case RED, WHITE:    row = vrow; col = vcol; break;
            case YELLOW, BLACK: row = 7 - vrow; col = 7 - vcol; break;
            case BLUE:   row = vcol; col = 7 - vrow; break; // Inverse of (7-c, r)
            case GREEN:  row = 7 - vcol; col = vrow; break; // Inverse of (c, 7-r)
            default:     row = vrow; col = vcol; break;
        }
        return new Point(col, row); // Point is (x=col, y=row)
    }

    /**
     * Draws scores of players.
     * @param g2d Graphics2D to paint on
     */
    private void drawScores(Graphics2D g2d) {
        // Only relevant for Chaturaji
        if (!(gameRules instanceof ChaturajiVariant)) return;

        // Iterate all 4 colors
        drawSingleScore(g2d, PieceColor.RED);
        drawSingleScore(g2d, PieceColor.BLUE);
        drawSingleScore(g2d, PieceColor.YELLOW);
        drawSingleScore(g2d, PieceColor.GREEN);
    }

    /**
     * Draw a certain player's points.
     * @param g2d Graphics2D to draw on
     * @param color color of the player
     */
    private void drawSingleScore(Graphics2D g2d, PieceColor color) {
        int score = board.getScore(color);
        String text = "Points: " + score;

        // Check if player is dead
        if (board.isPlayerDead(color)) {
            text += " (Dead)";
        }

        // Determine position based on current viewPerspective
        // We want the score to "stick" to the player's edge

        int x = 0, y = 0;
        int padding = 10;

        // Map specific colors to Visual Edges based on rotation.

        // Find relative position (0=Bottom, 1=Left, 2=Top, 3=Right)
        int relativePos = -1;
        PieceColor p = viewPerspective;
        for (int i = 0; i < 4; i++) {
            if (p == color) {
                relativePos = i;
                break;
            }
            p = p.next(); // Rotates clockwise (Red->Blue->Yellow->Green)
        }

        // Set coordinates based on Visual Edge
        g2d.setFont(new Font("Arial", Font.BOLD, 16));
        FontMetrics fm = g2d.getFontMetrics();
        int w = fm.stringWidth(text);
        int h = fm.getHeight();

        switch (relativePos) {
            case 0: // BOTTOM (Self)
                x = startX + (squareSize * 4) - (w / 2); // Center X
                y = startY + (squareSize * 8) + h + padding; // Below board
                break;
            case 1: // LEFT
                x = startX - w - padding; // Left of board
                y = startY + (squareSize * 4) + (h / 4); // Center Y
                break;
            case 2: // TOP
                x = startX + (squareSize * 4) - (w / 2); // Center X
                y = startY - padding; // Above board
                break;
            case 3: // RIGHT
                x = startX + (squareSize * 8) + padding; // Right of board
                y = startY + (squareSize * 4) + (h / 4); // Center Y
                break;
        }

        // Draw Background Bubble
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.fillRoundRect(x - 5, y - h + 5, w + 10, h + 5, 10, 10);

        // Draw Text (Color-coded)
        g2d.setColor(getColorForPlayer(color));
        g2d.drawString(text, x, y);
    }

    /**
     * @param color color of player
     * @return rgb color of player
     */
    private Color getColorForPlayer(PieceColor color) {
        return switch (color) {
            case RED -> new Color(191, 59, 67);
            case BLUE -> new Color(65, 133, 191);
            case YELLOW -> new Color(192, 149, 38);
            case GREEN -> new Color(78, 145, 97);
            default -> Color.WHITE;
        };
    }

    /**
     * Calculates the screen bounds for a specific reserve slot.
     * @param isTop true for opponent (top), false for self (bottom)
     * @param slotIndex 0...4 (Pawn...Queen)
     * @return screen bounds for given reserve slot
     */
    private Rectangle getReserveSlotBounds(boolean isTop, int slotIndex) {
        int totalWidth = (RESERVE_ORDER.length * RESERVE_SLOT_SIZE) + ((RESERVE_ORDER.length - 1) * RESERVE_GAP);
        int startX = (getWidth() - totalWidth) / 2;

        int y;
        if (isTop) {
            y = startY - RESERVE_SLOT_SIZE - 20;
        } else {
            y = startY + (squareSize * 8) + 20;
        }

        int x = startX + (slotIndex * (RESERVE_SLOT_SIZE + RESERVE_GAP));

        return new Rectangle(x, y, RESERVE_SLOT_SIZE, RESERVE_SLOT_SIZE);
    }

    /**
     * Helper to handle turn-switching, rules and game over checks.
     * @param move move that was made
     */
    private void finalizeTurn(Move move) {

        // Execute the Move on the Board
        board.executeMove(move);

        if (gameRules instanceof ChaturajiVariant chaturajiRules) {
            // Update Points & Handle Player Death
            chaturajiRules.handlePostMoveLogic(board, move);

            // Check Chaturaji End Conditions (3 Kings Dead)
            if (board.getAlivePlayerCount() <= 1) { // 3 dead = 1 alive
                System.out.println("---------------------------------------");
                System.out.println("GAME OVER! All enemies defeated.");
                printChaturajiStandings();
                System.out.println("---------------------------------------");
                isGameOver = true;
            } else {
                board.switchTurn();
                viewPerspective = board.getCurrentPlayer();
            }

            clearSelections();
            return;
        }

        // Check Win Condition (King Capture)
        if (move.capturedPiece() != null && move.capturedPiece().getType() == PieceType.KING) {
            System.out.println("---------------------------------------");
            System.out.println("GAME OVER! " + move.piece().getColor() + " captured the King!");
            System.out.println("---------------------------------------");
            isGameOver = true;
            clearSelections();
            return;
        }

        // --- SWITCHING TURNS ---
        if (gameRules instanceof DuckChessVariant) {
            // Duck Chess
            if (move.type() == MoveType.DUCK) {
                board.setWaitingForDuck(false);
                board.switchTurn();
                viewPerspective = board.getCurrentPlayer();
            } else {
                board.setWaitingForDuck(true);
            }
        } else {
            // Classical / Fog / Crazyhouse
            board.switchTurn();
            viewPerspective = board.getCurrentPlayer();
        }

        // Fog of War Logic
        if (gameRules instanceof FogOfWarVariant) {
            isBlindMode = true;
            repaint();
            SwingUtilities.invokeLater(() -> {
                Frame parent = (Frame) SwingUtilities.getWindowAncestor(BoardPanel.this);
                new PassTurnDialog(parent, board.getCurrentPlayer()).setVisible(true);
                isBlindMode = false;
                calculateVisibility();
                repaint();
            });
        }

        // Checkmate / Stalemate Checks
        if (!isGameOver && !board.isWaitingForDuck()) {
            PieceColor activePlayer = board.getCurrentPlayer();
            if (gameRules.isCheckmate(board, activePlayer)) {
                PieceColor winner = activePlayer.next();
                System.out.println("---------------------------------------");
                System.out.println("CHECKMATE! " + winner + " wins!");
                System.out.println("---------------------------------------");
                isGameOver = true;
            } else if (gameRules.isStalemate(board, activePlayer)) {
                System.out.println("STALEMATE!");
                isGameOver = true;
            }
        }

        clearSelections();
    }

    /**
     * Clears selected/dragged piece, and repaints.
     */
    private void clearSelections() {
        // Clear Selections
        selectedPiece = null;
        selectedLegalMoves = null;
        draggedPiece = null;
        currentLegalMoves = null;

        repaint();
    }

    /**
     * Helper to print scores and winners in Chaturaji.
     */
    private void printChaturajiStandings() {
        System.out.println("FINAL SCORES:");
        PieceColor[] players = {PieceColor.RED, PieceColor.BLUE, PieceColor.YELLOW, PieceColor.GREEN};

        int maxScore = -1;
        java.util.List<PieceColor> winners = new java.util.ArrayList<>();

        StringBuilder sb = new StringBuilder("GAME OVER!\n\nFinal Scores:\n");

        for (PieceColor player : players) {
            int score = board.getScore(player);
            System.out.println(player + ": " + score);

            sb.append(player).append(": ").append(score).append("\n");

            // Logic to track multiple winners
            if (score > maxScore) {
                maxScore = score;
                winners.clear(); // New high score found, discard previous winners
                winners.add(player);
            } else if (score == maxScore) {
                winners.add(player); // Tie found, add to list
            }
        }

        // Construct the winner string
        StringBuilder winnerMsg = new StringBuilder();
        if (winners.size() == 1) {
            winnerMsg.append("\nWINNER: ").append(winners.getFirst());
        } else {
            winnerMsg.append("\nWINNERS: ");
            for (int i = 0; i < winners.size(); i++) {
                winnerMsg.append(winners.get(i));
                if (i < winners.size() - 1) {
                    winnerMsg.append(", ");
                }
            }
        }

        System.out.println(winnerMsg);
        sb.append(winnerMsg).append("!");

        clearSelections();

        // Show Popup
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(this, sb.toString(), "Game Over", JOptionPane.INFORMATION_MESSAGE);
        });
    }

    /**
     * Helper: extract Promotion Dialog Logic
     * @param move promotion move that was made (moving the pawn to the opposite row)
     * @return transformed move that is now promotion move
     */
    private Move handlePromotion(Move move) {
        if (move.type() == MoveType.PROMOTION) {
            if (gameRules instanceof ChaturajiVariant) {
                return new Move(move, PieceType.BOAT);
            }

            Frame parent = (Frame) SwingUtilities.getWindowAncestor(this);
            PromotionDialog dialog = new PromotionDialog(parent, move.piece().getColor());
            dialog.setVisible(true);
            return new Move(move, dialog.getSelectedType());
        }
        return move;
    }

    /**
     * Handles when we click in a reserve.
     * @param mouseX x coordinate of the mouse
     * @param mouseY y coordinate of the mouse
     */
    private void checkReserveClick(int mouseX, int mouseY) {
        // We can only interact with OUR reserve (Bottom)
        PieceColor myColor = board.getCurrentPlayer();

        for (int i = 0; i < RESERVE_ORDER.length; i++) {
            Rectangle bounds = getReserveSlotBounds(false, i); // false = bottom

            if (bounds.contains(mouseX, mouseY)) {
                PieceType type = RESERVE_ORDER[i];
                int count = board.getReserveCount(myColor, type);

                if (count > 0) {
                    // HIT! We clicked a piece in reserve.

                    // Create a "Virtual" Piece for dragging (We use -1, -1 to indicate it's not on the board)
                    Piece virtualPiece;
                    switch (type) {
                        case ROOK -> virtualPiece = new Rook(myColor, -1, -1);
                        case KNIGHT -> virtualPiece = new Knight(myColor, -1, -1);
                        case BISHOP -> virtualPiece = new Bishop(myColor, -1, -1);
                        case QUEEN -> virtualPiece = new Queen(myColor, -1, -1);
                        default -> virtualPiece = new Pawn(myColor, -1, -1);
                    }

                    // Set Drag State
                    draggedPiece = virtualPiece;
                    currentLegalMoves = gameRules.getLegalMoves(board, virtualPiece);

                    // Set Visual Offsets (Center piece on mouse)
                    dragX = mouseX;
                    dragY = mouseY;
                    dragOffsetX = squareSize / 2;
                    dragOffsetY = squareSize / 2;

                    repaint();
                }
                return;
            }
        }
    }


    /**
     * --- Inner Class for Mouse Handling ---
     */
    private class DragController extends MouseAdapter {

        /**
         * Handles event when the mouse is pressed
         * @param e MouseEvent
         */
        @Override
        public void mousePressed(MouseEvent e) {

            // Disabled after end of game
            if (isGameOver) return;

            Point coords = getBoardCoordinates(e.getX(), e.getY());

            // Reserve Logic
            if (coords == null && gameRules instanceof CrazyhouseVariant) {
                // We clicked outside the board in Crazyhouse mode. Did we click the reserve?
                checkReserveClick(e.getX(), e.getY());
                return;
            }

            if (coords == null) {
                // Clicked outside board -> Deselect everything
                selectedPiece = null;
                selectedLegalMoves = null;
                repaint();
                return;
            }

            int clickedRow = coords.y;
            int clickedCol = coords.x;

            // --- CLICK TO MOVE LOGIC ---
            // If we have a selected piece, and we clicked on a valid move dot
            if (selectedPiece != null && selectedLegalMoves != null) {
                for (Move m : selectedLegalMoves) {
                    if (m.endRow() == clickedRow && m.endCol() == clickedCol) {

                        // Handle Promotion (if needed)
                        Move finalMove = handlePromotion(m);

                        // EXECUTE MOVE
                        finalizeTurn(finalMove);
                        return; // Stop here, we moved!
                    }
                }
            }

            // --- DRAG START LOGIC ---
            Piece clickedPiece = board.getPiece(clickedRow, clickedCol);

            if (clickedPiece != null) {
                // Turn Validation
                if (clickedPiece.getColor() != board.getCurrentPlayer() && clickedPiece.getColor() != PieceColor.SPECIAL) {
                    // Clicked enemy -> Deselect
                    selectedPiece = null;
                    selectedLegalMoves = null;
                    repaint();
                    return;
                }

                // Setup Drag
                draggedPiece = clickedPiece;
                hoverSquare = coords;
                currentLegalMoves = gameRules.getLegalMoves(board, draggedPiece);

                // Visual offsets
                dragX = e.getX();
                dragY = e.getY();
                int visualRow = getVisualRow(coords.y, coords.x);
                int visualCol = getVisualCol(coords.y, coords.x);
                int pieceX = startX + (visualCol * squareSize);
                int pieceY = startY + (visualRow * squareSize);
                dragOffsetX = e.getX() - pieceX;
                dragOffsetY = e.getY() - pieceY;

                repaint();
            } else {
                // Clicked empty square -> Deselect
                selectedPiece = null;
                selectedLegalMoves = null;
                repaint();
            }
        }

        /**
         * Handles event when the mouse is dragged
         * @param e MouseEvent
         */
        @Override
        public void mouseDragged(MouseEvent e) {
            if (draggedPiece != null) {
                dragX = e.getX();
                dragY = e.getY();

                // Update hover square
                hoverSquare = getBoardCoordinates(e.getX(), e.getY());

                repaint();
            }
        }

        /**
         * Handles event when the mouse is released
         * @param e MouseEvent
         */
        @Override
        public void mouseReleased(MouseEvent e) {
            if (draggedPiece != null) {
                Point coords = getBoardCoordinates(e.getX(), e.getY());
                boolean moveExecuted = false;

                // Check if dropped on board
                if (coords != null) {
                    int targetRow = coords.y;
                    int targetCol = coords.x;

                    // Try to Move
                    if (currentLegalMoves != null) {
                        for (Move m : currentLegalMoves) {
                            if (m.endRow() == targetRow && m.endCol() == targetCol) {
                                // Handle Promotion
                                Move finalMove = handlePromotion(m);
                                finalizeTurn(finalMove); // EXECUTE
                                moveExecuted = true;
                                break;
                            }
                        }
                    }
                }

                // Handle Drop Logic (If NOT moved)
                if (!moveExecuted) {
                    // If we dragged and dropped back on the same square (or invalid square), then we treat it as a "Click/Select" action.

                    if (selectedPiece == draggedPiece) {
                        // Was selected, clicked again -> DESELECT
                        selectedPiece = null;
                        selectedLegalMoves = null;
                    } else {
                        // Was not selected (or different) -> SELECT
                        selectedPiece = draggedPiece;
                        selectedLegalMoves = currentLegalMoves;
                    }
                }

                // Reset Drag State
                draggedPiece = null;
                hoverSquare = null;
                currentLegalMoves = null; // Clear list after use

                repaint();
            }
        }
    }
}
