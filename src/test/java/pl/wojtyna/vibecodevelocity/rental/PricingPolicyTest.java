package pl.wojtyna.vibecodevelocity.rental;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class PricingPolicyTest {

    private final PricingPolicy pricing =
            new PricingPolicy(new PricingProperties(new BigDecimal("2.00"), new BigDecimal("0.50"), 15));

    @ParameterizedTest(name = "{0}s ride costs {1}")
    @CsvSource({
            "0,    2.00",   // unlock fee only
            "900,  2.00",   // exactly 15 free minutes
            "901,  2.50",   // a started 16th minute is billed
            "960,  2.50",
            "1800, 9.50",   // 30 min -> 15 billable minutes
    })
    void chargesUnlockFeePlusEveryStartedMinuteBeyondFreeAllowance(long seconds, String expected) {
        assertThat(pricing.costOf(Duration.ofSeconds(seconds))).isEqualByComparingTo(expected);
    }
}
