package pl.wojtyna.vibecodevelocity.station;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.wojtyna.vibecodevelocity.bike.BikeRepository;
import pl.wojtyna.vibecodevelocity.shared.BusinessRuleViolationException;
import pl.wojtyna.vibecodevelocity.shared.NotFoundException;

import java.util.List;

@Service
@Transactional
public class StationService {

    private final StationRepository stations;
    private final BikeRepository bikes;

    StationService(StationRepository stations, BikeRepository bikes) {
        this.stations = stations;
        this.bikes = bikes;
    }

    public Station create(String name, int capacity) {
        return stations.save(new Station(name, capacity));
    }

    @Transactional(readOnly = true)
    public Station get(Long id) {
        return stations.findById(id).orElseThrow(() -> new NotFoundException("Station", id));
    }

    @Transactional(readOnly = true)
    public List<Station> findAll() {
        return stations.findAll();
    }

    /**
     * Locks the station and verifies it has a free dock. Must be called inside the transaction
     * that docks the bike, so the lock is held until the bike is saved.
     */
    public Station lockWithFreeDock(Long stationId) {
        Station station = stations.findByIdForUpdate(stationId)
                .orElseThrow(() -> new NotFoundException("Station", stationId));
        if (bikes.countByStationId(stationId) >= station.getCapacity()) {
            throw new BusinessRuleViolationException("Station %d has no free docks".formatted(stationId));
        }
        return station;
    }

    @Transactional(readOnly = true)
    public StationOccupancy occupancy(Station station) {
        long docked = bikes.countByStationId(station.getId());
        long available = bikes.countAvailableAt(station.getId());
        return new StationOccupancy(available, station.getCapacity() - docked);
    }

    public record StationOccupancy(long availableBikes, long freeDocks) {
    }
}
