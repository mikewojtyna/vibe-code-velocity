package pl.wojtyna.vibecodevelocity.rental;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;

@Component
public class PricingPolicy {

    private final PricingProperties properties;

    public PricingPolicy(PricingProperties properties) {
        this.properties = properties;
    }

    /** Unlock fee plus the per-minute rate for every started minute beyond the free allowance. */
    public BigDecimal costOf(Duration rideDuration) {
        long startedMinutes = Math.ceilDiv(rideDuration.toSeconds(), 60);
        long billableMinutes = Math.max(0, startedMinutes - properties.freeMinutes());
        return properties.unlockFee()
                .add(properties.pricePerMinute().multiply(BigDecimal.valueOf(billableMinutes)))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
