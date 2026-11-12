package pl.wojtyna.vibecodevelocity.bike;

import jakarta.persistence.*;
import pl.wojtyna.vibecodevelocity.shared.BusinessRuleViolationException;
import pl.wojtyna.vibecodevelocity.station.Station;

/**
 * Invariant: a bike is docked at a station ({@code station != null}) unless it is {@link BikeStatus#RENTED}.
 */
@Entity
public class Bike {

    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false, unique = true)
    private String serialNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BikeStatus status;

    @ManyToOne
    private Station station;

    /** Optimistic lock: prevents two riders from renting the same bike concurrently. */
    @Version
    private long version;

    protected Bike() {
    }

    public Bike(String serialNumber, Station station) {
        this.serialNumber = serialNumber;
        this.station = station;
        this.status = BikeStatus.AVAILABLE;
    }

    /** Undocks the bike and returns the station it was taken from. */
    public Station rent() {
        if (status != BikeStatus.AVAILABLE) {
            throw new BusinessRuleViolationException("Bike %d is not available (status: %s)".formatted(id, status));
        }
        Station from = station;
        station = null;
        status = BikeStatus.RENTED;
        return from;
    }

    public void dock(Station station) {
        if (status != BikeStatus.RENTED) {
            throw new BusinessRuleViolationException("Bike %d is not rented".formatted(id));
        }
        this.station = station;
        this.status = BikeStatus.AVAILABLE;
    }

    public void sendToMaintenance() {
        if (status != BikeStatus.AVAILABLE) {
            throw new BusinessRuleViolationException("Only an available bike can be sent to maintenance");
        }
        status = BikeStatus.MAINTENANCE;
    }

    public void returnFromMaintenance() {
        if (status != BikeStatus.MAINTENANCE) {
            throw new BusinessRuleViolationException("Bike %d is not in maintenance".formatted(id));
        }
        status = BikeStatus.AVAILABLE;
    }

    public Long getId() {
        return id;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public BikeStatus getStatus() {
        return status;
    }

    public Station getStation() {
        return station;
    }
}
