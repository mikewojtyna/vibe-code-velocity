package pl.wojtyna.vibecodevelocity.bike;

public enum BikeStatus {
    /** Docked at a station and ready to be rented. */
    AVAILABLE,
    /** Out with a rider; not docked anywhere. */
    RENTED,
    /** Docked at a station but withdrawn from rental. */
    MAINTENANCE
}
