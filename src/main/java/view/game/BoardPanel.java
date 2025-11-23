package view.game;

import javax.swing.*;
import java.awt.*;

public class BoardPanel extends JPanel {

    private BoardTheme currentTheme = BoardTheme.BROWN;

    public BoardPanel() {
        // Transparent so the dark grey background of GamePanel shows on the sides
        setOpaque(false);
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
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        // Calculate available space
        int w = getWidth();
        int h = getHeight();

        // Calculate Square Size dynamically, and take the smaller dimension so the board always fits inside the window
        int squareSize = Math.min(w, h) / 8;

        // Calculate centering (to keep board in middle)
        int boardPixelSize = squareSize * 8;
        int startX = (w - boardPixelSize) / 2;
        int startY = (h - boardPixelSize) / 2;

        // Draw the Board
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                int x = startX + (col * squareSize);
                int y = startY + (row * squareSize);

                // Determine color
                if ((row + col) % 2 == 0) {
                    g2d.setColor(currentTheme.light());
                } else {
                    g2d.setColor(currentTheme.dark());
                }

                // Draw the square
                g2d.fillRect(x, y, squareSize, squareSize);
            }
        }

        // (Optional) Draw a border around the whole board for a cleaner look
        g2d.setColor(Color.BLACK);
        g2d.setStroke(new BasicStroke(2));
        g2d.drawRect(startX, startY, boardPixelSize, boardPixelSize);
    }
}
