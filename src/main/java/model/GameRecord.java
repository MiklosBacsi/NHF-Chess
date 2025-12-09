package model;

import java.time.LocalDateTime;
import java.util.List;

/**
 * This record represents a single chess match.
 * @param filename name of the JSON file in which the game is stored
 * @param variant type of the chess variant
 * @param date time stamp when the game was finished
 * @param result outcome of the game
 * @param moves list of the moves that make up the game
 * @author Miklós Bácsi
 */
public record GameRecord(
        String filename,
        String variant,
        LocalDateTime date,
        String result,
        List<SavedMove> moves
) {}
