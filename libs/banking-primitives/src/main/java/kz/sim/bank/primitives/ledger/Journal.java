package kz.sim.bank.primitives.ledger;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import kz.sim.bank.primitives.identifier.CommandId;
import kz.sim.bank.primitives.identifier.JournalId;
import kz.sim.bank.primitives.money.CurrencyCode;
import kz.sim.bank.primitives.money.Money;

/** An immutable, balanced, single-currency double-entry journal transaction. */
public record Journal(
        JournalId id,
        CommandId sourceCommandId,
        Instant bookedAt,
        String description,
        Optional<JournalId> reversalOf,
        List<Posting> postings) {

    public Journal {
        Objects.requireNonNull(id, "journal id must not be null");
        Objects.requireNonNull(sourceCommandId, "source command id must not be null");
        Objects.requireNonNull(bookedAt, "booking time must not be null");
        Objects.requireNonNull(description, "journal description must not be null");
        Objects.requireNonNull(reversalOf, "reversal reference must not be null");
        Objects.requireNonNull(postings, "journal postings must not be null");

        description = description.strip();
        postings = List.copyOf(postings);
        if (description.isEmpty()) {
            throw new IllegalArgumentException("Journal description must not be blank");
        }
        if (postings.size() < 2) {
            throw new IllegalArgumentException("A journal must contain at least two postings");
        }
        if (reversalOf.filter(id::equals).isPresent()) {
            throw new IllegalArgumentException("A journal cannot reverse itself");
        }
        if (postings.stream().anyMatch(posting -> !posting.accountId().bankCode().equals(id.bankCode()))) {
            throw new IllegalArgumentException("Journal and posting accounts must belong to the same bank");
        }
        if (reversalOf.filter(original -> !original.bankCode().equals(id.bankCode())).isPresent()) {
            throw new IllegalArgumentException("Journal and reversal reference must belong to the same bank");
        }
        long currencyCount = postings.stream()
                .map(posting -> posting.amount().currency())
                .distinct()
                .count();
        if (currencyCount != 1) {
            throw new IllegalArgumentException("A journal must use a single currency; FX needs explicit journals");
        }
        CurrencyCode journalCurrency = postings.getFirst().amount().currency();
        if (!journalCurrency.isMovementEnabled()) {
            throw new IllegalArgumentException("Currency " + journalCurrency + " is not enabled for movements");
        }
        long accountCount = postings.stream().map(Posting::accountId).distinct().count();
        if (accountCount < 2) {
            throw new IllegalArgumentException("A journal must affect at least two distinct accounts");
        }
        BigDecimal debitTotal = total(postings, LedgerSide.DEBIT);
        BigDecimal creditTotal = total(postings, LedgerSide.CREDIT);
        if (debitTotal.compareTo(creditTotal) != 0) {
            throw new IllegalArgumentException(
                    "Journal does not balance: debits=" + debitTotal + ", credits=" + creditTotal);
        }
    }

    public static Journal book(
            JournalId id,
            CommandId sourceCommandId,
            Instant bookedAt,
            String description,
            List<Posting> postings) {
        return new Journal(id, sourceCommandId, bookedAt, description, Optional.empty(), postings);
    }

    public static Journal reversal(
            JournalId id,
            CommandId sourceCommandId,
            Instant bookedAt,
            String description,
            JournalId originalJournalId,
            List<Posting> postings) {
        return new Journal(
                id,
                sourceCommandId,
                bookedAt,
                description,
                Optional.of(Objects.requireNonNull(originalJournalId, "original journal id must not be null")),
                postings);
    }

    public CurrencyCode currency() {
        return postings.getFirst().amount().currency();
    }

    public Money totalDebits() {
        return new Money(total(postings, LedgerSide.DEBIT), currency());
    }

    public Money totalCredits() {
        return new Money(total(postings, LedgerSide.CREDIT), currency());
    }

    private static BigDecimal total(List<Posting> postings, LedgerSide side) {
        return postings.stream()
                .filter(posting -> posting.side() == side)
                .map(posting -> posting.amount().amount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
