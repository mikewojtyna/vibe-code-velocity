package pl.wojtyna.vibecodevelocity.rental;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.wojtyna.vibecodevelocity.bike.BikeService;
import pl.wojtyna.vibecodevelocity.rider.RiderRepository;
import pl.wojtyna.vibecodevelocity.shared.BusinessRuleViolationException;
import pl.wojtyna.vibecodevelocity.shared.NotFoundException;
import pl.wojtyna.vibecodevelocity.station.StationService;

import java.time.Clock;
import java.util.List;

@Service
@Transactional
public class RentalService {

    private final RentalRepository rentals;
    private final RiderRepository riders;
    private final BikeService bikeService;
    private final StationService stationService;
    private final PricingPolicy pricing;
    private final Clock clock;

    RentalService(RentalRepository rentals, RiderRepository riders, BikeService bikeService,
                  StationService stationService, PricingPolicy pricing, Clock clock) {
        this.rentals = rentals;
        this.riders = riders;
        this.bikeService = bikeService;
        this.stationService = stationService;
        this.pricing = pricing;
        this.clock = clock;
    }

    public Rental start(Long riderId, Long bikeId) {
        var rider = riders.findByIdForUpdate(riderId).orElseThrow(() -> new NotFoundException("Rider", riderId));
        if (rentals.existsByRiderIdAndEndedAtIsNull(riderId)) {
            throw new BusinessRuleViolationException("Rider %d already has an active rental".formatted(riderId));
        }
        var bike = bikeService.get(bikeId);
        var fromStation = bike.rent();
        return rentals.save(new Rental(rider, bike, fromStation, clock.instant()));
    }

    public Rental finish(Long rentalId, Long stationId) {
        var rental = get(rentalId);
        if (!rental.isActive()) {
            throw new BusinessRuleViolationException("Rental %d has already ended".formatted(rentalId));
        }
        var station = stationService.lockWithFreeDock(stationId);
        rental.finish(station, clock.instant(), pricing);
        return rental;
    }

    @Transactional(readOnly = true)
    public Rental get(Long id) {
        return rentals.findById(id).orElseThrow(() -> new NotFoundException("Rental", id));
    }

    @Transactional(readOnly = true)
    public List<Rental> historyOf(Long riderId) {
        if (!riders.existsById(riderId)) {
            throw new NotFoundException("Rider", riderId);
        }
        return rentals.findByRiderIdOrderByStartedAtDesc(riderId);
    }
}
