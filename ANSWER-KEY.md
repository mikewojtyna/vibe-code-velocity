# Exercise 1: answer key (trainer)

## How `ex01/start` was produced

1. An empty Spring Boot 4.1 project from Spring Initializr (Web MVC, Data JPA, H2, Validation).
2. `/init` in a fresh Claude Code session, before any code existed. The session had no DDD guidance and no skills.
3. One prompt: **"Create city bike rental system"**.
4. The result, committed unedited as `8b026b2`:
   - 1 776 lines in one commit;
   - 15 green tests;
   - a rewritten `CLAUDE.md` that describes the code.

## Why it looks fine

It's tidy, idiomatic Spring:
- feature packages;
- constructor injection;
- records for DTOs;
- `ProblemDetail` errors;
- an injected `Clock`;
- configurable pricing;
- `@Version` and pessimistic locks;
- an end-to-end test suite.

A quick review would approve it. The problems are in what it **decided**, what it **can't express** and what its
tests **don't check**.

Paths are relative to `src/main/java/pl/wojtyna/vibecodevelocity/` unless they start with `src/`, `CLAUDE.md` or
`application.properties` (in `src/main/resources/`). Line numbers refer to `ex01/start`. The behaviours marked ✓ were
checked with a throwaway test that isn't committed.

## A. Business decisions nobody approved

The prompt contained none of these. The AI picked them, and the tests then assert its own picks.

| # | Decision | Where | Question for the product owner |
|---|---|---|---|
| A1 | **Prices:** a 2.00 unlock fee, 0.50 per minute, 15 free minutes. **No currency** anywhere: money is a bare `BigDecimal` with scale 2. | `application.properties:5-7`, `rental/RentalController.java:48-49`, `rental/Rental.java:38-39` | What are the prices, in which currency, with or without VAT? Is there a subscription, a day pass, a student tariff? |
| A2 | **Rounding:** every *started* minute is billed, and the 15 free minutes are inclusive (15:00 is free, 15:01 costs 0.50). | `rental/PricingPolicy.java:18-25` | Per started minute, per second, or per 5-minute block? Are the free minutes a promotion or the rule? |
| A3 | The **unlock fee is charged even for a 0-second rental**, e.g. a rider takes a broken bike out and docks it straight back ✓ (cost 2.00). | `rental/PricingPolicy.java:19-25` | Does a rider pay for a bike that turned out to be broken? Is there a grace period to swap bikes? |
| A4 | The **price is computed at return with the configuration of that moment**. Nothing is fixed when the rental starts, so a price change deployed mid-ride changes what riders already riding pay. | `rental/Rental.java:58`, `rental/RentalService.java:51` | Which price applies: the one at the start, or at the end? Must the rider see the price before unlocking? |
| A5 | **The rider ends the rental with an API call that names any station.** Nothing checks that the bike is physically in that station's dock. A rider can "return" the bike from home and stop the meter. The bike then shows as available at a station where it isn't ✓. | `rental/RentalController.java:27-30`, `rental/RentalService.java:45-53` | Who ends a rental: the dock (lock sensor), the app, or both? What if they disagree? |
| A6 | **A full station rejects the return** with 409. The rental stays active ✓, the meter keeps running, and the rider gets no grace time, no alternative station and no way to leave the bike. | `station/StationService.java:41-48`, `rental/RentalService.java:50` | What does a rider do at a full station? Extra free minutes? Lock the bike next to the station? |
| A7 | **A bike in maintenance stays docked and occupies a dock.** It counts against capacity, and returns are rejected because of it ✓. It is listed to riders under `GET /stations/{id}/bikes` ✓. | `bike/BikeStatus.java:8-9`, `bike/BikeRepository.java:12-13`, `station/StationController.java:41-45` | Does maintenance happen at the station or in a workshop? Should riders see broken bikes? |
| A8 | **Only an AVAILABLE bike can go to maintenance.** A bike that breaks mid-ride can't be reported ✓, and a rented bike can't be withdrawn. | `bike/Bike.java:59-64` | How does a rider report a fault? What happens to their rental and their charge? |
| A9 | A **bike must be registered straight into a dock**. Registration at a full station fails, and new bikes can't wait in a depot. | `bike/BikeService.java:21-27` | Where are bikes before they enter service: a depot, a van, a station? |
| A10 | **One active rental per rider.** | `rental/RentalService.java:37-39` | Can a parent rent for a child? Can a group rent several bikes? |
| A11 | **No limit on rental length.** A bike that's never returned stays `RENTED` forever, and the rider is never charged, because the cost is computed only on return. | `rental/Rental.java:51-59` | After how long is a bike considered lost or stolen? Who pays? |
| A12 | **A rider is a name and an e-mail.** There's no payment method, verification, age check, terms acceptance or blocking. E-mail uniqueness is **case-sensitive**: `anna@example.com` and `Anna@Example.com` are two riders ✓. | `rider/Rider.java:12-16`, `rider/RiderController.java:26` | What does a rider need before the first rental? Can a rider with unpaid rentals rent again? |
| A13 | **A station is a name and a capacity.** It has no location, the capacity can never change after creation, and names needn't be unique. | `station/Station.java:12-25` | Do stations move, grow, close temporarily? How does the app show "nearest station"? |
| A14 | The **cost is `null` while the rental is active**. The rider can't see how much the ride costs so far. | `rental/RentalController.java:48-55` | Should the app show a running price? Is it binding? |

