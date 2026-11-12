package pl.wojtyna.vibecodevelocity.rider;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface RiderRepository extends JpaRepository<Rider, Long> {

    boolean existsByEmail(String email);

    /** Locks the rider row so concurrent requests can't start two rentals for the same rider. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Rider r where r.id = :id")
    Optional<Rider> findByIdForUpdate(Long id);
}
