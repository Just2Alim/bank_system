package kz.sim.bank.platform.core.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import kz.sim.bank.primitives.identifier.AccountId;
import kz.sim.bank.primitives.identifier.BankCode;
import kz.sim.bank.primitives.identifier.CommandId;
import kz.sim.bank.primitives.identifier.JournalId;
import kz.sim.bank.primitives.ledger.LedgerSide;
import kz.sim.bank.primitives.money.CurrencyCode;
import kz.sim.bank.primitives.money.Money;
import org.junit.jupiter.api.Test;

class BankingJournalFactoryTest {

    private final BankingJournalFactory factory = new BankingJournalFactory();
    private final Instant now = Instant.parse("2026-09-04T12:00:00Z");

    @Test
    void opensCustomerLiabilityByDebitingFundingAssetAndCreditingCustomer() {
        var funding = AccountId.random(BankCode.NOMAD);
        var customer = AccountId.random(BankCode.NOMAD);

        var journal = factory.openingBalance(
                JournalId.random(BankCode.NOMAD), CommandId.random(), now, funding, customer, Money.of("125000.00", CurrencyCode.KZT));

        assertThat(journal.totalDebits()).isEqualTo(Money.of("125000.00", CurrencyCode.KZT));
        assertThat(journal.totalCredits()).isEqualTo(Money.of("125000.00", CurrencyCode.KZT));
        assertThat(journal.postings())
                .extracting(posting -> posting.accountId(), posting -> posting.side())
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(funding, LedgerSide.DEBIT),
                        org.assertj.core.groups.Tuple.tuple(customer, LedgerSide.CREDIT));
    }

    @Test
    void internalTransferDebitsSenderLiabilityAndCreditsReceiverLiability() {
        var sender = AccountId.random(BankCode.NOMAD);
        var receiver = AccountId.random(BankCode.NOMAD);

        var journal = factory.internalTransfer(
                JournalId.random(BankCode.NOMAD), CommandId.random(), now, sender, receiver, Money.of("999.50", CurrencyCode.KZT));

        assertThat(journal.postings())
                .extracting(posting -> posting.accountId(), posting -> posting.side())
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(sender, LedgerSide.DEBIT),
                        org.assertj.core.groups.Tuple.tuple(receiver, LedgerSide.CREDIT));
    }

    @Test
    void reversalCreatesNewBalancedJournalAndNeverMutatesOriginal() {
        var original = factory.internalTransfer(
                JournalId.random(BankCode.NOMAD), CommandId.random(), now, AccountId.random(BankCode.NOMAD), AccountId.random(BankCode.NOMAD),
                Money.of("1500.00", CurrencyCode.KZT));

        var reversal = factory.reversal(
                JournalId.random(BankCode.NOMAD), CommandId.random(), now.plusSeconds(5), original, "Operator correction");

        assertThat(reversal.id()).isNotEqualTo(original.id());
        assertThat(reversal.reversalOf()).contains(original.id());
        assertThat(reversal.postings().get(0).side()).isEqualTo(LedgerSide.CREDIT);
        assertThat(reversal.postings().get(1).side()).isEqualTo(LedgerSide.DEBIT);
        assertThat(original.reversalOf()).isEmpty();
        assertThat(reversal.totalDebits()).isEqualTo(reversal.totalCredits());
    }

    @Test
    void rejectsSameAccountTransferBeforeAJournalCanBeBuilt() {
        var account = AccountId.random(BankCode.NOMAD);

        assertThatThrownBy(() -> factory.internalTransfer(
                        JournalId.random(BankCode.NOMAD), CommandId.random(), now, account, account,
                        Money.of("1.00", CurrencyCode.KZT)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("different accounts");
    }
}
