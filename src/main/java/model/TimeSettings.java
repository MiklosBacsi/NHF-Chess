package model;

/**
 * It represents the Chess Clock's properties.
 * Format: initialMinutes | incrementSeconds
 * @param initialMinutes
 * @param incrementSeconds
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