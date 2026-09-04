package kz.sim.bank.platform.core.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import kz.sim.bank.primitives.money.CurrencyCode;
import kz.sim.bank.primitives.money.Money;
import org.junit.jupiter.api.Test;

class AvailableFundsPolicyTest {

    private final AvailableFundsPolicy policy = new AvailableFundsPolicy();

    @Test
    void subtractsOnlyActiveHoldsFromBookBalance() {
        var availability = policy.calculate(
                Money.of("10000.00", CurrencyCode.KZT), Money.of("2500.50", CurrencyCode.KZT));

        assertThat(availability).isEqualTo(Money.of("7499.50", CurrencyCode.KZT));
    }

    @Test
    void permitsAnExactAvailableBalanceDebit() {
        policy.requireSufficient(
                Money.of("10000.00", CurrencyCode.KZT),
                Money.of("2500.00", CurrencyCode.KZT),
                Money.of("7500.00", CurrencyCode.KZT));
    }

    @Test
    void rejectsDebitThatWouldSpendHeldFunds() {
        assertThatThrownBy(() -> policy.requireSufficient(
                        Money.of("10000.00", CurrencyCode.KZT),
                        Money.of("2500.00", CurrencyCode.KZT),
                        Money.of("7500.01", CurrencyCode.KZT)))
                .isInstanceOf(InsufficientFundsException.class)
                .hasMessageContaining("available");
    }

    @Test
    void rejectsCurrencyMismatch() {
        assertThatThrownBy(() -> policy.calculate(
                        Money.of("100.00", CurrencyCode.KZT), Money.of("10.00", CurrencyCode.USD)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
