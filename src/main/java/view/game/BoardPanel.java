package view.game;

import model.Board;
import model.Piece;
import model.Move;

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

public class BoardPanel extends JPanel {

    // --- State ---
    private final Board board;
    private final Map<String, BufferedImage> pieceImages = new HashMap<>();

    // --- Drag & Drop State ---
    private Piece draggedPiece = null;
    private int dragX, dragY; // Current pixel coordinates of the mouse
    private int dragOffsetX, dragOffsetY; // To keep the mouse relative to the piece corner
    private Point hoverSquare = null; // Stores x=col, y=row of the square under mouse
    private final Color dragHighlightColor = new Color(255, 255, 0, 128); // Source (Yellow)
    private final Color targetHighlightColor = new Color(255, 255, 255, 100); // Target (White overlay)
    private final Color moveHintColor = new Color(0, 0, 0, 50); // Dark dot for valid squares
    private List<Move> currentLegalMoves = null; // All the squares a piece can legally move to

    // --- Visuals ---
    private BoardTheme currentTheme = BoardTheme.BROWN;
    private static final int BOARD_PADDING = 20;

    // Cached calculation values to share between paint() and mouse listeners
    private int startX, startY, squareSize;

    public BoardPanel() {
        this.board = new Board();
        setOpaque(false);
        loadResources();

        // Initialize Mouse Listeners
        MouseAdapter mouseHandler = new DragController();
        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);
    }

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

    public void setTheme(BoardTheme theme) {
        this.currentTheme = theme;
        repaint();
    }

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

        // Draw the Board & Pieces
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                int x = startX + (col * squareSize);
                int y = startY + (row * squareSize);

                // Determine color of square
                if ((row + col) % 2 == 0) {
                    g2d.setColor(currentTheme.getLight());
                } else {
                    g2d.setColor(currentTheme.getDark());
                }
                g2d.fillRect(x, y, squareSize, squareSize); // Draw the square

                // Draw Source Highlight (if this is the start square of the dragged piece)
                if (draggedPiece != null && draggedPiece.getRow() == row && draggedPiece.getCol() == col) {
                    g2d.setColor(dragHighlightColor);
                    g2d.fillRect(x, y, squareSize, squareSize);
                }

                // Draw Target Highlight (White)
                if (draggedPiece != null && hoverSquare != null && hoverSquare.y == row && hoverSquare.x == col) {
                    g2d.setColor(targetHighlightColor);
                    g2d.fillRect(x, y, squareSize, squareSize);
                }

                // Draw Piece (if it's not being dragged)
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
                int gx = startX + (move.endCol() * squareSize);
                int gy = startY + (move.endRow() * squareSize);

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

    private void drawPiece(Graphics2D g2d, Piece piece, int x, int y) {
        String key = piece.getFilename();
        BufferedImage img = pieceImages.get(key);
        if (img != null) {
            g2d.drawImage(img, x, y, squareSize, squareSize, this);
        }
    }

    // Converts screen pixel coordinate into a chess board one
    private Point getBoardCoordinates(int x, int y) {
        if (squareSize == 0) return null;
        int col = (x - startX) / squareSize;
        int row = (y - startY) / squareSize;
        if (col >= 0 && col < 8 && row >= 0 && row < 8) {
            return new Point(col, row);
        }
        // Outside the board
        return null;
    }


    // --- Inner Class for Mouse Handling ---
    private class DragController extends MouseAdapter {

        @Override
        public void mousePressed(MouseEvent e) {
            Point coords = getBoardCoordinates(e.getX(), e.getY());

            // Checks if we clicked a valid square
            if (coords != null) {
                Piece clickedPiece = board.getPiece(coords.y, coords.x);

                // Checks if another piece is already there
                if (clickedPiece != null) {
                    draggedPiece = clickedPiece;
                    hoverSquare = coords;
                    currentLegalMoves = draggedPiece.getPseudoLegalMoves(board);

                    dragX = e.getX();
                    dragY = e.getY();

                    // Calculate offset so piece doesn't jump to top-left of mouse
                    int pieceX = startX + (coords.x * squareSize);
                    int pieceY = startY + (coords.y * squareSize);
                    dragOffsetX = e.getX() - pieceX;
                    dragOffsetY = e.getY() - pieceY;

                    repaint();
                }
            }
        }

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

        @Override
        public void mouseReleased(MouseEvent e) {
            if (draggedPiece != null) {
                Point coords = getBoardCoordinates(e.getX(), e.getY());

                // Check if dropped on board
                if (coords != null) {
                    int targetRow = coords.y;
                    int targetCol = coords.x;

                    // Check if the move is Geometrically Possible
                    List<Move> legalMoves = draggedPiece.getPseudoLegalMoves(board);

                    boolean isValid = false;
                    for (Move m : legalMoves) {
                        if (m.endRow() == targetRow && m.endCol() == targetCol) {
                            isValid = true;
                            // Execute Move in Model
                            board.executeMove(m);
                            break;
                        }
                    }

                    if (!isValid) {
                        System.out.println("Invalid Move! Snapping back.");
                    }
                }

                // Reset Drag State
                draggedPiece = null;
                hoverSquare = null;

                repaint();
            }
        }
    }
}
