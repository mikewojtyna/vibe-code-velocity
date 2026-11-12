package pl.wojtyna.vibecodevelocity.rider;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import pl.wojtyna.vibecodevelocity.shared.BusinessRuleViolationException;
import pl.wojtyna.vibecodevelocity.shared.NotFoundException;

@RestController
@RequestMapping("/riders")
class RiderController {

    private final RiderRepository riders;

    RiderController(RiderRepository riders) {
        this.riders = riders;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    RiderResponse register(@Valid @RequestBody RegisterRiderRequest request) {
        if (riders.existsByEmail(request.email())) {
            throw new BusinessRuleViolationException("Rider with email %s already exists".formatted(request.email()));
        }
        return RiderResponse.from(riders.save(new Rider(request.name(), request.email())));
    }

    @GetMapping("/{id}")
    RiderResponse get(@PathVariable Long id) {
        return riders.findById(id).map(RiderResponse::from).orElseThrow(() -> new NotFoundException("Rider", id));
    }

    record RegisterRiderRequest(@NotBlank String name, @NotBlank @Email String email) {
    }

    record RiderResponse(Long id, String name, String email) {

        static RiderResponse from(Rider rider) {
            return new RiderResponse(rider.getId(), rider.getName(), rider.getEmail());
        }
    }
}
