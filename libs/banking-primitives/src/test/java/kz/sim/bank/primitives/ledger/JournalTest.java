package kz.sim.bank.primitives.ledger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import kz.sim.bank.primitives.identifier.AccountId;
import kz.sim.bank.primitives.identifier.BankCode;
import kz.sim.bank.primitives.identifier.CommandId;
import kz.sim.bank.primitives.identifier.JournalId;
import kz.sim.bank.primitives.money.CurrencyCode;
import kz.sim.bank.primitives.money.Money;
import org.junit.jupiter.api.Test;

class JournalTest {

    private static final Instant BOOKED_AT = Instant.parse("2026-09-04T06:00:00Z");

    @Test
    void acceptsAUniqueBalancedSingleCurrencyJournal() {
        AccountId customer = AccountId.random(BankCode.ORDA);
        AccountId funding = AccountId.random(BankCode.ORDA);

        Journal journal = Journal.book(
                JournalId.random(BankCode.ORDA),
                CommandId.random(),
                BOOKED_AT,
                " Seed customer ",
                List.of(
                        Posting.debit(funding, Money.of("2500.00", CurrencyCode.KZT), "Funding asset"),
                        Posting.credit(customer, Money.of("2500.00", CurrencyCode.KZT), "Customer liability")));

        assertThat(journal.description()).isEqualTo("Seed customer");
        assertThat(journal.currency()).isEqualTo(CurrencyCode.KZT);
        assertThat(journal.totalDebits()).isEqualTo(Money.of("2500.00", CurrencyCode.KZT));
        assertThat(journal.totalCredits()).isEqualTo(Money.of("2500.00", CurrencyCode.KZT));
        assertThat(journal.reversalOf()).isEmpty();
    }

    @Test
    void makesThePostingCollectionDefensivelyImmutable() {
        AccountId debit = AccountId.random(BankCode.ORDA);
        AccountId credit = AccountId.random(BankCode.ORDA);
        List<Posting> mutable = new ArrayList<>(List.of(
                Posting.debit(debit, Money.of("1.00", CurrencyCode.KZT), "Debit"),
                Posting.credit(credit, Money.of("1.00", CurrencyCode.KZT), "Credit")));

        Journal journal = Journal.book(
                JournalId.random(BankCode.ORDA), CommandId.random(), BOOKED_AT, "Book", mutable);
        mutable.clear();

        assertThat(journal.postings()).hasSize(2);
        assertThatThrownBy(() -> journal.postings().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void rejectsUnbalancedCrossCurrencyOrDegenerateJournals() {
        AccountId first = AccountId.random(BankCode.ORDA);
        AccountId second = AccountId.random(BankCode.ORDA);

        assertThatThrownBy(() -> book(List.of(
                Posting.debit(first, Money.of("10.00", CurrencyCode.KZT), "Debit"),
                Posting.credit(second, Money.of("9.99", CurrencyCode.KZT), "Credit"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("balance");
        assertThatThrownBy(() -> book(List.of(
                Posting.debit(first, Money.of("10.00", CurrencyCode.KZT), "Debit"),
                Posting.credit(second, Money.of("10.00", CurrencyCode.USD), "Credit"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("single currency");
        assertThatThrownBy(() -> book(List.of(
                Posting.debit(first, Money.of("10.00", CurrencyCode.KZT), "Debit"),
                Posting.credit(first, Money.of("10.00", CurrencyCode.KZT), "Credit"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("distinct accounts");
        assertThatThrownBy(() -> book(List.of(
                Posting.debit(first, Money.of("10.00", CurrencyCode.KZT), "Only one"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least two");
    }

    @Test
    void rejectsAReservedButDisabledMovementCurrency() {
        AccountId first = AccountId.random(BankCode.ORDA);
        AccountId second = AccountId.random(BankCode.ORDA);

        assertThatThrownBy(() -> book(List.of(
                Posting.debit(first, Money.of("10.00", CurrencyCode.USD), "Debit"),
                Posting.credit(second, Money.of("10.00", CurrencyCode.USD), "Credit"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not enabled");
    }

    @Test
    void rejectsAccountsOrReversalReferencesOwnedByAnotherBank() {
        AccountId ordaAccount = AccountId.random(BankCode.ORDA);
        AccountId nomadAccount = AccountId.random(BankCode.NOMAD);
        List<Posting> crossBankPostings = List.of(
                Posting.debit(ordaAccount, Money.of("10.00", CurrencyCode.KZT), "Debit"),
                Posting.credit(nomadAccount, Money.of("10.00", CurrencyCode.KZT), "Credit"));

        assertThatThrownBy(() -> book(crossBankPostings))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("same bank");

        AccountId secondOrdaAccount = AccountId.random(BankCode.ORDA);
        List<Posting> ordaPostings = List.of(
                Posting.debit(ordaAccount, Money.of("10.00", CurrencyCode.KZT), "Debit"),
                Posting.credit(secondOrdaAccount, Money.of("10.00", CurrencyCode.KZT), "Credit"));
        assertThatThrownBy(() -> Journal.reversal(
                JournalId.random(BankCode.ORDA),
                CommandId.random(),
                BOOKED_AT,
                "Wrong owner",
                JournalId.random(BankCode.NOMAD),
                ordaPostings))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("same bank");
    }

    @Test
    void postingRequiresPositiveAmountAndNarrative() {
        assertThatThrownBy(() -> Posting.debit(
                AccountId.random(BankCode.ORDA), Money.zero(CurrencyCode.KZT), "Zero"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive");
        assertThatThrownBy(() -> Posting.credit(
                AccountId.random(BankCode.ORDA), Money.of("1.00", CurrencyCode.KZT), " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("narrative");
    }

    @Test
    void reversalLinksToTheOriginalButNotToItself() {
        JournalId original = JournalId.random(BankCode.ORDA);
        JournalId reversalId = JournalId.random(BankCode.ORDA);
        AccountId debit = AccountId.random(BankCode.ORDA);
        AccountId credit = AccountId.random(BankCode.ORDA);
        List<Posting> entries = List.of(
                Posting.debit(debit, Money.of("5.00", CurrencyCode.KZT), "Reverse debit"),
                Posting.credit(credit, Money.of("5.00", CurrencyCode.KZT), "Reverse credit"));

        Journal reversal = Journal.reversal(
                reversalId, CommandId.random(), BOOKED_AT, "Reversal", original, entries);

        assertThat(reversal.reversalOf()).contains(original);
        assertThatThrownBy(() -> new Journal(
                reversalId,
                CommandId.random(),
                BOOKED_AT,
                "Self reversal",
                Optional.of(reversalId),
                entries))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("itself");
    }

    private static Journal book(List<Posting> postings) {
        return Journal.book(
                JournalId.random(BankCode.ORDA), CommandId.random(), BOOKED_AT, "Test journal", postings);
    }
}
