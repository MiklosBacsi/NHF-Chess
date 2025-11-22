package view.game;

import javax.swing.*;
import java.awt.*;

public class GamePanel extends JPanel {

    private JLabel gameModeLabel;
    private final Runnable onBack;

    public GamePanel(Runnable onBack) {
        this.onBack = onBack;
        setLayout(new BorderLayout());
        setBackground(new Color(25, 25, 40));

        JLabel title = new JLabel("CHESS GAME", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 48));
        title.setForeground(new Color(100, 200, 255));
        title.setBorder(BorderFactory.createEmptyBorder(40, 0, 20, 0));
        add(title, BorderLayout.NORTH);

        gameModeLabel = new JLabel("Current Mode: No mode selected", SwingConstants.CENTER);
        gameModeLabel.setFont(new Font("Arial", Font.BOLD, 32));
        gameModeLabel.setForeground(Color.WHITE);

        JPanel center = new JPanel(new GridBagLayout());
        center.setOpaque(false);
        center.add(gameModeLabel);
        add(center, BorderLayout.CENTER);

        add(createBackButton(), BorderLayout.SOUTH);
    }

    public void setGameModeText(String text) {
        gameModeLabel.setText(text);
    }

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
        wrapper.setBackground(new Color(25, 25, 40));
        wrapper.add(btn);
        return wrapper;
    }
}
