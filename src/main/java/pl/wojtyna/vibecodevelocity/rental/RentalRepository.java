package pl.wojtyna.vibecodevelocity.rental;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RentalRepository extends JpaRepository<Rental, Long> {

    boolean existsByRiderIdAndEndedAtIsNull(Long riderId);

    List<Rental> findByRiderIdOrderByStartedAtDesc(Long riderId);
}
