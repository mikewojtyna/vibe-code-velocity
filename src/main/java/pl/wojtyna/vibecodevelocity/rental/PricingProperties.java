package pl.wojtyna.vibecodevelocity.rental;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;

/**
 * @param unlockFee      flat fee charged for every rental
 * @param pricePerMinute charged for every started minute beyond the free ones
 * @param freeMinutes    started minutes included in the unlock fee
 */
@Validated
@ConfigurationProperties("bike-rental.pricing")
public record PricingProperties(@NotNull @PositiveOrZero BigDecimal unlockFee,
                                @NotNull @PositiveOrZero BigDecimal pricePerMinute,
                                @PositiveOrZero int freeMinutes) {
}
