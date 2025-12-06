package view.game;

import model.*;
import model.pieces.Duck;
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

        // Reset padding
        this.boardPadding = 20;

        // Select Rules & Special Setup
        switch (mode) {
            case "Classical":
                this.gameRules = new ClassicalVariant();
                break;

            case "Fog of War":
                this.gameRules = new FogOfWarVariant();
                break;

            case "Duck Chess":
                this.gameRules = new DuckChessVariant();
                board.placePiece(new Duck(3, 3), 3, 3); // d5
                break;

            case "Crazyhouse":
                this.gameRules = new CrazyhouseVariant();
                board.setCrazyhouseMode(true); // ENABLE RESERVES
                this.boardPadding = 90;
                break;

            default:
                this.gameRules = new ClassicalVariant(); // Fallback
                break;
        }

        this.viewPerspective = PieceColor.WHITE;

        // Calculate initial visibility for White
        if (gameRules instanceof FogOfWarVariant) {
            calculateVisibility();
        }

        isGameOver = false;

        // Clear UI state
        draggedPiece = null;
        hoverSquare = null;
        currentLegalMoves = null;
        selectedPiece = null;
        selectedLegalMoves = null;

        // Redraw
        repaint();
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
                int visualRow = toVisualRow(row);
                int visualCol = toVisualCol(col);

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
                int visualRow = toVisualRow(move.endRow());
                int visualCol = toVisualCol(move.endCol());

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
            int modelRow = toModelRow(visualRow);
            int modelCol = toModelCol(visualCol);
            return new Point(modelCol, modelRow);
        }
        // Outside the board
        return null;
    }

    /**
     * Converts Model Row (0..7) to Visual Row (0..7) based on perspective.
     * If WHITE view: Row 0 is top, Row 7 is bottom (Standard).
     * If BLACK view: Row 0 is bottom, Row 7 is top (Rotated by 180 degrees).
     */
    private int toVisualRow(int modelRow) {
        if (viewPerspective == PieceColor.WHITE) {
            return modelRow;
        } else {
            return 7 - modelRow;
        }
    }

    /**
     * Converts Model Column (0..7) to Visual Column (0..7) based on perspective.
     * If WHITE view: Column 0 is left, Column 7 is right (Standard).
     * If BLACK view: Column right is bottom, Column 7 is left (Rotated by 180 degrees).
     * @param modelCol model column index
     * @return visual column index
     */
    private int toVisualCol(int modelCol) {
        if (viewPerspective == PieceColor.WHITE) {
            return modelCol;
        } else {
            return 7 - modelCol;
        }
    }

    /**
     * Converts Visual coordinates (what we clicked) back to Model coordinates.
     * For 180-degree rotation, the math is identical (X = 7 - X),
     * but we separate it in case we add 90-degree rotation.
     * @param visualRow visual row index
     * @return model row index
     */
    private int toModelRow(int visualRow) {
        if (viewPerspective == PieceColor.WHITE) {
            return visualRow;
        } else {
            return 7 - visualRow;
        }
    }

    /**
     * Converts Visual coordinates (what we clicked) back to Model coordinates.
     * For 180-degree rotation, the math is identical (X = 7 - X),
     * but we separate it in case we add 90-degree rotation.
     * @param visualCol visual column index
     * @return model column index
     */
    private int toModelCol(int visualCol) {
        if (viewPerspective == PieceColor.WHITE) {
            return visualCol;
        } else {
            return 7 - visualCol;
        }
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
     * Helper to handle turn-switching, rules and game over checks
     * @param move move that was made
     */
    private void finalizeTurn(Move move) {

        // Execute the Move on the Board
        board.executeMove(move);

        // Check Win Condition (King Capture)
        if (move.capturedPiece() != null && move.capturedPiece().getType() == PieceType.KING) {
            System.out.println("---------------------------------------");
            System.out.println("GAME OVER! " + move.piece().getColor() + " captured the King!");
            System.out.println("---------------------------------------");
            isGameOver = true;
            repaint();
            return;
        }

        // Handle Rules (Duck / Classical)
        if (gameRules instanceof DuckChessVariant) {
            if (move.type() == model.MoveType.DUCK) {
                board.setWaitingForDuck(false);
                board.switchTurn();
                viewPerspective = board.getCurrentPlayer();
            } else {
                board.setWaitingForDuck(true);
            }
        } else {
            // Classical / Fog / Others
            board.switchTurn();
            viewPerspective = board.getCurrentPlayer();
        }

        // Fog of War Logic
        if (gameRules instanceof model.rules.FogOfWarVariant) {
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
                System.out.println("CHECKMATE! " + winner + " wins!");
                isGameOver = true;
            } else if (gameRules.isStalemate(board, activePlayer)) {
                System.out.println("STALEMATE!");
                isGameOver = true;
            }
        }

        // Clear Selections
        selectedPiece = null;
        selectedLegalMoves = null;
        draggedPiece = null;
        currentLegalMoves = null;

        repaint();
    }

    /**
     * Helper: extract Promotion Dialog Logic
     * @param move promotion move that was made (moving the pawn to the opposite row)
     * @return transformed move that is now promotion move
     */
    private Move handlePromotion(Move move) {
        if (move.type() == model.MoveType.PROMOTION) {
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
                        case ROOK -> virtualPiece = new model.pieces.Rook(myColor, -1, -1);
                        case KNIGHT -> virtualPiece = new model.pieces.Knight(myColor, -1, -1);
                        case BISHOP -> virtualPiece = new model.pieces.Bishop(myColor, -1, -1);
                        case QUEEN -> virtualPiece = new model.pieces.Queen(myColor, -1, -1);
                        default -> virtualPiece = new model.pieces.Pawn(myColor, -1, -1);
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
                int visualRow = toVisualRow(coords.y);
                int visualCol = toVisualCol(coords.x);
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
