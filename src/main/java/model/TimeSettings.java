package model;

/**
 * It represents the Chess Clock's properties.
 * Format: initialMinutes | incrementSeconds
 * @param initialMinutes initial minutes
 * @param incrementSeconds that many seconds are added to the remaining time after each move of the player
 * @see ChessClock
 * @see view.game.GameSetupDialog
 * @author Miklós Bácsi
 */
public record TimeSettings(int initialMinutes, int incrementSeconds) {

    /**
     * @return total initial time in milliseconds
     */
    public long getInitialMillis() {
        return initialMinutes * 60 * 1000L;
    }

    /**
     * @return incrementing time in milliseconds
     */
    public long getIncrementMillis() {
        return incrementSeconds * 1000L;
    }

    /**
     * Helper to print time settings.
     * @return formatted time settings
     */
    @Override
    public String toString() {
        if (incrementSeconds == 0) return initialMinutes + "";
        return initialMinutes + " | " + incrementSeconds;
    }
}