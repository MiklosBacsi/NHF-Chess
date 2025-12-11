package view.history;

import model.GameSaver;
import model.GameRecord;

import javax.swing.*;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;

/**
 * This class is responsible for controlling the program
 * @author Miklós Bácsi
 */
public class HistoryPanel extends JPanel {

    private final Consumer<GameRecord> onReplay;
    private final DefaultListModel<GameRecord> listModel;
    private final JList<GameRecord> gameList;

    /**
     * Constructor that initializes the panel with its content.
     * @param onBack go back to the menu
     * @param onReplay replay match
     */
    public HistoryPanel(Runnable onBack, Consumer<GameRecord> onReplay) {
        this.onReplay = onReplay;

        Color darkBackground = new Color(40, 40, 40);

        setLayout(new BorderLayout());
        setBackground(darkBackground);

        // HEADER
        JLabel title = new JLabel("GAME HISTORY", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 40));
        title.setForeground(Color.WHITE);
        title.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));
        add(title, BorderLayout.NORTH);

        // LIST
        listModel = new DefaultListModel<>();
        gameList = new JList<>(listModel);
        gameList.setBackground(darkBackground);
        gameList.setForeground(Color.WHITE);
        gameList.setFont(new Font("Monospaced", Font.PLAIN, 16));
        gameList.setCellRenderer(new GameListRenderer());

        JScrollPane scroll = new JScrollPane(gameList);
        scroll.setBorder(BorderFactory.createEmptyBorder(10, 50, 10, 50));
        scroll.getViewport().setBackground(darkBackground);
        scroll.setBackground(new Color(38, 38, 38));
        add(scroll, BorderLayout.CENTER);

        // BUTTONS
        JPanel btnPanel = new JPanel(new FlowLayout());
        btnPanel.setOpaque(false);

        JButton backBtn = createButton("Back");
        backBtn.addActionListener(e -> onBack.run());

        JButton deleteBtn = createButton("Delete Selected");
        deleteBtn.addActionListener(e -> deleteSelected());

        JButton replayBtn = createButton("Replay Game");
        replayBtn.addActionListener(e -> replaySelected());

        btnPanel.add(backBtn);
        btnPanel.add(deleteBtn);
        btnPanel.add(replayBtn);
        add(btnPanel, BorderLayout.SOUTH);
    }

    /**
     * Refreshes the list of saved chess matches.
     */
    public void refresh() {
        listModel.clear();
        List<GameRecord> records = GameSaver.loadAllGames();
        for (GameRecord r : records) listModel.addElement(r);
    }

    /**
     * Deletes selected saved game.
     */
    private void deleteSelected() {
        GameRecord selected = gameList.getSelectedValue();
        if (selected != null) {
            GameSaver.deleteGame(selected.filename());
            refresh();
        }
    }

    /**
     * Replays selected game.
     */
    private void replaySelected() {
        GameRecord selected = gameList.getSelectedValue();
        if (selected != null) {
            onReplay.accept(selected);
        }
    }

    /**
     * Helper to create buttons (used at the bottom of the panel).
     * @param text text on the button
     * @return new button created with given text
     */
    private JButton createButton(String text) {
        JButton btn = new JButton(text);
        btn.setPreferredSize(new Dimension(150, 40));
        btn.setFocusPainted(false);
        return btn;
    }

    /**
     * Custom Renderer to make the list look pretty.
     */
    private static class GameListRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof GameRecord record) {
                String date = record.date().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                setText(String.format(" %-15s | %-20s | %s", record.variant(), date, record.result()));
            }
            if (isSelected) setBackground(new Color(211, 73, 0));
            else setBackground(index % 2 == 0 ? new Color(50, 50, 50) : new Color(60, 60, 60));
            return this;
        }
    }
}
