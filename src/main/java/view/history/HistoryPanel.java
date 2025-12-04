package view.history;

import javax.swing.*;
import java.awt.*;

/**
 * This class is responsible for controlling the program
 * @author Miklós Bácsi
 */
public class HistoryPanel extends JPanel {

    private final Runnable onBack;

    /**
     * Constructor that creates the game history and other items.
     * @param onBack back to the main menu button's action
     */
    public HistoryPanel(Runnable onBack) {
        this.onBack = onBack;
        setLayout(new BorderLayout());
        setBackground(new Color(32, 64, 32));

        JLabel title = new JLabel("GAME HISTORY", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 50));
        title.setForeground(new Color(128, 255, 133));
        title.setBorder(BorderFactory.createEmptyBorder(60, 0, 40, 0));
        add(title, BorderLayout.NORTH);

        JTextArea text = new JTextArea("No games played yet.\n\nYour completed games will appear here.");
        text.setEditable(false);
        text.setFont(new Font("Consolas", Font.PLAIN, 20));
        text.setForeground(Color.WHITE);
        text.setBackground(getBackground());
        text.setBorder(BorderFactory.createEmptyBorder(20, 100, 100, 100));
        add(text, BorderLayout.CENTER);

        add(createBackButton(), BorderLayout.SOUTH);
    }

    /**
     * Creates a back to the main menu button.
     * @return JComponent of button's panel
     */
    private JComponent createBackButton() {
        JButton btn = new JButton("Back to Main Menu");
        btn.setFont(new Font("Arial", Font.BOLD, 20));
        btn.setForeground(Color.WHITE);
        btn.setBackground(new Color(100, 100, 150));
        btn.setPreferredSize(new Dimension(300, 60));
        btn.setFocusPainted(false);
        btn.addActionListener(e -> onBack.run());

        JPanel wrapper = new JPanel(new FlowLayout());
        wrapper.setBorder(BorderFactory.createEmptyBorder(20, 0, 30, 0));
        wrapper.setBackground(new Color(25, 25, 40)); // Matches GamePanel style for wrapper
        wrapper.add(btn);
        return wrapper;
    }
}
