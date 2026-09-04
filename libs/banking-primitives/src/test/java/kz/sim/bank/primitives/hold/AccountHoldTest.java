package kz.sim.bank.primitives.hold;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import kz.sim.bank.primitives.identifier.AccountId;
import kz.sim.bank.primitives.identifier.BankCode;
import kz.sim.bank.primitives.identifier.HoldId;
import kz.sim.bank.primitives.money.CurrencyCode;
import kz.sim.bank.primitives.money.Money;
import org.junit.jupiter.api.Test;

class AccountHoldTest {

    private static final Instant CREATED_AT = Instant.parse("2026-09-04T06:00:00Z");
    private static final Instant EXPIRES_AT = CREATED_AT.plus(7, ChronoUnit.DAYS);

    @Test
    void captureReleaseAndExpiryReturnNewTerminalSnapshots() {
        AccountHold active = activeHold();
        Instant beforeExpiry = CREATED_AT.plus(1, ChronoUnit.DAYS);

        AccountHold captured = active.capture(beforeExpiry);
        AccountHold released = active.release(beforeExpiry);
        AccountHold expired = active.expire(EXPIRES_AT);

        assertThat(active.status()).isEqualTo(HoldStatus.ACTIVE);
        assertThat(active.resolvedAt()).isEmpty();
        assertThat(captured.status()).isEqualTo(HoldStatus.CAPTURED);
        assertThat(released.status()).isEqualTo(HoldStatus.RELEASED);
        assertThat(expired.status()).isEqualTo(HoldStatus.EXPIRED);
        assertThat(captured.resolvedAt()).contains(beforeExpiry);
    }

    @Test
    void terminalHoldsCannotTransitionAgain() {
        AccountHold captured = activeHold().capture(CREATED_AT.plusSeconds(1));

        assertThatThrownBy(() -> captured.release(CREATED_AT.plusSeconds(2)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("terminal");
        assertThat(HoldStatus.ACTIVE.canTransitionTo(HoldStatus.CAPTURED)).isTrue();
        assertThat(HoldStatus.CAPTURED.canTransitionTo(HoldStatus.RELEASED)).isFalse();
    }

    @Test
    void transitionTimesMustRespectTheHoldWindow() {
        AccountHold active = activeHold();

        assertThatThrownBy(() -> active.capture(CREATED_AT.minusSeconds(1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("creation");
        assertThatThrownBy(() -> active.capture(EXPIRES_AT.plusSeconds(1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("expiry");
        assertThatThrownBy(() -> active.expire(EXPIRES_AT.minusSeconds(1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("before expiry");
    }

    @Test
    void constructionRejectsInvalidAmountWindowOrResolutionShape() {
        assertThatThrownBy(() -> AccountHold.create(
                HoldId.random(BankCode.ORDA), AccountId.random(BankCode.ORDA), Money.zero(CurrencyCode.KZT),
                CREATED_AT, EXPIRES_AT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive");
        assertThatThrownBy(() -> AccountHold.create(
                HoldId.random(BankCode.ORDA), AccountId.random(BankCode.ORDA), Money.of("1.00", CurrencyCode.KZT),
                CREATED_AT, CREATED_AT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("after creation");
        assertThatThrownBy(() -> new AccountHold(
                HoldId.random(BankCode.ORDA), AccountId.random(BankCode.ORDA), Money.of("1.00", CurrencyCode.KZT),
                CREATED_AT, EXPIRES_AT, HoldStatus.ACTIVE, Optional.of(CREATED_AT)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("resolution time");
        assertThatThrownBy(() -> new AccountHold(
                HoldId.random(BankCode.ORDA), AccountId.random(BankCode.ORDA), Money.of("1.00", CurrencyCode.KZT),
                CREATED_AT, EXPIRES_AT, HoldStatus.RELEASED, Optional.empty()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("resolution time");
        assertThatThrownBy(() -> AccountHold.create(
                HoldId.random(BankCode.ORDA),
                AccountId.random(BankCode.NOMAD),
                Money.of("1.00", CurrencyCode.KZT),
                CREATED_AT,
                EXPIRES_AT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("same bank");
    }

    private static AccountHold activeHold() {
        return AccountHold.create(
                HoldId.random(BankCode.ORDA),
                AccountId.random(BankCode.ORDA),
                Money.of("1500.00", CurrencyCode.KZT),
                CREATED_AT,
                EXPIRES_AT);
    }
}
