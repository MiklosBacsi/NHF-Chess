import controller.GameController;

import javax.swing.*;

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