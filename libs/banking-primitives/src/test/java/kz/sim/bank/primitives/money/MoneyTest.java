package kz.sim.bank.primitives.money;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MoneyTest {

    @Test
    void normalizesAmountsToTheLedgerScale() {
        Money money = Money.of("10", CurrencyCode.KZT);

        assertThat(money.amount()).isEqualByComparingTo("10.00");
        assertThat(money.currency()).isEqualTo(CurrencyCode.KZT);
    }

    @Test
    void performsImmutableSameCurrencyArithmetic() {
        Money original = Money.of("12.25", CurrencyCode.KZT);
        Money result = original.add(Money.of("7.75", CurrencyCode.KZT));

        assertThat(result).isEqualTo(Money.of("20.00", CurrencyCode.KZT));
        assertThat(original).isEqualTo(Money.of("12.25", CurrencyCode.KZT));
        assertThat(result.subtract(original)).isEqualTo(Money.of("7.75", CurrencyCode.KZT));
        assertThat(original.negate()).isEqualTo(Money.of("-12.25", CurrencyCode.KZT));
        assertThat(original.compareTo(Money.of("12.24", CurrencyCode.KZT))).isPositive();
    }

    @Test
    void exposesSignWithoutLosingCurrency() {
        assertThat(Money.zero(CurrencyCode.KZT).isZero()).isTrue();
        assertThat(Money.of("0.01", CurrencyCode.KZT).isPositive()).isTrue();
        assertThat(Money.of("-0.01", CurrencyCode.KZT).isNegative()).isTrue();
        assertThat(Money.of("-1.00", CurrencyCode.KZT).abs())
                .isEqualTo(Money.of("1.00", CurrencyCode.KZT));
    }

    @Test
    void rejectsCurrencyMixingAndUnsupportedPrecision() {
        assertThatThrownBy(() -> Money.of("1.001", CurrencyCode.KZT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fractional digits");
        assertThatThrownBy(() -> Money.of("100000000000000000.00", CurrencyCode.KZT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("NUMERIC(19,2)");
        assertThatThrownBy(() -> Money.of("1.00", CurrencyCode.KZT)
                .add(Money.of("1.00", CurrencyCode.USD)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("currency");
        assertThatThrownBy(() -> new Money(null, CurrencyCode.KZT))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new Money(BigDecimal.ONE, null))
                .isInstanceOf(NullPointerException.class);
    }
}
