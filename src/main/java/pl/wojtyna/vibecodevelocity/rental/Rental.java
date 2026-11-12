package pl.wojtyna.vibecodevelocity.rental;

import jakarta.persistence.*;
import pl.wojtyna.vibecodevelocity.bike.Bike;
import pl.wojtyna.vibecodevelocity.rider.Rider;
import pl.wojtyna.vibecodevelocity.shared.BusinessRuleViolationException;
import pl.wojtyna.vibecodevelocity.station.Station;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

/** A rental is active while {@code endedAt} is null. */
@Entity
public class Rental {

    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne(optional = false)
    private Rider rider;

    @ManyToOne(optional = false)
    private Bike bike;

    @ManyToOne(optional = false)
    private Station startStation;

    @ManyToOne
    private Station endStation;

    @Column(nullable = false)
    private Instant startedAt;

    private Instant endedAt;

    @Column(precision = 10, scale = 2)
    private BigDecimal cost;

    protected Rental() {
    }

    Rental(Rider rider, Bike bike, Station startStation, Instant startedAt) {
        this.rider = rider;
        this.bike = bike;
        this.startStation = startStation;
        this.startedAt = startedAt;
    }

    void finish(Station endStation, Instant endedAt, PricingPolicy pricing) {
        if (!isActive()) {
            throw new BusinessRuleViolationException("Rental %d has already ended".formatted(id));
        }
        bike.dock(endStation);
        this.endStation = endStation;
        this.endedAt = endedAt;
        this.cost = pricing.costOf(Duration.between(startedAt, endedAt));
    }

    public boolean isActive() {
        return endedAt == null;
    }

    public Long getId() {
        return id;
    }

    public Rider getRider() {
        return rider;
    }

    public Bike getBike() {
        return bike;
    }

    public Station getStartStation() {
        return startStation;
    }

    public Station getEndStation() {
        return endStation;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getEndedAt() {
        return endedAt;
    }

    public BigDecimal getCost() {
        return cost;
    }
}