Look also at what `CLAUDE.md:7` states as fact: "return it to any station that has a free dock; they pay per
minute when they return it". Nobody said so. The AI wrote its own assumptions down as the project's description, and
every later session will read them as requirements.

## B. Bugs and inconsistencies the green build doesn't catch

| # | Problem | Where | Why it matters |
|---|---|---|---|
| B1 | **Check-then-insert races.** Two concurrent registrations with the same e-mail or serial number both pass the `exists…` check. The unique constraint then throws `DataIntegrityViolationException`, which no handler maps, so the client gets a **500** ✓ (race simulated by stubbing the check). | `rider/RiderController.java:25-30`, `bike/BikeService.java:21-27`, `shared/ApiExceptionHandler.java:10-28` | A double tap on "Sign up" ends in a server error instead of a 409. |
| B2 | **A missing `free-minutes` silently becomes 0**, because it's a primitive `int` with no `@NotNull`. The other two properties fail fast ✓. | `rental/PricingProperties.java:17-19` | A typo in the config raises every price without any error at startup. |
| B3 | **Sub-seconds are truncated before rounding up.** `toSeconds()` drops the millis, so 15:00.5 costs 2.00 ✓, though the 16th minute has started. | `rental/PricingPolicy.java:20` | It contradicts its own "every started minute" Javadoc (`:18`). Small, but it's a money rule. |
| B4 | **The "already ended" check is duplicated** in the service and in the entity. The rental (and its bike) is loaded **before** the station lock, so a concurrent double return is stopped only by accident, by `Bike`'s `@Version`, and gets a generic "modified concurrently, please retry". | `rental/RentalService.java:46-50`, `rental/Rental.java:52-54`, `shared/ApiExceptionHandler.java:23-27` | Correctness depends on a side effect nobody designed. |
| B5 | **`CLAUDE.md` contradicts the code.** It says "Domain rules live in the entities" (`CLAUDE.md:33`), but the capacity invariant lives in a service that counts rows in a repository. `Bike.dock()` accepts any station. The service guards it only by a comment: "must be called inside the transaction that docks the bike". | `station/StationService.java:37-48`, `bike/Bike.java:51-57`, `CLAUDE.md:35` | Temporal coupling: the next caller that docks a bike without that call silently overfills a station. |
| B6 | **The concurrency guarantees are only promised in comments.** No test runs two requests at once. | `bike/Bike.java:27-29`, `rider/RiderRepository.java:14-17`, `station/StationRepository.java:12-15` | The parts most likely to fail in production are the untested ones. |
| B7 | **Test clock state leaks between tests.** `MutableClock` is a singleton in a cached Spring context. Time advanced in one test is still there in the next ✓. `withZone` ignores the zone. | `src/test/java/.../rental/BikeRentalApiTest.java:34-41`, `src/test/java/.../MutableClock.java:31-34` | Tests pass today because none asserts an absolute time after another test moved the clock. Test order can break them. |
| B8 | **Station validation lives only at the API.** `capacity` must be positive in the request, but the entity accepts anything, as does any future caller. | `station/StationController.java:53`, `station/Station.java:22-25` | The invariant depends on the entry point. |

