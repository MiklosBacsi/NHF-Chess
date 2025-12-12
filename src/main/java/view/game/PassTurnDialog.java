package view.game;

import model.PieceColor;

import javax.swing.*;
import java.awt.*;

/**
 * This class is responsible for opening the window on which we can pass the turn to the next player.
 * @see BoardPanel
 * @see model.rules.FogOfWarVariant
 * @author Miklós Bácsi
 */
public class PassTurnDialog extends JDialog {

    /**
     * Constructor that creates window.
     * @param parent main frame (game window)
     * @param nextPlayer color of the next player
     */
    public PassTurnDialog(Frame parent, PieceColor nextPlayer) {
        super(parent, "Next Turn", true); // Modal = blocks input
        setUndecorated(true); // Removes window border/X button

        // Make it cover a good chunk of the screen or just center it
        setSize(400, 200);
        setLocationRelativeTo(parent);

        JPanel panel = new JPanel();
        panel.setBackground(new Color(30, 30, 30));
        panel.setLayout(new GridBagLayout());
        panel.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2));

        JLabel label = new JLabel("Pass device to " + nextPlayer);
        label.setFont(new Font("Arial", Font.BOLD, 24));
        label.setForeground(Color.WHITE);

        JButton btn = new JButton("I am ready");
        btn.setFont(new Font("Arial", Font.BOLD, 18));
        btn.setFocusPainted(false);
        btn.setBackground(new Color(100, 200, 100));
        btn.addActionListener(e -> dispose()); // Close dialog

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.insets = new Insets(20, 20, 20, 20);
        panel.add(label, gbc);

        gbc.gridy = 1;
        panel.add(btn, gbc);

        add(panel);
    }
}