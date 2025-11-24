package view.game;

import javax.swing.*;
import java.awt.*;

public class GamePanel extends JPanel {

    private final BoardPanel boardPanel;
    private final JLabel gameModeLabel;

    public GamePanel(Runnable onBack) {
        setLayout(new BorderLayout());
        setBackground(new Color(60, 60, 60));

        // --- TOP: Navigation & Title ---
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);
        // Add padding so the button isn't glued to the screen edge
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Define a fixed size for the navigation elements
        Dimension navSize = new Dimension(160, 40);

        // Create the menu button
        JButton backButton = new JButton("Back to Main Menu");
        backButton.setPreferredSize(navSize); // Set the fixed size
        backButton.setFocusPainted(false);
        backButton.addActionListener(e -> onBack.run());
        topPanel.add(backButton, BorderLayout.WEST);

        // Create the Top Center Label
        gameModeLabel = new JLabel("Classical Chess", SwingConstants.CENTER);
        gameModeLabel.setFont(new Font("Arial", Font.BOLD, 24));
        gameModeLabel.setForeground(Color.LIGHT_GRAY);
        gameModeLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        topPanel.add(gameModeLabel, BorderLayout.CENTER);

        // An invisible placeholder, which forces the BorderLayout.CENTER to be perfectly in the middle of the window.
        Component dummyRight = Box.createRigidArea(navSize);
        topPanel.add(dummyRight, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);

        // --- CENTER: The Chess Board ---
        boardPanel = new BoardPanel();
        add(boardPanel, BorderLayout.CENTER);
    }

    public void setGameModeText(String text) {
        gameModeLabel.setText(text);
    }

    public void setBoardTheme(BoardTheme theme) {
        boardPanel.setTheme(theme);
    }

    public void startNewGame(String modeName) {
        // Update Title text
        setGameModeText("Current Mode: " + modeName);

        // 2. Reset Board and Rules
        boardPanel.setupGame(modeName);
    }
}
