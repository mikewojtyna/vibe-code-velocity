package pl.wojtyna.vibecodevelocity.rental;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@RestController
class RentalController {

    private final RentalService rentalService;

    RentalController(RentalService rentalService) {
        this.rentalService = rentalService;
    }

    @PostMapping("/rentals")
    @ResponseStatus(HttpStatus.CREATED)
    RentalResponse start(@Valid @RequestBody StartRentalRequest request) {
        return RentalResponse.from(rentalService.start(request.riderId(), request.bikeId()));
    }

    @PostMapping("/rentals/{id}/return")
    RentalResponse finish(@PathVariable Long id, @Valid @RequestBody ReturnBikeRequest request) {
        return RentalResponse.from(rentalService.finish(id, request.stationId()));
    }

    @GetMapping("/rentals/{id}")
    RentalResponse get(@PathVariable Long id) {
        return RentalResponse.from(rentalService.get(id));
    }

    @GetMapping("/riders/{riderId}/rentals")
    List<RentalResponse> historyOf(@PathVariable Long riderId) {
        return rentalService.historyOf(riderId).stream().map(RentalResponse::from).toList();
    }

    record StartRentalRequest(@NotNull Long riderId, @NotNull Long bikeId) {
    }

    record ReturnBikeRequest(@NotNull Long stationId) {
    }

    record RentalResponse(Long id, Long riderId, Long bikeId, Long startStationId, Long endStationId,
                          Instant startedAt, Instant endedAt, BigDecimal cost, boolean active) {

        static RentalResponse from(Rental rental) {
            Long endStationId = rental.getEndStation() == null ? null : rental.getEndStation().getId();
            return new RentalResponse(rental.getId(), rental.getRider().getId(), rental.getBike().getId(),
                    rental.getStartStation().getId(), endStationId, rental.getStartedAt(), rental.getEndedAt(),
                    rental.getCost(), rental.isActive());
        }
    }
}
