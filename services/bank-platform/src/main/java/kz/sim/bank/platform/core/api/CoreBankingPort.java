package kz.sim.bank.platform.core.api;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import kz.sim.bank.platform.core.application.CommandContext;
import kz.sim.bank.primitives.hold.HoldStatus;
import kz.sim.bank.primitives.identifier.AccountId;
import kz.sim.bank.primitives.identifier.HoldId;
import kz.sim.bank.primitives.identifier.JournalId;
import kz.sim.bank.primitives.ledger.AccountStatus;
import kz.sim.bank.primitives.ledger.LedgerSide;
import kz.sim.bank.primitives.money.CurrencyCode;
import kz.sim.bank.primitives.money.Money;

/** Narrow authoritative core contract shared by native and remote Orda adapters. */
public interface CoreBankingPort {

    AccountView openAccount(CommandContext context, OpenAccountCommand command);

    AccountView transitionAccount(CommandContext context, AccountId accountId, AccountStatus targetStatus);

    AccountView getAccount(AccountId accountId);

    BalanceView getBalance(AccountId accountId);

    TransferReceipt transferInternal(CommandContext context, InternalTransferCommand command);

    HoldView placeHold(CommandContext context, PlaceHoldCommand command);

    HoldView releaseHold(CommandContext context, HoldId holdId);

    HoldView captureHold(CommandContext context, HoldId holdId);

    JournalView reverseJournal(CommandContext context, JournalId journalId, String reason);

    JournalView getJournal(JournalId journalId);

    List<JournalView> statement(AccountId accountId, int limit);

    record OpenAccountCommand(
            UUID customerId, String displayName, CurrencyCode currency, BigDecimal openingBalance) {
        public OpenAccountCommand {
            Objects.requireNonNull(customerId, "customer id must not be null");
            displayName = text(displayName, "display name", 120);
            Objects.requireNonNull(currency, "currency must not be null");
            if (!currency.isMovementEnabled()) {
                throw new IllegalArgumentException("Only KZT movements are enabled in this simulator scope");
            }
            openingBalance = amount(openingBalance, "opening balance", true);
        }
    }

    record InternalTransferCommand(
            AccountId senderAccountId, AccountId receiverAccountId, BigDecimal amount, String narrative) {
        public InternalTransferCommand {
            Objects.requireNonNull(senderAccountId, "sender account must not be null");
            Objects.requireNonNull(receiverAccountId, "receiver account must not be null");
            if (senderAccountId.equals(receiverAccountId)) {
                throw new IllegalArgumentException("Sender and receiver must be different accounts");
            }
            amount = amount(amount, "transfer amount", false);
            narrative = text(narrative, "narrative", 180);
        }
    }

    record PlaceHoldCommand(AccountId accountId, BigDecimal amount, Duration ttl) {
        public PlaceHoldCommand {
            Objects.requireNonNull(accountId, "account must not be null");
            amount = amount(amount, "hold amount", false);
            Objects.requireNonNull(ttl, "hold ttl must not be null");
            if (ttl.isNegative() || ttl.isZero() || ttl.compareTo(Duration.ofDays(7)) > 0) {
                throw new IllegalArgumentException("Hold ttl must be between one nanosecond and seven days");
            }
        }
    }

    record AccountView(
            AccountId id,
            UUID customerId,
            String accountNumber,
            String displayName,
            CurrencyCode currency,
            AccountStatus status,
            Instant createdAt,
            Instant updatedAt) {}

    record BalanceView(
            AccountId accountId,
            Money bookBalance,
            Money heldAmount,
            Money availableBalance,
            long version,
            Instant asOf) {}

    record TransferReceipt(JournalId journalId, String status, Instant bookedAt) {}

    record HoldView(
            HoldId id,
            AccountId accountId,
            Money amount,
            HoldStatus status,
            Instant createdAt,
            Instant expiresAt,
            Optional<Instant> resolvedAt) {}

    record JournalLine(AccountId accountId, LedgerSide side, Money amount, String narrative) {}

    record JournalView(
            JournalId id,
            Instant bookedAt,
            String description,
            Optional<JournalId> reversalOf,
            List<JournalLine> lines) {
        public JournalView {
            lines = List.copyOf(lines);
        }
    }

    private static BigDecimal amount(BigDecimal value, String label, boolean zeroAllowed) {
        Objects.requireNonNull(value, label + " must not be null");
        try {
            value = value.setScale(Money.SCALE, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException(label + " supports at most two fractional digits", exception);
        }
        if (value.precision() > Money.PRECISION || (zeroAllowed ? value.signum() < 0 : value.signum() <= 0)) {
            throw new IllegalArgumentException(label + (zeroAllowed ? " must not be negative" : " must be positive"));
        }
        return value;
    }

    private static String text(String value, String label, int maxLength) {
        Objects.requireNonNull(value, label + " must not be null");
        value = value.strip();
        if (value.isEmpty() || value.length() > maxLength) {
            throw new IllegalArgumentException(label + " must contain 1-" + maxLength + " characters");
        }
        return value;
    }
}
