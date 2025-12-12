package model;

/**
 * This enum represents the type of moves possible.
 * @see Move
 * @see GameVariant
 * @author Miklós Bácsi
 */
public enum MoveType {
    NORMAL,
    CASTLING,
    EN_PASSANT,
    PROMOTION,
    DUCK,
    DROP,
    TIMEOUT,
    RESIGN,
    DRAW
}