## C. Concepts that don't exist (what the business can't do)

| # | Missing | Question |
|---|---|---|
| C1 | **Payment.** The cost is computed and stored, then never charged, invoiced or refunded. | When and how is the rider charged? What if the card fails? |
| C2 | **Rebalancing.** Staff can't move a bike between stations: `dock()` only works for a `RENTED` bike. | How do bikes get from full stations to empty ones? |
| C3 | **Reservation, lost or stolen bikes, damage reports, disputes, refunds.** | Which of these does the first release need? |
| C4 | **A rental lifecycle.** "Active" is just `endedAt == null`, so cancelled, lost and force-closed rentals have no name (`rental/Rental.java:13`, `:61-63`). | What states can a rental be in, and who moves it between them? |
| C5 | **History.** The rider's history has no payments, no duration and no pagination (`rental/RentalRepository.java:11`). | What does a rider need to see, and for how long must it be kept? |

## D. Design problems (what DDD would have exposed)

| # | Problem | Where |
|---|---|---|
| D1 | **One model for everything.** `Bike` serves fleet management (register, maintenance) *and* renting. `Station` serves capacity *and* rental history. One transaction changes rider, bike, station and rental at once: `Rental.finish()` docks the bike itself. Pricing, fleet, docking and rentals can't change independently. | `rental/Rental.java:51-59`, `rental/RentalService.java:35-53` |
| D2 | **Cyclic package dependencies.** `bike → station`: `BikeService` uses `StationService`. `station → bike`: `StationService` and `StationController` use `BikeRepository`, and `StationController` imports `BikeController.BikeResponse`. The "feature packages" are one module. | `bike/BikeService.java:7`, `station/StationService.java:5`, `station/StationController.java:8-9` |
| D3 | **No ubiquitous language.** Ending a rental is called **return** (`POST /rentals/{id}/return`, `ReturnBikeRequest`), **finish** (`RentalService.finish`, `Rental.finish`) and **dock** (`Bike.dock`). Starting one is **start** (service, endpoint) and **rent** (`Bike.rent`). Is it a ride, a trip or a rental? Nobody asked. | `rental/RentalController.java:21-30, 45`, `rental/RentalService.java:35, 45`, `bike/Bike.java:41, 51` |
| D4 | **Rules are scattered, with no pattern:** bike state transitions sit in the entity (`bike/Bike.java`), station capacity in a service via a repository count (`station/StationService.java:41-48`), rider uniqueness in a controller (`rider/RiderController.java:25-30`), and "one active rental per rider" in a service via a query (`rental/RentalService.java:36-39`). `Station` and `Rider` are anemic data holders. | as listed |
| D5 | **Primitive obsession.** `start(Long riderId, Long bikeId)` compiles with the arguments swapped. Money is a `BigDecimal` with no currency. Durations and prices have no types. | `rental/RentalService.java:35`, `rental/PricingPolicy.java:19` |
| D6 | **JPA entities double as the domain model.** Sequential database ids are the public identity in every URL and response, so anyone can enumerate riders and rentals. | all `@Entity` classes, all controllers |
| D7 | **A command returns a query result.** `Bike.rent()` changes the bike and returns the station it was taken from. | `bike/Bike.java:40-49` |

