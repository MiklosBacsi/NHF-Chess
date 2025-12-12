import controller.GameController;
import model.GameVariant;

import javax.swing.*;

/**
 * This class represents and starts the program.
 * @see GameVariant
 * @see view.game.BoardPanel
 * @see GameVariant
 * @see view.MainFrame
 * @see view.game.GamePanel
 * @author Miklós Bácsi
 */
public class ChessVariantsApplication {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}

            // Create and start the application
            GameController controller = new GameController();
            controller.start();
        });
    }
}