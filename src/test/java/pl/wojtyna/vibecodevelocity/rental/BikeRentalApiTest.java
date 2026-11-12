package pl.wojtyna.vibecodevelocity.rental;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import pl.wojtyna.vibecodevelocity.MutableClock;

import java.time.Duration;
import java.time.Instant;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "bike-rental.pricing.unlock-fee=2.00",
        "bike-rental.pricing.price-per-minute=0.50",
        "bike-rental.pricing.free-minutes=15"
})
@AutoConfigureMockMvc
@Transactional
class BikeRentalApiTest {

    @TestConfiguration
    static class ClockConfig {
        @Bean
        @Primary
        MutableClock testClock() {
            return new MutableClock(Instant.parse("2026-11-10T10:00:00Z"));
        }
    }

    @Autowired
    MockMvc mvc;

    @Autowired
    MutableClock clock;

    @Test
    void riderRentsBikeAndReturnsItToAnotherStation() throws Exception {
        long from = createStation("Central", 2);
        long to = createStation("Old Town", 2);
        long bike = registerBike("BK-001", from);
        long rider = registerRider("anna@example.com");

        long rental = id(postJson("/rentals", """
                {"riderId": %d, "bikeId": %d}""".formatted(rider, bike))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.startStationId").value(from)));

        mvc.perform(get("/bikes/{id}", bike))
                .andExpect(jsonPath("$.status").value("RENTED"))
                .andExpect(jsonPath("$.stationId").isEmpty());
        mvc.perform(get("/stations/{id}", from))
                .andExpect(jsonPath("$.availableBikes").value(0))
                .andExpect(jsonPath("$.freeDocks").value(2));

        clock.advance(Duration.ofMinutes(20).plusSeconds(10));

        postJson("/rentals/%d/return".formatted(rental), """
                {"stationId": %d}""".formatted(to))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false))
                .andExpect(jsonPath("$.endStationId").value(to))
                .andExpect(jsonPath("$.endedAt").value("2026-11-10T10:20:10Z"))
                .andExpect(jsonPath("$.cost").value(5.00)); // 2.00 + 6 started minutes * 0.50

        mvc.perform(get("/bikes/{id}", bike))
                .andExpect(jsonPath("$.status").value("AVAILABLE"))
                .andExpect(jsonPath("$.stationId").value(to));
        mvc.perform(get("/riders/{id}/rentals", rider))
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void bikeCannotBeRentedTwice() throws Exception {
        long station = createStation("Central", 5);
        long bike = registerBike("BK-001", station);
        long anna = registerRider("anna@example.com");
        long bob = registerRider("bob@example.com");
        startRental(anna, bike);

        postJson("/rentals", """
                {"riderId": %d, "bikeId": %d}""".formatted(bob, bike))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Bike %d is not available (status: RENTED)".formatted(bike)));
    }

    @Test
    void riderCannotHaveTwoActiveRentals() throws Exception {
        long station = createStation("Central", 5);
        long first = registerBike("BK-001", station);
        long second = registerBike("BK-002", station);
        long rider = registerRider("anna@example.com");
        startRental(rider, first);

        postJson("/rentals", """
                {"riderId": %d, "bikeId": %d}""".formatted(rider, second))
                .andExpect(status().isConflict());
    }

    @Test
    void bikeCannotBeReturnedToFullStation() throws Exception {
        long from = createStation("Central", 1);
        long full = createStation("Tiny", 1);
        long bike = registerBike("BK-001", from);
        registerBike("BK-002", full);
        long rental = startRental(registerRider("anna@example.com"), bike);

        postJson("/rentals/%d/return".formatted(rental), """
                {"stationId": %d}""".formatted(full))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Station %d has no free docks".formatted(full)));
    }

    @Test
    void rentalCannotBeReturnedTwice() throws Exception {
        long station = createStation("Central", 1);
        long rental = startRental(registerRider("anna@example.com"), registerBike("BK-001", station));
        postJson("/rentals/%d/return".formatted(rental), """
                {"stationId": %d}""".formatted(station)).andExpect(status().isOk());

        postJson("/rentals/%d/return".formatted(rental), """
                {"stationId": %d}""".formatted(station))
                .andExpect(status().isConflict());
    }

    @Test
    void bikeInMaintenanceCannotBeRented() throws Exception {
        long station = createStation("Central", 1);
        long bike = registerBike("BK-001", station);
        mvc.perform(post("/bikes/{id}/maintenance", bike))
                .andExpect(jsonPath("$.status").value("MAINTENANCE"));

        postJson("/rentals", """
                {"riderId": %d, "bikeId": %d}""".formatted(registerRider("anna@example.com"), bike))
                .andExpect(status().isConflict());
    }

    @Test
    void cannotRegisterBikeAtFullStation() throws Exception {
        long station = createStation("Central", 1);
        registerBike("BK-001", station);

        postJson("/bikes", """
                {"serialNumber": "BK-002", "stationId": %d}""".formatted(station))
                .andExpect(status().isConflict());
    }

    @Test
    void rejectsInvalidInput() throws Exception {
        postJson("/stations", """
                {"name": "", "capacity": 0}""")
                .andExpect(status().isBadRequest());
        postJson("/riders", """
                {"name": "Anna", "email": "not-an-email"}""")
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownResourcesReturnNotFound() throws Exception {
        mvc.perform(get("/rentals/{id}", 999_999)).andExpect(status().isNotFound());
        mvc.perform(get("/stations/{id}", 999_999)).andExpect(status().isNotFound());
    }

    private long createStation(String name, int capacity) throws Exception {
        return id(postJson("/stations", """
                {"name": "%s", "capacity": %d}""".formatted(name, capacity)).andExpect(status().isCreated()));
    }

    private long registerBike(String serialNumber, long stationId) throws Exception {
        return id(postJson("/bikes", """
                {"serialNumber": "%s", "stationId": %d}""".formatted(serialNumber, stationId))
                .andExpect(status().isCreated()));
    }

    private long registerRider(String email) throws Exception {
        return id(postJson("/riders", """
                {"name": "Test Rider", "email": "%s"}""".formatted(email)).andExpect(status().isCreated()));
    }

    private long startRental(long riderId, long bikeId) throws Exception {
        return id(postJson("/rentals", """
                {"riderId": %d, "bikeId": %d}""".formatted(riderId, bikeId)).andExpect(status().isCreated()));
    }

    private ResultActions postJson(String path, String json) throws Exception {
        return mvc.perform(post(path)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    private static long id(ResultActions result) throws Exception {
        return ((Number) JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id")).longValue();
    }
}
