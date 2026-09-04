package kz.sim.bank.platform.core.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import kz.sim.bank.primitives.identifier.AccountId;
import kz.sim.bank.primitives.identifier.CommandId;
import kz.sim.bank.primitives.identifier.JournalId;
import kz.sim.bank.primitives.ledger.Journal;
import kz.sim.bank.primitives.ledger.LedgerSide;
import kz.sim.bank.primitives.ledger.Posting;
import kz.sim.bank.primitives.money.Money;

/** Centralizes accounting templates so adapters cannot invent debit/credit direction. */
public final class BankingJournalFactory {

    public Journal openingBalance(
            JournalId journalId,
            CommandId commandId,
            Instant at,
            AccountId fundingAsset,
            AccountId customerLiability,
            Money amount) {
        requireDifferent(fundingAsset, customerLiability);
        return Journal.book(
                journalId,
                commandId,
                at,
                "Synthetic opening balance",
                List.of(
                        Posting.debit(fundingAsset, amount, "Simulation funding asset"),
                        Posting.credit(customerLiability, amount, "Customer deposit liability")));
    }

    public Journal internalTransfer(
            JournalId journalId,
            CommandId commandId,
            Instant at,
            AccountId senderLiability,
            AccountId receiverLiability,
            Money amount) {
        requireDifferent(senderLiability, receiverLiability);
        return Journal.book(
                journalId,
                commandId,
                at,
                "Internal customer transfer",
                List.of(
                        Posting.debit(senderLiability, amount, "Debit sender deposit liability"),
                        Posting.credit(receiverLiability, amount, "Credit receiver deposit liability")));
    }

    public Journal reversal(
            JournalId journalId,
            CommandId commandId,
            Instant at,
            Journal original,
            String reason) {
        Objects.requireNonNull(original, "original journal must not be null");
        List<Posting> reversed = original.postings().stream()
                .map(line -> new Posting(
                        line.accountId(),
                        line.side() == LedgerSide.DEBIT ? LedgerSide.CREDIT : LedgerSide.DEBIT,
                        line.amount(),
                        "Reversal: " + line.narrative()))
                .toList();
        return Journal.reversal(journalId, commandId, at, reason, original.id(), reversed);
    }

    private static void requireDifferent(AccountId first, AccountId second) {
        Objects.requireNonNull(first, "first account must not be null");
        Objects.requireNonNull(second, "second account must not be null");
        if (first.equals(second)) {
            throw new IllegalArgumentException("A transfer journal requires two different accounts");
        }
    }
}
