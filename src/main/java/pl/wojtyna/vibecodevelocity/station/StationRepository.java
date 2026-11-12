package pl.wojtyna.vibecodevelocity.station;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface StationRepository extends JpaRepository<Station, Long> {

    /** Locks the station row so concurrent docking can't exceed its capacity. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Station s where s.id = :id")
    Optional<Station> findByIdForUpdate(Long id);
}
