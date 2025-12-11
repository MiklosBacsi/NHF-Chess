package view.game;

import model.GameRecord;
import model.TimeSettings;

import javax.swing.*;
import java.awt.*;

/**
 * This class is responsible the game panel of the program.
 * @author Miklós Bácsi
 */
public class GamePanel extends JPanel {

    private final BoardPanel boardPanel;
    private final JLabel gameModeLabel;

    // --- NAVIGATION FIELDS ---
    private final JButton backButton;           // The button component
    private final Runnable defaultBackAction;   // Takes us to Main Menu
    private Runnable currentBackAction;         // The logic currently assigned to the button

    /**
     * Constructor that creates the board and other items.
     * @param onBackToMenu back to the main menu button's action
     */
    public GamePanel(Runnable onBackToMenu) {
        this.defaultBackAction = onBackToMenu;
        this.currentBackAction = onBackToMenu; // Default state

        setLayout(new BorderLayout());
        setBackground(new Color(60, 60, 60));

        // --- TOP: Navigation & Title ---
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);
        // Add padding so the button isn't glued to the screen edge
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));

        // Define a fixed size for the navigation elements
        Dimension navSize = new Dimension(170, 40);

        // Create the menu button
        backButton = new JButton("Back to Main Menu");
        backButton.setPreferredSize(navSize); // Set the fixed size
        backButton.setFocusPainted(false);

        // Dynamic Listener
        backButton.addActionListener(e -> {
            // Stop the game logic first
            stopGame();

            // Run whatever action is currently assigned
            if (currentBackAction != null) {
                currentBackAction.run();
            }
        });

        topPanel.add(backButton, BorderLayout.WEST);

        // Create the Top Center Label
        gameModeLabel = new JLabel("Classical Chess", SwingConstants.CENTER);
        gameModeLabel.setFont(new Font("Arial", Font.BOLD, 24));
        gameModeLabel.setForeground(Color.LIGHT_GRAY);
        gameModeLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        topPanel.add(gameModeLabel, BorderLayout.CENTER);

        // An invisible placeholder, which forces the BorderLayout.CENTER to be perfectly in the middle of the window.
        Component dummyRight = Box.createRigidArea(navSize);
        topPanel.add(dummyRight, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);

        // --- CENTER: The Chess Board ---
        boardPanel = new BoardPanel();
        add(boardPanel, BorderLayout.CENTER);
    }

    /**
     * Sets the game mode label to the given text.
     * @param text set game mode label to this
     */
    public void setGameModeText(String text) {
        gameModeLabel.setText(text);
    }

    /**
     * Sets the theme of the board to the given one
     * @param theme set theme of the board to this
     */
    public void setBoardTheme(BoardTheme theme) {
        boardPanel.setTheme(theme);
    }

    /**
     * Starts a new game with the chosen variant
     * @param modeName name of the chosen variant
     */
    public void startNewGame(String modeName, TimeSettings  timeSettings) {
        // Update Title text
        setGameModeText("Current Mode: " + modeName);

        // Reset Action
        this.currentBackAction = defaultBackAction;

        // Reset Visuals
        if (backButton != null) {
            backButton.setText("Back to Main Menu");
            backButton.setBackground(null); // Reset color
        }

        // Reset Board and Rules
        boardPanel.setupGame(modeName, timeSettings, true);
    }

    /**
     * Stops chess clock.
     */
    public void stopGame() {
        boardPanel.stopGame();
    }

    /**
     * Start the replay of a match.
     * @param record chess match
     */
    public void startReplay(GameRecord record) {
        // Update the top label to show we are in Replay mode
        setGameModeText("Replay: " + record.variant());

        // Delegate logic to the board
        boardPanel.startReplay(record);
    }

    /**
     * Starts the replay of the chosen saved game from the Game History.
     * @param record a saved chess match
     * @param onBackToHistory action that takes us back to the Game History panel
     */
    public void startReplay(GameRecord record, Runnable onBackToHistory) {
        setGameModeText("Replay: " + record.variant());

        // Update Action
        this.currentBackAction = onBackToHistory;

        // Update Visuals
        if (backButton != null) {
            backButton.setText("Back to Game History");
        }

        boardPanel.startReplay(record);
    }
}