## E. Code quality

| # | Problem | Where |
|---|---|---|
| E1 | **Inconsistent layering:** `RiderController` talks to the repository directly with `@Transactional` on a controller method, while the other features have services. `StationController` bypasses its own service to read `BikeRepository`. | `rider/RiderController.java:16-30`, `station/StationController.java:18, 44` |
| E2 | **A call made for its side effect:** `bikesAt` calls `stationService.get(id)` and discards the result, only to get a 404. | `station/StationController.java:43` |
| E3 | **N+1 queries:** `GET /stations` runs two count queries per station. Every `@ManyToOne` is eager by default, so each row of the rider history also loads the rider, the bike, the bike's station and both rental stations. There's no pagination. | `station/StationController.java:31-34, 47-51`, `rental/Rental.java:21-31`, `rental/RentalController.java:37-40` |
| E4 | **Odd REST:** `DELETE /bikes/{id}/maintenance` means "the bike is repaired". | `bike/BikeController.java:35-38` |
| E5 | **Error messages expose internal ids**, and tests assert the exact message strings, which makes them brittle. | `shared/NotFoundException.java:5-7`, `src/test/java/.../rental/BikeRentalApiTest.java:97, 124` |
| E6 | **One unreviewable commit** of 1 776 lines, with a typo in its message ("city city"). Nobody can review the decisions in A one by one. | `git show --stat HEAD~1` |

## F. Tests

| # | Problem | Where |
|---|---|---|
| F1 | **The tests assert the AI's own numbers.** The pricing values are copied from `application.properties` into the test (`BikeRentalApiTest.java:25-29`, `PricingPolicyTest.java:13-14`), so the tests prove the code agrees with itself. Every decision in A passes. | `src/test/java/.../rental/` |
| F2 | **Only API-level tests plus one pricing test.** There's no domain-level test of `Bike` or `Rental` transitions. | `src/test/java/` |
| F3 | **Untested:** concurrency (B6), return from maintenance, history order, the station list, 404 for a bike or a rider, the full-station return leaving the rental active, and what the rider is charged at a full station. | `BikeRentalApiTest.java` |
| F4 | `rejectsInvalidInput` checks two unrelated cases in one test, and `VibeCodeVelocityApplicationTests` only repeats the context load that every API test already does. | `BikeRentalApiTest.java:161-169`, `src/test/java/.../VibeCodeVelocityApplicationTests.java` |

## G. Security and operations

| # | Problem | Where |
|---|---|---|
| G1 | **No authentication or authorization.** Anyone can start or end a rental for any rider id, read any rider's e-mail and history, create stations and send bikes to maintenance. | all controllers |
| G2 | **The H2 console is enabled** in the main configuration, not in a dev profile. | `application.properties:3` |
| G3 | **The data lives only in memory** and is gone on restart, including active rentals and what riders owe. | `pom.xml:51-55` |

## The twist

Nothing in this repository *looks* wrong, and it's exactly what was asked for. But:
- every number, rule and edge-case behaviour in section A came from the AI;
- the tests were written to agree with them;
- `CLAUDE.md` now presents them as the project's truth to every future session.

Each row of A is a question that should have gone to the product owner. In the flow those questions come up before
any code: in the intent ("what does the business need?") and as spec examples ("a rider returns to a full station:
then…"). Sections C and D are what bounded-context discovery and planning would have surfaced:
- rentals, fleet, docking, pricing, payments and riders are separate models that change at different speeds;
- they're glued here into one transaction.

Suggested debrief order:
1. A5 (remote return);
2. A6 (full station);
3. A3 (pay for a broken bike);
4. D3 (three names for one action);
5. F1 (why the build is green).
