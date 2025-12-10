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
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;

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
    private boolean drawOfferPending = false; // True if current player clicked Draw

    // Clock State
    private ChessClock chessClock;
    private final Timer gameLoopTimer; // Swing Timer for repainting

    // --- In-Game Review State ---
    private boolean isInGameReview = false;
    private List<Move> liveMoveHistoryBackup = null; // Snapshot of real moves
    private int reviewIndex = 0;
    private PieceColor livePerspectiveSnapshot = null; // To keep the board from spinning
    private PieceColor liveCurrentPlayerSnapshot = null; // Snapshot of real current player

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
    // UI Buttons
    private JButton btnResign;
    private JButton btnDraw;
    // --- For Game History ---
    private String currentModeName = "Classical";
    private boolean isReplayMode = false;
    private List<SavedMove> replayMoves;
    private int currentReplayIndex = 0;
    // Perspective Control
    private final JButton switchPerspectiveBtn;
    private PieceColor lockedReplayPerspective = null; // null = Auto (Follow Move)

    // Cached calculation values to share between paint() and mouse listeners
    private int startX, startY, squareSize;

    private GameVariant gameRules; // The Strategy


    /**
     * Constructor of BoardPanel which creates a new BoardPanel.
     */
    public BoardPanel() {
        this.board = new Board();
        setOpaque(false);
        setFocusable(true); // enable keyboard
        setLayout(null); // Absolute layout for dynamic positioning

        loadResources();

        // Default startup
        this.gameRules = new ClassicalVariant();

        // Perspective Button
        switchPerspectiveBtn = new JButton("View: Dynamic");
        switchPerspectiveBtn.setFocusable(false); // Don't steal focus from KeyListener
        switchPerspectiveBtn.setBackground(new Color(60, 60, 60));
        switchPerspectiveBtn.setForeground(Color.WHITE);
        switchPerspectiveBtn.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        switchPerspectiveBtn.setVisible(false); // Hidden by default
        switchPerspectiveBtn.addActionListener(e -> cycleReplayPerspective());
        add(switchPerspectiveBtn);

        // --- RESIGN BUTTON ---
        btnResign = new JButton("Resign");
        btnResign.setFocusable(false);
        btnResign.setBackground(new Color(200, 60, 60));
        btnResign.setForeground(Color.WHITE);
        btnResign.addActionListener(e -> onResignClicked());
        add(btnResign);

        // --- DRAW BUTTON ---
        btnDraw = new JButton("Draw");
        btnDraw.setFocusable(false);
        btnDraw.setBackground(new Color(100, 100, 150));
        btnDraw.setForeground(Color.WHITE);
        btnDraw.addActionListener(e -> onDrawClicked());
        add(btnDraw);

        // Initialize Mouse Listeners
        MouseAdapter mouseHandler = new DragController();
        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);

        // Key Listener for Replay
        addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent e) {
                int key = e.getKeyCode();

                // History Panel Replay (Loaded from file)
                if (isReplayMode) {
                    if (key == java.awt.event.KeyEvent.VK_RIGHT) replayStep(1);
                    else if (key == java.awt.event.KeyEvent.VK_LEFT) replayStep(-1);
                }
                // In-Game Review (Live Game)
                else {
                    if (key == java.awt.event.KeyEvent.VK_LEFT) {
                        if (!board.getMoveHistory().isEmpty()) {
                            stepInGameReview(-1);
                        }
                    }
                    else if (key == java.awt.event.KeyEvent.VK_RIGHT) {
                        // Only allow right if already reviewing
                        if (isInGameReview) {
                            stepInGameReview(1);
                        }
                    }
                }
            }
        });
        // Create a timer that ticks every 100ms to update clocks
        gameLoopTimer = new Timer(100, e -> updateGameLoop());
    }

    /**
     * It creates a new game with the chosen variant.
     * @param mode determines the type of the game variant
     */
    public void setupGame(String mode, TimeSettings timeSettings) {
        this.currentModeName = mode;
        this.isReplayMode = false;
        isInGameReview = false;
        liveMoveHistoryBackup = null;
        livePerspectiveSnapshot = null;
        liveCurrentPlayerSnapshot = null;

        // Reset Model
        board.resetBoard();

        this.viewPerspective = PieceColor.WHITE;

        // Reset padding
        this.boardPadding = 40;

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
                this.boardPadding = 120;
                break;

            case "Chaturaji":
                this.gameRules = new ChaturajiVariant();
                setupChaturajiBoard();
                this.viewPerspective = PieceColor.RED;
                board.setCurrentPlayer(PieceColor.RED);
                this.boardPadding = 65;
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

        // Setup Clock
        if (timeSettings != null) {
            this.chessClock = new ChessClock(timeSettings);

            // Start clock for the first player (White or Red)
            PieceColor startColor = (gameRules instanceof ChaturajiVariant) ? PieceColor.RED : PieceColor.WHITE;
            chessClock.start(startColor);
            gameLoopTimer.start();
        }

        isGameOver = false;

        // Clear UI state
        isBlindMode = false;
        draggedPiece = null;
        hoverSquare = null;
        currentLegalMoves = null;
        selectedPiece = null;
        selectedLegalMoves = null;
        drawOfferPending = false;
        btnDraw.setText("Draw");
        btnDraw.setEnabled(true);

        requestFocusInWindow(); // enable keyboard

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
        // Standard black & white pieces
        String[] colors = {"white", "black"};
        String[] types = {"pawn", "rook", "knight", "bishop", "queen", "king"};
        loadTextures(colors, types);

        // Duck
        String[] duckColor = {"special"};
        String[] duckType = {"duck"};
        loadTextures(duckColor, duckType);

        // Chaturaji
        String[] chaturajiColors = {"red", "blue", "yellow", "green"};
        String[] chaturajiTypes = {"pawn", "boat", "knight", "bishop", "king"};
        loadTextures(chaturajiColors, chaturajiTypes);

        // Dead pieces
        String[] deadColor = {"grey"};
        String[] deadTypes = {"pawn", "rook", "boat", "knight", "bishop", "queen", "king"};
        loadTextures(deadColor, deadTypes);
    }

    /**
     * Helper to load textures.
     * @param deadColor color of the dead pieces (grey)
     * @param deadTypes types of the dead pieces
     */
    private void loadTextures(String[] deadColor, String[] deadTypes) {
        for (String c : deadColor) {
            for (String t : deadTypes) {
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
     * Sets the theme to the chosen one.
     * @param theme chosen theme
     */
    public void setTheme(BoardTheme theme) {
        this.currentTheme = theme;
        repaint();
    }

    /**
     * Helper that helps us cycle through different perspectives in Game History.
     */
    private void cycleReplayPerspective() {
        PieceColor[] cycle;

        // Determine the cycle order based on Variant
        if (gameRules instanceof ChaturajiVariant) {
            cycle = new PieceColor[]{PieceColor.RED, PieceColor.BLUE, PieceColor.YELLOW, PieceColor.GREEN};
        } else {
            cycle = new PieceColor[]{PieceColor.WHITE, PieceColor.BLACK};
        }

        // Logic: null (Dynamic) -> First Color -> Second Color -> ... -> null
        if (lockedReplayPerspective == null) {
            // Switch to first color
            lockedReplayPerspective = cycle[0];
        } else {
            // Find current index
            int index = -1;
            for (int i = 0; i < cycle.length; i++) {
                if (cycle[i] == lockedReplayPerspective) {
                    index = i;
                    break;
                }
            }

            // Go to next, or back to null if at end
            if (index == cycle.length - 1) {
                lockedReplayPerspective = null; // Back to Dynamic
            } else {
                lockedReplayPerspective = cycle[index + 1];
            }
        }

        // Update Text
        updatePerspectiveButtonText();

        // Apply Change immediately
        updateReplayPerspective();

        // Update Fog if necessary
        if (gameRules instanceof FogOfWarVariant) {
            calculateVisibility();
        }

        repaint();
    }

    /**
     * Helper to update text of perspective button.
     */
    private void updatePerspectiveButtonText() {
        if (lockedReplayPerspective == null) {
            switchPerspectiveBtn.setText("View: Dynamic");
        } else {
            switchPerspectiveBtn.setText("View: " + lockedReplayPerspective);
        }
    }

    /**
     * Helper to implement resign dialog.
     */
    private void onResignClicked() {
        if (isGameOver || isReplayMode || isInGameReview) return;

        // Confirm
        int choice = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to resign?", "Resign", JOptionPane.YES_NO_OPTION);

        if (choice == JOptionPane.YES_OPTION) {
            if (!(gameRules instanceof ChaturajiVariant)) {
                isGameOver = true; // Lock the Game
            }

            PieceColor me = board.getCurrentPlayer();

            // Create Resign Move
            Piece dummy = new King(me, -1, -1);
            Move m = new Move(dummy, -1, -1, -1, -1,
                    MoveType.RESIGN, null, false, null);
            finalizeTurn(m);

            if (!(gameRules instanceof ChaturajiVariant)) {
                PieceColor winner = board.getCurrentPlayer();      // Current player
                PieceColor resigner = board.getCurrentPlayer().next(); // Previous player

                showGameOverDialog("GAME OVER! " + resigner + " resigned, so " + winner + " wins!");
            }
        }
    }

    /**
     * Helper to register draw offer.
     */
    private void onDrawClicked() {
        if (isGameOver || isReplayMode || isInGameReview) return;

        // Just set the flag. Logic happens after turn switch.
        drawOfferPending = true;
        btnDraw.setText("Offer Sent");
        btnDraw.setEnabled(false); // Disable to prevent spamming

        // Regain focus for board interaction
        requestFocusInWindow();
    }

    /**
     * This method is responsible for drawing the board with its pieces.
     * @param g Graphics (drawing on this)
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        // --- HIGH QUALITY SETTINGS ---
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        // --- Position Perspective Button ---
        if (isReplayMode) {
            if (!switchPerspectiveBtn.isVisible()) {
                switchPerspectiveBtn.setVisible(true);
                updatePerspectiveButtonText(); // Ensure text is correct on first show
            }

            int btnWidth = 120;
            int btnHeight = 30;
            int boardCenter = startX + (squareSize * 4);

            // Place it 40px above the board (centered horizontally)
            int btnX = boardCenter - (btnWidth / 2);
            int btnY = startY - btnHeight - 6;

            if (gameRules instanceof ChaturajiVariant) {
                btnY -= 25;
            }
            if (gameRules instanceof CrazyhouseVariant) {
                btnY -= 80;
            }

            switchPerspectiveBtn.setBounds(btnX, btnY, btnWidth, btnHeight);
        } else {
            if (switchPerspectiveBtn.isVisible()) {
                switchPerspectiveBtn.setVisible(false);
            }
        }

        // --- BUTTON VISIBILITY & POSITION ---
        boolean buttonsVisible = !(isReplayMode || isInGameReview) && !isGameOver;

        if (buttonsVisible) {
            int crazyOffset = (gameRules instanceof CrazyhouseVariant) ? 78 : 0;

            int btnWidth = 100;
            int btnHeight = 30;
            int y = startY + (squareSize * 8) + 5 + crazyOffset;

            // Resign (Right Edge)
            btnResign.setBounds(startX + (squareSize * 8) - btnWidth, y, btnWidth, btnHeight);
            btnResign.setVisible(true);

            // Draw (Left Edge) - Hidden in Chaturaji
            if (gameRules instanceof ChaturajiVariant) {
                btnDraw.setVisible(false);
            } else {
                btnDraw.setBounds(startX, y, btnWidth, btnHeight);
                btnDraw.setVisible(true);
            }
        } else {
            btnResign.setVisible(false);
            btnDraw.setVisible(false);
        }

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

        // Determine last moves to highlight
        List<Move> recentMoves = getRecentMovesForHighlight();

        // Checks
        boolean whiteInCheck = gameRules.isCheck(board, PieceColor.WHITE);
        boolean blackInCheck = gameRules.isCheck(board, PieceColor.BLACK);

        // We need to know WHERE the kings are to highlight them
        Piece whiteKing = board.findKing(PieceColor.WHITE);
        Piece blackKing = board.findKing(PieceColor.BLACK);

        Move lastMove = board.getLastMove();

        // Helper: What are we focusing on? Dragged takes priority, then Selected.
        Piece activePiece = (draggedPiece != null) ? draggedPiece : selectedPiece;
        List<Move> activeMoves = (draggedPiece != null) ? currentLegalMoves : selectedLegalMoves;

        // --- DRAW BOARD & PIECES ---
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                drawSingleSquareWithPieceAndHighlights(
                        row, col, g2d, recentMoves, activePiece, whiteKing, blackKing, whiteInCheck, blackInCheck
                );
            }
        }

        // Draw Move Hints (Dots and Rings)
        drawMoveHints(g2d, activePiece, activeMoves);

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

        // --- DRAW CLOCK ---
        drawClocks(g2d);

        // --- DRAW REVIEW INDICATOR ---
        drawReviewIndicator(g);
    }

    /**
     * Helper to draw a single square with piece and highlights.
     * @param row row index
     * @param col column index
     * @param g2d Graphics2D to draw on
     * @param recentMoves list of the recent move that get highlighted
     * @param activePiece active piece
     * @param whiteKing white king
     * @param blackKing black king
     * @param whiteInCheck contains whether the white king is in check
     * @param blackInCheck contains whether the black king is in check
     */
    private void drawSingleSquareWithPieceAndHighlights(
            int row, int col, Graphics2D g2d, List<Move> recentMoves, Piece activePiece,
            Piece whiteKing, Piece blackKing, boolean whiteInCheck, boolean blackInCheck
    )   {

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
        if (!(gameRules instanceof FogOfWarVariant)) isVisible = true;


        // Determine color of square
        boolean isLightSquare = (row + col) % 2 == 0;
        if (isLightSquare) {
            g2d.setColor(currentTheme.getLight());
        } else {
            g2d.setColor(currentTheme.getDark());
        }

        // Draw the square (Background)
        g2d.fillRect(x, y, squareSize, squareSize);

        // --- DRAW COORDINATES ---
        int fontSize = Math.max(12, squareSize / 5);
        g2d.setFont(new Font("Arial", Font.BOLD, fontSize));

        // Text Color: Inverse of the square color
        if (isLightSquare) g2d.setColor(currentTheme.getDark());
        else g2d.setColor(currentTheme.getLight());

        int padding = fontSize / 4;

        // Determine if board is "Upright" (Red/Yellow/White/Black) or "Sideways" (Blue/Green)
        boolean isUpright = (viewPerspective == PieceColor.RED ||
                viewPerspective == PieceColor.YELLOW ||
                viewPerspective == PieceColor.WHITE ||
                viewPerspective == PieceColor.BLACK);

        // LEFT EDGE (Visual Vertical Axis)
        if (visualCol == 0) {
            String label;
            if (isUpright) {
                // Standard: Vertical axis shows Ranks (1-8)
                label = String.valueOf(8 - row);
            } else {
                // Sideways: Vertical axis shows Files (a-h)
                label = String.valueOf((char)('a' + col));
            }
            g2d.drawString(label, x + padding, y + fontSize);
        }

        // BOTTOM EDGE (Visual Horizontal Axis)
        if (visualRow == 7) {
            String label;
            if (isUpright) {
                // Standard: Horizontal axis shows Files (a-h)
                label = String.valueOf((char)('a' + col));
            } else {
                // Sideways: Horizontal axis shows Ranks (1-8)
                label = String.valueOf(8 - row);
            }

            int textWidth = g2d.getFontMetrics().stringWidth(label);
            g2d.drawString(label, x + squareSize - textWidth - (padding * 3 / 4), y + squareSize - padding);
        }


        // IF NOT VISIBLE -> DRAW FOG AND SKIP CONTENT
        if (!isVisible) {
            // Use the GREY theme colors to represent fog
            if ((row + col) % 2 == 0) {
                g2d.setColor(BoardTheme.GREY.getLight());
            } else {
                g2d.setColor(BoardTheme.GREY.getDark());
            }
            g2d.fillRect(x, y, squareSize, squareSize);
            return; // STOP HERE for this square (Don't draw pieces/highlights)
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

    /**
     * Helper to draw Move Hints (Dots and Rings).
     * @param g2d Graphics2D to draw on
     */
    private void drawMoveHints(Graphics2D g2d, Piece activePiece, List<Move> activeMoves) {
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
    }

    /**
     * Helper to determine recent moves for highlight.
     * @return last moves that become highlighted on the board
     */
    private List<Move> getRecentMovesForHighlight () {
        // Determine last moves to highlight
        List<Move> recentMoves;

        if (gameRules instanceof ChaturajiVariant) {
            // Get raw history (max 4 moves)
            // Note: getLastMoves returns [Newest, Older, Oldest...]
            recentMoves = board.getLastMoves(4);

            // Remove latest move when playing (and not replaying)
            if (!(isReplayMode || isInGameReview)) {
                if (recentMoves.size() == 4) {
                    recentMoves.removeLast();
                }
            }

            // Repetition Filter (Keep moves until we find a color that has already moved)
            List<Move> filteredMoves = new ArrayList<>();
            Set<PieceColor> seenColors = new HashSet<>();

            for (Move move : recentMoves) {
                PieceColor color = move.piece().getColor();

                // Stop here to remove repetition
                if (seenColors.contains(color)) {
                    break;
                }

                seenColors.add(color);
                filteredMoves.add(move);
            }

            recentMoves = filteredMoves;

        } else {
            // Default: show 1 move (2 in duck chess)
            int movesToShow = (gameRules instanceof DuckChessVariant) ? 2 : 1;
            recentMoves = board.getLastMoves(movesToShow);
        }
        return recentMoves;
    }

    /**
     * Helper to draw review indicator in in-game replay.
     * @param g Graphics to draw on
     */
    private void drawReviewIndicator(Graphics g) {
        if (isInGameReview) {
            String msg = "REVIEWING: " + reviewIndex + "/" + liveMoveHistoryBackup.size();

            // Setup Font
            g.setFont(new Font("Arial", Font.BOLD, 20));
            FontMetrics fm = g.getFontMetrics();
            int w = fm.stringWidth(msg);
            int h = fm.getHeight(); // Roughly matches clock height

            // Calculate Y (Match Top Clock Baseline)
            int padding = 10;
            int scoreOffset = (gameRules instanceof ChaturajiVariant) ? 25 : 0;
            int reserveOffset = (gameRules instanceof CrazyhouseVariant) ? 80 : 0;

            int y = startY - padding - scoreOffset - reserveOffset;

            // Calculate X (Left of the Top Clock)
            int boardCenterX = startX + (squareSize * 4);

            // "00:00" in Monospaced 20 is approx 60px wide.
            // Half width (30) + Bubble Padding (5) + Gap (20) = ~55px offset from center
            int clockOffset = 55;

            int x = Math.min(startX, boardCenterX - clockOffset - w);

            // Draw Background Bubble
            g.setColor(new Color(50, 50, 50, 200));
            // Match the clock's fillRoundRect style: (x-5, y-h+5, w+10, h, 10, 10)
            g.fillRoundRect(x - 5, y - h + 5, w + 10, h, 10, 10);

            // Draw Text
            g.setColor(Color.WHITE);
            g.drawString(msg, x, y);
        }
    }

    /**
     * Recalculates visible squares.
     */
    private void calculateVisibility() {
        visibleSquares.clear();
        PieceColor myColor = this.viewPerspective;

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
                y = startY + (squareSize * 8) + h; // Below board
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
        g2d.fillRoundRect(x - 5, y - h + 5, w + 10, h, 10, 10);

        // Draw Text (Color-coded)
        g2d.setColor(getColorForPlayer(color));
        g2d.drawString(text, x, y);
    }

    /**
     * Shows game over dialog at the end of game.
     * @param message message that is shown on the dialog
     */
    private void showGameOverDialog(String message) {
        // Lock the game
        isGameOver = true;

        // Clear visual artifacts (dots, highlights)
        clearSelections();

        if (!isReplayMode) {
            // Get moves chronologically
            List<Move> allMoves = new ArrayList<>(board.getMoveHistory());

            // Save Game
            GameSaver.saveGame(currentModeName, message.replace("\n", " "), allMoves);
        }

        // Show the Popup
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(this, message, "Game Over", JOptionPane.INFORMATION_MESSAGE);
        });
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
            y = startY - RESERVE_SLOT_SIZE - 15;
        } else {
            y = startY + (squareSize * 8) + 15;
        }

        int x = startX + (slotIndex * (RESERVE_SLOT_SIZE + RESERVE_GAP));

        return new Rectangle(x, y, RESERVE_SLOT_SIZE, RESERVE_SLOT_SIZE);
    }

    /**
     * Updates the Chess Clock.
     */
    private void updateGameLoop() {
        if (isGameOver || chessClock == null) return;

        // Update Time
        chessClock.tick();

        // --- Check Timeout ---
        PieceColor playerToCheck;

        if (isInGameReview) {
            // In review, the board is in the past, so we MUST use the snapshot
            playerToCheck = liveCurrentPlayerSnapshot;
        } else {
            // In live game, the board is accurate
            playerToCheck = board.getCurrentPlayer();
        }

        if (chessClock.isFlagFallen(playerToCheck)) {
            handleTimeout(playerToCheck);
        }

        // Redraw (to show updated digits)
        repaint();
    }

    /**
     * Handles what happens when a player's time runs out.
     * @param loser color of the loser player
     */
    private void handleTimeout(PieceColor loser) {
        // SAFETY: If we are reviewing, force exit to Live State first!
        if (isInGameReview) {
            exitGameReview();
        }

        // Create Timeout Move (We create a dummy piece just to carry the Color info)
        Piece dummy = new King(loser, -1, -1);

        // Create a move with -1 coordinates and TIMEOUT type
        Move timeoutMove = new Move(
                dummy, -1, -1, -1, -1,
                MoveType.TIMEOUT,null, false, null
        );

        // Use finalizeTurn to handle history, saving, and UI updates
        finalizeTurn(timeoutMove);

        if (!(gameRules instanceof ChaturajiVariant)) {
            // Standard / Duck / Fog / Crazy -> Immediate Loss
            PieceColor winner = loser.next(); // Simplified (In 2 player)
            showGameOverDialog("TIMEOUT!\n" + loser + " ran out of time.\n" + winner + " wins!");
        }
    }

    /**
     * Helper for drawing all chess clocks.
     * @param g2d Graphics2D to draw on
     */
    private void drawClocks(Graphics2D g2d) {
        if (chessClock == null) return;

        // Determine players to show
        PieceColor[] players;
        if (gameRules instanceof ChaturajiVariant) {
            players = new PieceColor[]{PieceColor.RED, PieceColor.BLUE, PieceColor.YELLOW, PieceColor.GREEN};
        } else {
            players = new PieceColor[]{PieceColor.WHITE, PieceColor.BLACK};
        }

        for (PieceColor player : players) {
            if (board.isPlayerDead(player)) continue; // Don't show clock for dead players

            long millis = chessClock.getTime(player);
            drawSingleClock(g2d, player, formatTime(millis));
        }
    }

    /**
     * @param millis remaining time in milliseconds
     * @return Formated remaining time
     */
    private String formatTime(long millis) {
        if (millis < 0) millis = 0;
        long sec = millis / 1000;
        long min = sec / 60;
        sec = sec % 60;
        return String.format("%02d:%02d", min, sec);
    }

    /**
     * Helper to draw a single chess clock.
     * @param g2d Graphics2D to draw on
     * @param color color of the player
     * @param timeText text on the clock that we draw
     */
    private void drawSingleClock(Graphics2D g2d, PieceColor color, String timeText) {
        // Calculate Relative Position (0=Bottom, 1=Left, 2=Top, 3=Right)
        int relativePos = -1;

        if (gameRules instanceof ChaturajiVariant) {
            PieceColor p = viewPerspective;
            for (int i = 0; i < 4; i++) {
                if (p == color) {
                    relativePos = i;
                    break;
                }
                p = p.next();
            }
        } else {
            // Standard Chess (2 Player)
            if (color == viewPerspective) relativePos = 0; // Self (Bottom)
            else relativePos = 2; // Opponent (Top)
        }

        // Setup Font & Colors
        g2d.setFont(new Font("Monospaced", Font.BOLD, 20));
        FontMetrics fm = g2d.getFontMetrics();
        int w = fm.stringWidth(timeText);
        int h = fm.getHeight();
        int padding = 10;

        // Active Player Highlight
        if (color == viewPerspective) {
            g2d.setColor(new Color(255, 255, 255)); // Bright White for active
        } else {
            g2d.setColor(Color.LIGHT_GRAY);       // Gray for waiting
        }

        // Calculate Coordinates
        // We push the clock OUTWARDS (further from board than scores)
        int x = 0, y = 0;
        int scoreOffset = (gameRules instanceof ChaturajiVariant) ? 25 : 0;
        int reserveOffset = (gameRules instanceof CrazyhouseVariant) ? 80 : 0;

        switch (relativePos) {
            case 0: // BOTTOM
                x = startX + (squareSize * 4) - (w / 2);
                y = startY + (squareSize * 8) + h + scoreOffset + reserveOffset;
                break;
            case 1: // LEFT
                x = startX - w - padding;
                y = startY + (squareSize * 4) + (h / 3) + scoreOffset;
                break;
            case 2: // TOP
                x = startX + (squareSize * 4) - (w / 2);
                y = startY - padding - scoreOffset - reserveOffset;
                break;
            case 3: // RIGHT
                x = startX + (squareSize * 8) + padding;
                y = startY + (squareSize * 4) + (h / 3) + scoreOffset;
                break;
        }

        // Draw Background Bubble
        g2d.fillRoundRect(x - 5, y - h + 5, w + 10, h, 10, 10);

        // Draw Text (Black text on colored background)
        g2d.setColor(Color.BLACK);
        g2d.drawString(timeText, x, y);
    }

    /**
     * Stops the Chess Clock.
     */
    public void stopGame() {
        chessClock = null;

        if (gameLoopTimer.isRunning()) gameLoopTimer.stop();
    }

    /**
     * Helper to handle turn-switching, rules and game over checks.
     * It calls executeMoveLogic to execute purely the move first.
     * @param move move that was made
     */
    private void finalizeTurn(Move move) {
        // Ensure we are not in review mode when finalizing a real move
        if (isInGameReview) exitGameReview();

        // Run Logic
        executeMoveLogic(move);

        // --- UI: Check Game Over Conditions (Popups) ---

        // Chaturaji Game Over
        if (gameRules instanceof ChaturajiVariant) {
            if (board.getAlivePlayerCount() <= 1) {
                isGameOver = true;
                printChaturajiStandings();
            }
        }
        // King Capture (Duck/Fog)
        else if (move.capturedPiece() != null && move.capturedPiece().getType() == PieceType.KING) {
            String winner = move.piece().getColor().toString();
            showGameOverDialog("GAME OVER!\n" + winner + " captured the King and WINS!");
        }
        // Checkmate / Stalemate (Classical)
        else if (!isGameOver && !board.isWaitingForDuck()) {
            PieceColor activePlayer = board.getCurrentPlayer();
            if (gameRules.isCheckmate(board, activePlayer)) {
                PieceColor winner = activePlayer.next();
                showGameOverDialog("CHECKMATE!\n" + winner + " wins!");
            } else if (gameRules.isStalemate(board, activePlayer)) {
                String msg = (gameRules instanceof DuckChessVariant)
                        ? "STALEMATE!\n" + activePlayer + " has no moves and WINS!"
                        : "STALEMATE!\nThe game is a draw.";
                showGameOverDialog(msg);
            }
            // 50-Move Rule
            else if (gameRules.isDrawBy50MoveRule(board) && !(gameRules instanceof ChaturajiVariant)) {
                showGameOverDialog("DRAW!\n50-move rule (no pawn moves or captures).");
            }
        }

        // Fog of War UI
        if (gameRules instanceof FogOfWarVariant && !isReplayMode &&
                (move.capturedPiece() == null || move.capturedPiece().getType() != PieceType.KING)) {
            // Only show Pass Turn dialog if NOT in replay mode


            // PAUSE CLOCK
            if (chessClock != null) chessClock.stop();

            isBlindMode = true;
            repaint();

            SwingUtilities.invokeLater(() -> {
                Frame parent = (Frame) SwingUtilities.getWindowAncestor(BoardPanel.this);
                new PassTurnDialog(parent, board.getCurrentPlayer()).setVisible(true);
                isBlindMode = false;

                // RESUME CLOCK
                if (chessClock != null) chessClock.start(board.getCurrentPlayer());

                calculateVisibility();
                repaint();

                // Handle Draw Offer
                if (drawOfferPending) {
                    drawOfferPending = handleDrawLogicOffer();
                }
            });
        } else if (gameRules instanceof FogOfWarVariant) {
            // In Replay, just recalculate visibility without dialog
            calculateVisibility();
        }

        // --- DRAW OFFER LOGIC ---
        // After moving the Duck
        if (drawOfferPending && !(gameRules instanceof DuckChessVariant && move.type() != MoveType.DUCK)) {
            drawOfferPending = handleDrawLogicOffer();
        }

        // Reset Flag
        btnDraw.setText("Draw");
        btnDraw.setEnabled(true);

        clearSelections();
    }

    /**
     * Helper to open draw offer dialog whenever needed.
     * @return whether the draw request is still PENDING
     */
    private  boolean handleDrawLogicOffer() {
        // Check this AFTER switching turn (so it's the opponent's turn now)
        if (drawOfferPending && !isGameOver && !isBlindMode) {

            // Show Dialog
            PieceColor offerer = board.getCurrentPlayer().next(); // Previous player
            PieceColor responder = board.getCurrentPlayer();      // Current player

            // Use invokeLater so the board repaints (shows the move) before dialog
            SwingUtilities.invokeLater(() -> {
                int response = JOptionPane.showConfirmDialog(this,
                        offerer + " offers a draw.\nDo you accept?",
                        "Draw Offer", JOptionPane.YES_NO_OPTION);

                if (response == JOptionPane.YES_OPTION) {
                    isGameOver = true;

                    // Create Draw Move
                    Piece dummy = new King(offerer, -1, -1);
                    Move drawMove = new Move(dummy, -1, -1, -1, -1, MoveType.DRAW, null, false, null);

                    // Execute Draw (Recursive call, but safe because it hits Game Over logic immediately)
                    finalizeTurn(drawMove);

                    if (!(gameRules instanceof ChaturajiVariant)) {
                        // Standard / Duck / Fog / Crazy -> End game
                        isGameOver = true;

                        showGameOverDialog("DRAW!\n" + offerer + " offered a draw and " + responder + " accepted it!");
                    }
                }
            });
            return false;
        }
        return true;
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
     * Helper to print scores and winners in Chaturaji, and save the game.
     */
    private void printChaturajiStandings() {
        // Lock the game
        isGameOver = true;

        PieceColor[] players = {PieceColor.RED, PieceColor.BLUE, PieceColor.YELLOW, PieceColor.GREEN};

        int maxScore = -1;
        List<PieceColor> winners = new ArrayList<>();

        StringBuilder sb = new StringBuilder("GAME OVER!\n\nFinal Scores:\n");

        for (PieceColor player : players) {
            int score = board.getScore(player);

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
            winnerMsg.append("WINNER: ").append(winners.getFirst());
        } else {
            winnerMsg.append("WINNERS: ");
            for (int i = 0; i < winners.size(); i++) {
                winnerMsg.append(winners.get(i));
                if (i < winners.size() - 1) {
                    winnerMsg.append(", ");
                }
            }
        }

        sb.append("\n").append(winnerMsg).append("!");

        clearSelections();

        if (!isReplayMode) {
            // Get moves chronologically
            String resultString = winnerMsg.toString();
            List<Move> allMoves = new ArrayList<>(board.getMoveHistory());

            // Save Game
            GameSaver.saveGame(currentModeName, resultString, allMoves);
        }

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
                    Piece virtualPiece = getDropPieceFromType(myColor, type);

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
     * Starts the replay of given game.
     * @param record a chess match
     */
    public void startReplay(GameRecord record) {
        // Set the board up like a new game of that variant
        setupGame(record.variant(), null); // null clock

        // Set Replay State
        isReplayMode = true;
        replayMoves = record.moves();
        currentReplayIndex = 0;
        isGameOver = true; // Disable moving pieces

        // Reset Replay Perspective
        lockedReplayPerspective = null;
        switchPerspectiveBtn.setVisible(false); // Hidden by default, to update text on start-up
        updateReplayPerspective();

        // Request Focus for KeyListener
        setFocusable(true);
        requestFocusInWindow();

        repaint();
    }

    /**
     * Helper to step forward/backward in replay.
     * @param direction forward if greater than 0, backward otherwise
     */
    public void replayStep(int direction) {
        if (!isReplayMode) return;

        if (direction > 0) {
            // --- STEP FORWARD ---
            if (currentReplayIndex < replayMoves.size()) {
                SavedMove saved = replayMoves.get(currentReplayIndex);
                Move move = convertSavedMove(saved);

                if (move != null) {
                    finalizeTurn(move); // Execute + UI Updates
                    currentReplayIndex++;

                    // Correct the Rotation
                    updateReplayPerspective();

                    // Correct the Fog
                    if (gameRules instanceof FogOfWarVariant) {
                        calculateVisibility();
                    }

                    repaint();
                }
            }
        } else {
            // --- STEP BACKWARD ---
            if (currentReplayIndex > 0) {
                int targetIndex = currentReplayIndex - 1;

                // Reset Board to Start
                setupGame(currentModeName, null);

                // Restore Replay State
                isReplayMode = true;
                isGameOver = true;

                // FAST FORWARD (Silent)
                // Instead of actually stepping back, it's much more simple to start from the beginning
                // and step forward until reaching the desired move.
                for (int i = 0; i < targetIndex; i++) {
                    Move m = convertSavedMove(replayMoves.get(i));
                    if (m != null) {
                        executeMoveLogic(m); // Pure Logic, No UI
                    }
                }

                currentReplayIndex = targetIndex;

                // Update Visuals Once
                if (gameRules instanceof FogOfWarVariant) {
                    calculateVisibility();
                }

                // Correct the Rotation
                updateReplayPerspective();

                // Correct the Fog
                if (gameRules instanceof FogOfWarVariant) {
                    calculateVisibility();
                }

                repaint();
            }
        }
    }

    /**
     * Updates the perspective that we always see the board like the person who made the move.
     */
    private void updateReplayPerspective() {
        // If Locked, force that perspective
        if (lockedReplayPerspective != null) {
            this.viewPerspective = lockedReplayPerspective;
            return;
        }

        // Otherwise, use Dynamic Logic
        Move lastMove = board.getLastMove();

        // Start of Game (No moves yet) -> Default View
        if (lastMove == null) {
            this.viewPerspective = (gameRules instanceof ChaturajiVariant)
                    ? PieceColor.RED
                    : PieceColor.WHITE;
            return;
        }

        PieceColor pieceColor = lastMove.piece().getColor();

        // Standard Move -> View matches Piece Color
        if (pieceColor != PieceColor.SPECIAL) {
            this.viewPerspective = pieceColor;
        }
        // Duck Move (Special Color) -> View matches the player who just finished turn
        else {
            this.viewPerspective = board.getCurrentPlayer().next();
        }
    }

    /**
     * Helper to convert a saved move to a "real" one that can be executed.
     * @param saved saved move that is read from the JSON file of the match.
     * @return converted move that can be executed
     */
    private Move convertSavedMove(SavedMove saved) {
        MoveType type = MoveType.valueOf(saved.type());

        // --- RESIGN / DRAW ---
        if (type == MoveType.RESIGN || type == MoveType.DRAW) {
            PieceColor color = board.getCurrentPlayer();
            // Create dummy piece
            Piece dummy = new King(color, -1, -1);
            return new Move(dummy, -1, -1, -1, -1, type, null, false, null);
        }

        // --- TIMEOUT LOGIC (NEW) ---
        if (type == MoveType.TIMEOUT) {
            // We need to know WHO timed out (In replay, we rely on the board's current turn to assign the color)
            PieceColor color = board.getCurrentPlayer();
            Piece dummy = new King(color, -1, -1);

            return new Move(dummy, -1, -1, -1, -1, type, null, false, null);
        }

        int sr = saved.sr();
        int sc = saved.sc();
        int er = saved.er();
        int ec = saved.ec();

        Piece piece;
        if (type == MoveType.DROP) {
            // Determine Color
            PieceColor color = board.getCurrentPlayer();

            // Determine Type from Saved Data
            PieceType dropType;
            if (saved.dropPiece() != null) {
                dropType = PieceType.valueOf(saved.dropPiece());
            } else {
                // Fallback for old save files (assume Pawn)
                dropType = PieceType.PAWN;
            }

            // Create the correct piece
            piece = getDropPieceFromType(color, dropType);

            return Move.createDropMove(piece, er, ec);

        } else {
            // --- STANDARD MOVE LOGIC ---
            piece = board.getPiece(sr, sc);

            if (piece == null) return null;

            Piece target = board.getPiece(er, ec);
            Move move = new Move(piece, sr, sc, er, ec, type, target);

            if (saved.promo() != null) {
                PieceType pType = PieceType.valueOf(saved.promo());
                move = new Move(move, pType);
            }
            return move;
        }
    }

    /**
     * Helper to create a drop piece from given type and color.
     * @param color color of the piece
     * @param dropType type of the piece
     * @return new piece that can be dropped on the board
     */
    private Piece getDropPieceFromType(PieceColor color, PieceType dropType) {
        Piece piece;
        switch (dropType) {
            case ROOK -> piece = new Rook(color, -1, -1);
            case KNIGHT -> piece = new Knight(color, -1, -1);
            case BISHOP -> piece = new Bishop(color, -1, -1);
            case QUEEN -> piece = new Queen(color, -1, -1);
            default -> piece = new Pawn(color, -1, -1);
        }
        return piece;
    }

    /**
     * Executes purely the move without opening dialogs, switch turns checking game end conditions etc.
     * @param move move to be executed
     */
    private void executeMoveLogic(Move move) {
        // Update Points & Handle Player Death
        board.executeMove(move);

        // Chaturaji Logic
        if (gameRules instanceof ChaturajiVariant chaturajiRules) {
            // Update Points & Handle Player Death
            chaturajiRules.handlePostMoveLogic(board, move);

            board.switchTurn();
            viewPerspective = board.getCurrentPlayer();

            // Switch turn for Chess Clock
            if (chessClock != null && !isReplayMode && !isInGameReview) {
                chessClock.switchTurn(board.getCurrentPlayer());
            }

            return;
        }

        // Duck Chess Logic
        if (gameRules instanceof DuckChessVariant) {
            if (move.type() == MoveType.DUCK) {
                board.setWaitingForDuck(false);
                board.switchTurn();
                viewPerspective = board.getCurrentPlayer();

                // Switch turn for Chess Clock
                if (chessClock != null && !isReplayMode && !isInGameReview) {
                    chessClock.switchTurn(board.getCurrentPlayer());
                }
            } else {
                board.setWaitingForDuck(true);
            }
        }
        // Fog of War
        else if (gameRules instanceof FogOfWarVariant) {
            if (move.capturedPiece() == null || move.capturedPiece().getType() != PieceType.KING) {
                board.switchTurn();
                viewPerspective = board.getCurrentPlayer();

                // Switch turn for Chess Clock
                if (chessClock != null && !isReplayMode && !isInGameReview) {
                    chessClock.switchTurn(board.getCurrentPlayer());
                }
            }
        }
        // Classical / Crazyhouse
        else {
            board.switchTurn();
            viewPerspective = board.getCurrentPlayer();

            // Switch turn for Chess Clock
            if (chessClock != null && !isReplayMode && !isInGameReview) {
                chessClock.switchTurn(board.getCurrentPlayer());
            }
        }
    }


    // --- IN-GAME REVIEW LOGIC ---

    /**
     * Handles entering into in-game replay mode to review recent moves.
     */
    private void enterGameReview() {
        if (isInGameReview || board.getMoveHistory().isEmpty()) return;

        // Snapshot the Live State (We copy the stack to a list so we can iterate it)
        liveMoveHistoryBackup = new ArrayList<>(board.getMoveHistory());
        livePerspectiveSnapshot = viewPerspective; // Save current rotation
        liveCurrentPlayerSnapshot = board.getCurrentPlayer(); // save current real player

        isInGameReview = true;
        reviewIndex = liveMoveHistoryBackup.size(); // Start at the end
    }

    /**
     * Handles exiting from in-game replay mode.
     */
    private void exitGameReview() {
        if (!isInGameReview) return;

        // Restore Live State (board and replay ALL moves from the backup)
        board.resetBoard();

        // We need to ensure the base setup is correct
        restoreBaseSetup();

        for (Move m : liveMoveHistoryBackup) {
            executeMoveLogic(m);
        }

        // Restore Visuals
        viewPerspective = livePerspectiveSnapshot;

        // Clear Snapshot
        liveMoveHistoryBackup = null;
        isInGameReview = false;
        livePerspectiveSnapshot = null;
        liveCurrentPlayerSnapshot = null;

        // Update Fog for Live State
        if (gameRules instanceof FogOfWarVariant) {
            calculateVisibility();
        }
        repaint();
    }

    /**
     * Handles user's request of moving forward or backward in in-game replay.
     * @param direction forward (+1) or backward (-1)
     */
    private void stepInGameReview(int direction) {
        if (!isInGameReview) enterGameReview();

        int newIndex = reviewIndex + direction;

        // Bounds Check
        if (newIndex < 0) return; // Can't go before start
        if (newIndex > liveMoveHistoryBackup.size()) return;

        // If we go back to the very end, exit review mode (return to live game)
        if (newIndex == liveMoveHistoryBackup.size()) {
            exitGameReview();
            return;
        }

        reviewIndex = newIndex;

        // --- RECONSTRUCT BOARD AT reviewIndex ---
        board.resetBoard();
        restoreBaseSetup();

        for (int i = 0; i < reviewIndex; i++) {
            Move m = liveMoveHistoryBackup.get(i);
            executeMoveLogic(m);
        }

        // Restore perspective
        this.viewPerspective = livePerspectiveSnapshot;

        // Update Fog for the historic state
        if (gameRules instanceof FogOfWarVariant) {
            calculateVisibility();
        }

        repaint();
    }

    /**
     * Helper to apply the initial setup after a reset.
     */
    private void restoreBaseSetup() {
        if (gameRules instanceof ChaturajiVariant) {
            // Re-call the setup logic we have (setupChaturajiBoard() calls resetBoard() internally, so it's safe)
            setupChaturajiBoard();

            // Re-set the starting player to RED because resetBoard sets WHITE
            board.setCurrentPlayer(PieceColor.RED);
        } else {
            board.addStandardPieces();

            if (gameRules instanceof DuckChessVariant) {
                // Place duck on board (if history replayed moves the duck, it will be moved from default)
                board.placePiece(new Duck(3, 3), 3, 3);
            }
        }

        // Restore modes
        if (gameRules instanceof CrazyhouseVariant) {
            board.setCrazyhouseMode(true);
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
            requestFocusInWindow();// enable mouse

            // Block input if Game Over OR Reviewing the past
            if (isGameOver && !isInGameReview) return;

            // Block moves if reviewing history
            if (isInGameReview) return;

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
