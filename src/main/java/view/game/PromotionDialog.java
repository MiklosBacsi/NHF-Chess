package view.game;

import model.PieceColor;
import model.PieceType;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.URL;

/**
 * This class is responsible for opening the window on which we can choose which piece to promote to.
 * @see BoardPanel
 * @author Miklós Bácsi
 */
public class PromotionDialog extends JDialog {

    private PieceType selectedType = PieceType.QUEEN; // Default

    /**
     * Constructor that creates window.
     * @param parent main frame (game window)
     * @param color color of the player
     */
    public PromotionDialog(Frame parent, PieceColor color) {
        super(parent, "Promote Pawn", true); // true = Modal (blocks game)
        setLayout(new GridLayout(1, 4, 10, 0)); // 1 row, 4 cols, gap
        setResizable(false);
        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE); // Force selection

        PieceType[] options = {PieceType.QUEEN, PieceType.ROOK, PieceType.KNIGHT, PieceType.BISHOP};

        for (PieceType type : options) {
            JButton btn = createButton(type, color);
            add(btn);
        }

        pack();
        setLocationRelativeTo(parent); // Center on game window
    }

    /**
     * Creates button for piece with picture.
     * @param type type of the piece
     * @param color color of the piece
     * @return created button of given type and color
     */
    private JButton createButton(PieceType type, PieceColor color) {
        JButton btn = new JButton();
        btn.setPreferredSize(new Dimension(160, 160)); // Slightly larger than 150x150 image
        btn.setFocusPainted(false);
        btn.setBackground(new Color(60, 60, 60)); // Dark background to match theme

        // Load Icon
        String filename = "/piece/" + color.name().toLowerCase() + "/" + type.name().toLowerCase() + ".png";
        try {
            URL url = getClass().getResource(filename);
            if (url != null) {
                Image img = ImageIO.read(url).getScaledInstance(150, 150, Image.SCALE_SMOOTH);
                btn.setIcon(new ImageIcon(img));
            }
        } catch (IOException e) {
            e.printStackTrace();
            btn.setText(type.name()); // Fallback text
        }

        btn.addActionListener(e -> {
            this.selectedType = type;
            dispose(); // Close dialog
        });

        return btn;
    }

    /**
     * @return selected piece we want to promote to
     */
    public PieceType getSelectedType() {
        return selectedType;
    }
}
