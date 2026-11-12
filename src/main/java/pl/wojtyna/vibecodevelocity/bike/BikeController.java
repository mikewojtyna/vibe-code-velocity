package pl.wojtyna.vibecodevelocity.bike;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/bikes")
public class BikeController {

    private final BikeService bikeService;

    BikeController(BikeService bikeService) {
        this.bikeService = bikeService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    BikeResponse register(@Valid @RequestBody RegisterBikeRequest request) {
        return BikeResponse.from(bikeService.register(request.serialNumber(), request.stationId()));
    }

    @GetMapping("/{id}")
    BikeResponse get(@PathVariable Long id) {
        return BikeResponse.from(bikeService.get(id));
    }

    @PostMapping("/{id}/maintenance")
    BikeResponse sendToMaintenance(@PathVariable Long id) {
        return BikeResponse.from(bikeService.sendToMaintenance(id));
    }

    @DeleteMapping("/{id}/maintenance")
    BikeResponse returnFromMaintenance(@PathVariable Long id) {
        return BikeResponse.from(bikeService.returnFromMaintenance(id));
    }

    record RegisterBikeRequest(@NotBlank String serialNumber, @NotNull Long stationId) {
    }

    public record BikeResponse(Long id, String serialNumber, BikeStatus status, Long stationId) {

        public static BikeResponse from(Bike bike) {
            Long stationId = bike.getStation() == null ? null : bike.getStation().getId();
            return new BikeResponse(bike.getId(), bike.getSerialNumber(), bike.getStatus(), stationId);
        }
    }
}
