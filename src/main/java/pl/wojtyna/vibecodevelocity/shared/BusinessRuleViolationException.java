package pl.wojtyna.vibecodevelocity.shared;

/**
 * Thrown when a request is well-formed but conflicts with the current state of the system
 * (e.g. renting a bike that is already rented). Mapped to HTTP 409.
 */
public class BusinessRuleViolationException extends RuntimeException {

    public BusinessRuleViolationException(String message) {
        super(message);
    }
}
