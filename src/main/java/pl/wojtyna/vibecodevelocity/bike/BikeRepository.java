package pl.wojtyna.vibecodevelocity.bike;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface BikeRepository extends JpaRepository<Bike, Long> {

    List<Bike> findByStationId(Long stationId);

    /** Bikes occupying a dock at the station (available or in maintenance). */
    long countByStationId(Long stationId);

    @Query("select count(b) from Bike b where b.station.id = :stationId and b.status = BikeStatus.AVAILABLE")
    long countAvailableAt(Long stationId);

    boolean existsBySerialNumber(String serialNumber);
}
