package model;

import java.util.HashMap;
import java.util.Map;

/**
 * This class implements the Chess Clock.
 * @author Miklós Bácsi
 */
public class ChessClock {
    private final Map<PieceColor, Long> remainingTime = new HashMap<>();
    private final long incrementMillis;

    private PieceColor activePlayer;
    private long lastTickTime;
    private boolean isRunning = false;

    /**
     * Constructor for Chess Clock.
     * @param settings properties of the clock (initial time and incrementing seconds)
     */
    public ChessClock(TimeSettings settings) {
        this.incrementMillis = settings.getIncrementMillis();
        long startMillis = settings.getInitialMillis();

        // Initialize for all potential colors
        for (PieceColor player : PieceColor.values()) {
            remainingTime.put(player, startMillis);
        }
    }

    /**
     * Starts (resumes) the clock of the player.
     * @param player color of the player
     */
    public void start(PieceColor player) {
        this.activePlayer = player;
        this.lastTickTime = System.currentTimeMillis();
        this.isRunning = true;
    }

    /**
     * Stops (pauses) the Chess Clock.
     */
    public void stop() {
        this.isRunning = false;
    }

    /**
     * Handle chess clock when switching turn.
     * @param nextPlayer color of the next player
     */
    public void switchTurn(PieceColor nextPlayer) {
        // Add increment to the player who just finished
        if (activePlayer != null) {
            long current = remainingTime.get(activePlayer);
            remainingTime.put(activePlayer, current + incrementMillis);
        }

        // Switch
        start(nextPlayer);
    }

    /**
     * Called by the Game Loop (Timer) to deduct time.
     */
    public void tick() {
        if (!isRunning || activePlayer == null) return;

        long now = System.currentTimeMillis();
        long delta = now - lastTickTime;

        long current = remainingTime.get(activePlayer);
        remainingTime.put(activePlayer, current - delta);

        lastTickTime = now;
    }

    /**
     * @param color color of the player
     * @return remaining time of the player
     */
    public long getTime(PieceColor color) {
        return remainingTime.getOrDefault(color, 0L);
    }

    /**
     * @param color color of the player
     * @return if the player has run out of time
     */
    public boolean isFlagFallen(PieceColor color) {
        return getTime(color) <= 0;
    }
}
