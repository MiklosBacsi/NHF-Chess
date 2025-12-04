package view.game;

import model.*;
import model.rules.ClassicalVariant;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
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

    // --- Drag & Drop State ---
    private Piece draggedPiece = null;
    private int dragX, dragY; // Current pixel coordinates of the mouse
    private int dragOffsetX, dragOffsetY; // To keep the mouse relative to the piece corner
    private Point hoverSquare = null; // Stores x=col, y=row of the square under mouse
    private List<Move> currentLegalMoves = null; // All the squares a piece can legally move to

    // --- Visuals ---
    private BoardTheme currentTheme = BoardTheme.BROWN;
    private static final int BOARD_PADDING = 20;
    private final Color moveHighlightColor = new Color(255, 255, 0, 96); // Source (Yellow)
    private final Color targetHighlightColor = new Color(255, 255, 255, 100); // Target (White overlay)
    private final Color moveHintColor = new Color(0, 0, 0, 50); // Dark dot for valid squares
    private final Color checkHighlightColor = new Color(255, 0, 0, 128);
    private PieceColor viewPerspective = PieceColor.WHITE; // Determines which side is at the bottom of the screen

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

        // Select Rules Strategy
        // We will add the other classes later, for now we map them!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!1
        switch (mode) {
            case "Classical":
                this.gameRules = new ClassicalVariant();
                break;
            // Placeholders for future:
            // case "Fog of War": this.gameRules = new FogOfWarVariant(); break;
            // case "Duck Chess": this.gameRules = new DuckChessVariant(); break;
            default:
                this.gameRules = new ClassicalVariant(); // Fallback
                break;
        }

        this.viewPerspective = PieceColor.WHITE;

        // Clear UI state
        draggedPiece = null;
        hoverSquare = null;
        currentLegalMoves = null;

        // Redraw
        repaint();
    }

    /**
     * Loads the textures in memory
     */
    private void loadResources() {
        String[] colors = {"white", "black"};
        String[] types = {"pawn", "rook", "knight", "bishop", "queen", "king"};

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

        int minimumDimension = Math.min(width, height) - (BOARD_PADDING * 2);
        if (minimumDimension < 0) minimumDimension = 0;

        // Calculate Square Size dynamically, and take the smaller dimension so the board always fits inside the window
        this.squareSize = minimumDimension / 8;

        // Calculate centering (to keep board in middle)
        int boardPixelSize = squareSize * 8;
        this.startX = (width - boardPixelSize) / 2;
        this.startY = (height - boardPixelSize) / 2;

        boolean whiteInCheck = gameRules.isCheck(board, PieceColor.WHITE);
        boolean blackInCheck = gameRules.isCheck(board, PieceColor.BLACK);

        // We need to know WHERE the kings are to highlight them
        Piece whiteKing = board.findKing(PieceColor.WHITE);
        Piece blackKing = board.findKing(PieceColor.BLACK);

        Move lastMove = board.getLastMove();

        // Draw the Board & Pieces
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                // TRANSFORM TO VISUAL COORDS
                int visualRow = toVisualRow(row);
                int visualCol = toVisualCol(col);

                int x = startX + (visualCol * squareSize);
                int y = startY + (visualRow * squareSize);

                // Determine color of square
                if ((row + col) % 2 == 0) {
                    g2d.setColor(currentTheme.getLight());
                } else {
                    g2d.setColor(currentTheme.getDark());
                }

                // Draw the square (Background)
                g2d.fillRect(x, y, squareSize, squareSize);


                // --- Highlights/Hints logic uses MODEL coords to check, but draws at VISUAL coords (x,y) ---

                // Draw Last Move Highlight (only draw if NOT dragging, and there is a history)
                if (draggedPiece == null && lastMove != null) {
                    boolean isStart = (lastMove.startRow() == row && lastMove.startCol() == col);
                    boolean isEnd = (lastMove.endRow() == row && lastMove.endCol() == col);

                    if (isStart || isEnd) {
                        g2d.setColor(moveHighlightColor); // Reusing the Yellow
                        g2d.fillRect(x, y, squareSize, squareSize);
                    }
                }

                // Draw Source Highlight (if this is the start square of the dragged piece)
                if (draggedPiece != null && draggedPiece.getRow() == row && draggedPiece.getCol() == col) {
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
        if (draggedPiece != null && currentLegalMoves != null) {
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

            for (Move move : currentLegalMoves) {
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
     * --- Inner Class for Mouse Handling ---
     */
    private class DragController extends MouseAdapter {

        /**
         * Handles event when the mouse is pressed
         * @param e MouseEvent
         */
        @Override
        public void mousePressed(MouseEvent e) {
            Point coords = getBoardCoordinates(e.getX(), e.getY());

            // Checks if we clicked a valid square
            if (coords != null) {
                Piece clickedPiece = board.getPiece(coords.y, coords.x);

                // Checks if another piece is already there
                if (clickedPiece != null) {

                    // Turn Validation
                    if (clickedPiece.getColor() != board.getCurrentPlayer()) {
                        System.out.println("Not your turn!");
                        return; // Ignore click
                    }

                    draggedPiece = clickedPiece;
                    hoverSquare = coords;
                    currentLegalMoves = gameRules.getLegalMoves(board, draggedPiece);

                    dragX = e.getX();
                    dragY = e.getY();

                    // Calculate visual offset (so piece doesn't jump to top-left of mouse)
                    int visualRow = toVisualRow(coords.y);
                    int visualCol = toVisualCol(coords.x);
                    int pieceX = startX + (visualCol * squareSize);
                    int pieceY = startY + (visualRow * squareSize);
                    dragOffsetX = e.getX() - pieceX;
                    dragOffsetY = e.getY() - pieceY;

                    repaint();
                }
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

                // Check if dropped on board
                if (coords != null) {
                    int targetRow = coords.y;
                    int targetCol = coords.x;

                    boolean isValid = false;

                    // Check if the move is Possible
                    if (currentLegalMoves != null) {
                        for (Move m : currentLegalMoves) {
                            if (m.endRow() == targetRow && m.endCol() == targetCol) {
                                isValid = true;
                                // Execute Move in Model
                                board.executeMove(m);

                                // Switch Turn
                                board.switchTurn();

                                // Rotate view to match the new player
                                viewPerspective = board.getCurrentPlayer();

                                break;
                            }
                        }
                    }

                    if (!isValid) {
                        System.out.println("Invalid Move! Snapping back.");
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
