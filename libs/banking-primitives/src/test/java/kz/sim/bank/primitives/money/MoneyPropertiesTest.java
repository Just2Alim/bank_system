package kz.sim.bank.primitives.money;

import static org.assertj.core.api.Assertions.assertThat;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;

class MoneyPropertiesTest {

    @Property
    void additionIsCommutativeForSameCurrency(
            @ForAll @IntRange(min = -1_000_000, max = 1_000_000) int leftMinor,
            @ForAll @IntRange(min = -1_000_000, max = 1_000_000) int rightMinor) {
        Money left = Money.ofMinor(leftMinor, CurrencyCode.KZT);
        Money right = Money.ofMinor(rightMinor, CurrencyCode.KZT);

        assertThat(left.add(right)).isEqualTo(right.add(left));
    }

    @Property
    void subtractionRoundTripsAddition(
            @ForAll @IntRange(min = -1_000_000, max = 1_000_000) int initialMinor,
            @ForAll @IntRange(min = -1_000_000, max = 1_000_000) int deltaMinor) {
        Money initial = Money.ofMinor(initialMinor, CurrencyCode.KZT);
        Money delta = Money.ofMinor(deltaMinor, CurrencyCode.KZT);

        assertThat(initial.add(delta).subtract(delta)).isEqualTo(initial);
    }
}
