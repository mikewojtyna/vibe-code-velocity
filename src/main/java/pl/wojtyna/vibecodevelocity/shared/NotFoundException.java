package pl.wojtyna.vibecodevelocity.shared;

public class NotFoundException extends RuntimeException {

    public NotFoundException(String resource, Long id) {
        super("%s %d not found".formatted(resource, id));
    }
}
