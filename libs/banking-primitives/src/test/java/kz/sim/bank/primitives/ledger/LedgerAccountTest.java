package kz.sim.bank.primitives.ledger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import kz.sim.bank.primitives.identifier.AccountId;
import kz.sim.bank.primitives.identifier.BankCode;
import kz.sim.bank.primitives.money.CurrencyCode;
import org.junit.jupiter.api.Test;

class LedgerAccountTest {

    @Test
    void accountClassesExposeTheirNormalLedgerSide() {
        assertThat(LedgerAccountClass.ASSET.normalSide()).isEqualTo(LedgerSide.DEBIT);
        assertThat(LedgerAccountClass.EXPENSE.normalSide()).isEqualTo(LedgerSide.DEBIT);
        assertThat(LedgerAccountClass.LIABILITY.normalSide()).isEqualTo(LedgerSide.CREDIT);
        assertThat(LedgerAccountClass.EQUITY.normalSide()).isEqualTo(LedgerSide.CREDIT);
        assertThat(LedgerAccountClass.REVENUE.normalSide()).isEqualTo(LedgerSide.CREDIT);
        assertThat(LedgerSide.DEBIT.opposite()).isEqualTo(LedgerSide.CREDIT);
        assertThat(LedgerSide.CREDIT.opposite()).isEqualTo(LedgerSide.DEBIT);
    }

    @Test
    void legalTransitionsReturnNewAccountSnapshots() {
        LedgerAccount pending = account(AccountStatus.PENDING_OPEN);
        LedgerAccount active = pending.transitionTo(AccountStatus.ACTIVE);
        LedgerAccount frozen = active.transitionTo(AccountStatus.FROZEN);
        LedgerAccount reopened = frozen.transitionTo(AccountStatus.ACTIVE);
        LedgerAccount closed = reopened.transitionTo(AccountStatus.CLOSED);

        assertThat(pending.status()).isEqualTo(AccountStatus.PENDING_OPEN);
        assertThat(active.status()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(frozen.status()).isEqualTo(AccountStatus.FROZEN);
        assertThat(reopened.status()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(closed.status()).isEqualTo(AccountStatus.CLOSED);
    }

    @Test
    void illegalTransitionsAreRejected() {
        assertThatThrownBy(() -> account(AccountStatus.PENDING_OPEN).transitionTo(AccountStatus.FROZEN))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PENDING_OPEN");
        assertThatThrownBy(() -> account(AccountStatus.ACTIVE).transitionTo(AccountStatus.ACTIVE))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> account(AccountStatus.CLOSED).transitionTo(AccountStatus.ACTIVE))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("terminal");
    }

    @Test
    void accountMetadataIsValidatedAndCanonicalized() {
        LedgerAccount account = new LedgerAccount(
                AccountId.random(BankCode.ORDA),
                "  CUSTOMER.KZT.001 ",
                "  Customer deposits  ",
                LedgerAccountClass.LIABILITY,
                CurrencyCode.KZT,
                AccountStatus.ACTIVE);

        assertThat(account.code()).isEqualTo("CUSTOMER.KZT.001");
        assertThat(account.name()).isEqualTo("Customer deposits");
        assertThatThrownBy(() -> new LedgerAccount(
                AccountId.random(BankCode.ORDA), "bad code", "name", LedgerAccountClass.ASSET,
                CurrencyCode.KZT, AccountStatus.ACTIVE))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new LedgerAccount(
                AccountId.random(BankCode.ORDA), "ASSET.1", " ", LedgerAccountClass.ASSET,
                CurrencyCode.KZT, AccountStatus.ACTIVE))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static LedgerAccount account(AccountStatus status) {
        return new LedgerAccount(
                AccountId.random(BankCode.ORDA),
                "CUSTOMER.KZT.001",
                "Customer deposits",
                LedgerAccountClass.LIABILITY,
                CurrencyCode.KZT,
                status);
    }
}
