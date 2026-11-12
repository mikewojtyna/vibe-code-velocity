package pl.wojtyna.vibecodevelocity.station;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import pl.wojtyna.vibecodevelocity.bike.BikeController.BikeResponse;
import pl.wojtyna.vibecodevelocity.bike.BikeRepository;

import java.util.List;

@RestController
@RequestMapping("/stations")
class StationController {

    private final StationService stationService;
    private final BikeRepository bikes;

    StationController(StationService stationService, BikeRepository bikes) {
        this.stationService = stationService;
        this.bikes = bikes;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    StationResponse create(@Valid @RequestBody CreateStationRequest request) {
        return toResponse(stationService.create(request.name(), request.capacity()));
    }

    @GetMapping
    List<StationResponse> list() {
        return stationService.findAll().stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    StationResponse get(@PathVariable Long id) {
        return toResponse(stationService.get(id));
    }

    @GetMapping("/{id}/bikes")
    List<BikeResponse> bikesAt(@PathVariable Long id) {
        stationService.get(id);
        return bikes.findByStationId(id).stream().map(BikeResponse::from).toList();
    }

    private StationResponse toResponse(Station station) {
        var occupancy = stationService.occupancy(station);
        return new StationResponse(station.getId(), station.getName(), station.getCapacity(),
                occupancy.availableBikes(), occupancy.freeDocks());
    }

    record CreateStationRequest(@NotBlank String name, @Positive int capacity) {
    }

    record StationResponse(Long id, String name, int capacity, long availableBikes, long freeDocks) {
    }
}
