package pl.wojtyna.vibecodevelocity.bike;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.wojtyna.vibecodevelocity.shared.BusinessRuleViolationException;
import pl.wojtyna.vibecodevelocity.shared.NotFoundException;
import pl.wojtyna.vibecodevelocity.station.StationService;

@Service
@Transactional
public class BikeService {

    private final BikeRepository bikes;
    private final StationService stationService;

    BikeService(BikeRepository bikes, StationService stationService) {
        this.bikes = bikes;
        this.stationService = stationService;
    }

    public Bike register(String serialNumber, Long stationId) {
        if (bikes.existsBySerialNumber(serialNumber)) {
            throw new BusinessRuleViolationException("Bike with serial number %s already exists".formatted(serialNumber));
        }
        var station = stationService.lockWithFreeDock(stationId);
        return bikes.save(new Bike(serialNumber, station));
    }

    @Transactional(readOnly = true)
    public Bike get(Long id) {
        return bikes.findById(id).orElseThrow(() -> new NotFoundException("Bike", id));
    }

    public Bike sendToMaintenance(Long id) {
        Bike bike = get(id);
        bike.sendToMaintenance();
        return bike;
    }

    public Bike returnFromMaintenance(Long id) {
        Bike bike = get(id);
        bike.returnFromMaintenance();
        return bike;
    }
}
