# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project state

City bike rental REST API. Riders rent a bike docked at a station and return it to any station that has a free dock; they pay per minute when they return it.

## Stack

- Java 25, Maven (use the bundled wrapper `./mvnw`)
- Spring Boot 4.1.2 (parent POM) — note Boot 4 uses modular starters, e.g. `spring-boot-starter-webmvc` (not `-web`), `spring-boot-h2console`, and dedicated test starters (`spring-boot-starter-webmvc-test`, `spring-boot-starter-data-jpa-test`)
- Spring Web MVC + Spring Data JPA, backed by an in-memory H2 database (runtime scope); H2 console module is included
- Config in `src/main/resources/application.properties`: pricing (`bike-rental.pricing.*`), `open-in-view=false`, H2 console at `/h2-console`

## Commands

```bash
./mvnw clean verify                     # build + run all tests
./mvnw test                             # run tests
./mvnw test -Dtest=ClassName            # single test class
./mvnw test -Dtest=ClassName#method     # single test method
./mvnw spring-boot:run                  # run the app
./mvnw package                          # build jar into target/
```

No linter or formatter is configured.

## Architecture

The code is organized by feature package under `pl.wojtyna.vibecodevelocity`: `station`, `bike`, `rider`, `rental`, plus `shared` for exceptions, error handling and the `Clock` bean. Each controller defines its request/response DTOs as nested records.

- **Domain rules live in the entities.** `Bike.rent()/dock()` and `Rental.finish()` enforce state transitions. The invariant: a bike has a `station` unless its status is `RENTED`. A rental is active while `endedAt` is null.
- **Errors:** throw `NotFoundException` (404) or `BusinessRuleViolationException` (409). `shared/ApiExceptionHandler` turns them into `ProblemDetail` responses. Bean validation failures return 400.
- **Concurrency guards:** `Bike` uses `@Version` so the same bike can't be rented twice. `RiderRepository.findByIdForUpdate` stops a rider from starting two active rentals. `StationService.lockWithFreeDock` stops a station from going over capacity, and it must be called inside the transaction that docks the bike.
- **Time** always comes from the injected `Clock`, never `Instant.now()`. Tests replace it with `MutableClock` (in test sources) to control ride durations.
- **Pricing** (`rental/PricingPolicy`): the unlock fee, plus `price-per-minute` for every *started* minute beyond `free-minutes`. Configured with the `PricingProperties` record.
- `open-in-view` is disabled, so map entities to DTOs while the associations are still loaded (they're all eager `@ManyToOne`).

## Testing

- `rental/BikeRentalApiTest`: an end-to-end test that runs the full context with MockMvc and `@Transactional` rollback. Add scenario tests here.
- In Boot 4, test annotations moved to modules, e.g. `org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc` and `org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest`.

## Notes

- The POM contains intentionally empty `<licenses>`, `<developers>`, `<scm>` elements to suppress inheritance from the Spring Boot parent (see `HELP.md`); remove them only if inheritance is actually wanted.
