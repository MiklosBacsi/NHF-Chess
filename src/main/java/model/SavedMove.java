package model;

// Standalone record (was previously inside GameSaver)

/**
 * This record represents a single move in a  chess match.
 * @param sr start row
 * @param sc start column
 * @param er end row
 * @param ec end column
 * @param type type of the piece moved
 * @param promo type of the piece that the pawn promoted to (null if no promotion was made)
 * @param dropPiece type of the piece that was dropped on the board (null if no drop was made)
 * @author Miklós Bácsi
 */
public record SavedMove(
        int sr,
        int sc,
        int er,
        int ec,
        String type,
        String promo,
        String dropPiece
) {}