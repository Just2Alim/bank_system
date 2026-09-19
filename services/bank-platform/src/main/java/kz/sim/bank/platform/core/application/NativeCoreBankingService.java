package kz.sim.bank.platform.core.application;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import kz.sim.bank.platform.core.api.CoreBankingPort;
import kz.sim.bank.platform.core.domain.AvailableFundsPolicy;
import kz.sim.bank.platform.core.domain.BankingJournalFactory;
import kz.sim.bank.primitives.hold.AccountHold;
import kz.sim.bank.primitives.identifier.AccountId;
import kz.sim.bank.primitives.identifier.BankCode;
import kz.sim.bank.primitives.identifier.CommandId;
import kz.sim.bank.primitives.identifier.HoldId;
import kz.sim.bank.primitives.identifier.JournalId;
import kz.sim.bank.primitives.ledger.AccountStatus;
import kz.sim.bank.primitives.ledger.Journal;
import kz.sim.bank.primitives.ledger.LedgerAccount;
import kz.sim.bank.primitives.ledger.LedgerAccountClass;
import kz.sim.bank.primitives.ledger.LedgerSide;
import kz.sim.bank.primitives.money.CurrencyCode;
import kz.sim.bank.primitives.money.Money;
import org.springframework.transaction.annotation.Transactional;

/** Authoritative transactional application service used inside Nomad/Tengri and by Orda's legacy core. */
public class NativeCoreBankingService implements CoreBankingPort {

    private final BankCode bankCode;
    private final CoreBankingStore store;
    private final IdempotentCommandExecutor idempotency;
    private final BankingJournalFactory journals;
    private final AvailableFundsPolicy funds;
    private final Clock clock;

    public NativeCoreBankingService(
            BankCode bankCode,
            CoreBankingStore store,
            IdempotentCommandExecutor idempotency,
            BankingJournalFactory journals,
            AvailableFundsPolicy funds,
            Clock clock) {
        this.bankCode = Objects.requireNonNull(bankCode);
        this.store = Objects.requireNonNull(store);
        this.idempotency = Objects.requireNonNull(idempotency);
        this.journals = Objects.requireNonNull(journals);
        this.funds = Objects.requireNonNull(funds);
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    @Transactional
    public AccountView openAccount(CommandContext context, OpenAccountCommand command) {
        return idempotency.execute(context, "OPEN_ACCOUNT", command, AccountView.class, () -> {
            Instant now = Instant.now(clock);
            AccountId id = AccountId.random(bankCode);
            LedgerAccount pending = new LedgerAccount(
                    id,
                    accountNumber(id),
                    command.displayName(),
                    LedgerAccountClass.LIABILITY,
                    command.currency(),
                    AccountStatus.PENDING_OPEN);
            LedgerAccount active = pending.transitionTo(AccountStatus.ACTIVE);
            var stored = store.createCustomerAccount(
                    active, command.customerId(), pending.code(), command.displayName(), now);

            if (command.openingBalance().signum() > 0) {
                LedgerAccount funding = store.ensureFundingAccount(bankCode, command.currency(), now);
                lockInOrder(List.of(funding.id(), active.id()));
                Journal journal = journals.openingBalance(
                        JournalId.random(bankCode),
                        CommandId.random(),
                        now,
                        funding.id(),
                        active.id(),
                        new Money(command.openingBalance(), command.currency()));
                store.persistJournal(journal);
            }
            auditAndPublish(context, now, "ACCOUNT_OPENED", "ACCOUNT", id.externalForm(), Map.of(
                    "customerId", command.customerId().toString(),
                    "currency", command.currency().value(),
                    "openingBalance", command.openingBalance().toPlainString()));
            return accountView(stored);
        });
    }

    @Override
    @Transactional
    public AccountView transitionAccount(
            CommandContext context, AccountId accountId, AccountStatus targetStatus) {
        requireOwned(accountId);
        record TransitionRequest(String accountId, AccountStatus targetStatus) {}
        var request = new TransitionRequest(accountId.externalForm(), targetStatus);
        return idempotency.execute(context, "TRANSITION_ACCOUNT", request, AccountView.class, () -> {
            Instant now = Instant.now(clock);
            var current = store.lockCustomerAccount(accountId);
            current.ledger().transitionTo(targetStatus);
            if (targetStatus == AccountStatus.CLOSED) {
                var balance = balanceLocked(accountId, current.ledger().currency(), now);
                if (!balance.bookBalance().isZero() || !balance.heldAmount().isZero()) {
                    throw new IllegalStateException("An account can close only with zero book balance and no active holds");
                }
            }
            var updated = store.updateAccountStatus(accountId, targetStatus, now);
            auditAndPublish(context, now, "ACCOUNT_STATUS_CHANGED", "ACCOUNT", accountId.externalForm(),
                    Map.of("from", current.ledger().status().name(), "to", targetStatus.name()));
            return accountView(updated);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public AccountView getAccount(AccountId accountId) {
        requireOwned(accountId);
        return accountView(store.requireCustomerAccount(accountId));
    }

    @Override
    @Transactional(readOnly = true)
    public BalanceView getBalance(AccountId accountId) {
        requireOwned(accountId);
        var account = store.requireCustomerAccount(accountId);
        return balanceUnlocked(accountId, account.ledger().currency(), Instant.now(clock));
    }

    @Override
    @Transactional
    public TransferReceipt transferInternal(CommandContext context, InternalTransferCommand command) {
        requireOwned(command.senderAccountId());
        requireOwned(command.receiverAccountId());
        return idempotency.execute(context, "INTERNAL_TRANSFER", command, TransferReceipt.class, () -> {
            Instant now = Instant.now(clock);
            lockInOrder(List.of(command.senderAccountId(), command.receiverAccountId()));
            var sender = store.requireCustomerAccount(command.senderAccountId());
            var receiver = store.requireCustomerAccount(command.receiverAccountId());
            requireMovable(sender.ledger());
            requireMovable(receiver.ledger());
            if (!sender.ledger().currency().equals(receiver.ledger().currency())) {
                throw new IllegalArgumentException("Internal transfer accounts must have the same currency");
            }
            Money amount = new Money(command.amount(), sender.ledger().currency());
            var senderBalance = balanceLocked(sender.ledger().id(), sender.ledger().currency(), now);
            funds.requireSufficient(senderBalance.bookBalance(), senderBalance.heldAmount(), amount);

            Journal journal = journals.internalTransfer(
                    JournalId.random(bankCode),
                    CommandId.random(),
                    now,
                    sender.ledger().id(),
                    receiver.ledger().id(),
                    amount);
            store.persistJournal(journal);
            auditAndPublish(context, now, "INTERNAL_TRANSFER_BOOKED", "JOURNAL", journal.id().externalForm(), Map.of(
                    "senderAccountId", sender.ledger().id().externalForm(),
                    "receiverAccountId", receiver.ledger().id().externalForm(),
                    "amount", amount.amount().toPlainString(),
                    "currency", amount.currency().value(),
                    "narrative", command.narrative()));
            return new TransferReceipt(journal.id(), "BOOKED", now);
        });
    }

    @Override
    @Transactional
    public HoldView placeHold(CommandContext context, PlaceHoldCommand command) {
        requireOwned(command.accountId());
        return idempotency.execute(context, "PLACE_HOLD", command, HoldView.class, () -> {
            Instant now = Instant.now(clock);
            var account = store.lockCustomerAccount(command.accountId());
            requireMovable(account.ledger());
            Money amount = new Money(command.amount(), account.ledger().currency());
            var balance = balanceLocked(command.accountId(), account.ledger().currency(), now);
            funds.requireSufficient(balance.bookBalance(), balance.heldAmount(), amount);
            AccountHold hold = AccountHold.create(
                    HoldId.random(bankCode), command.accountId(), amount, now, now.plus(command.ttl()));
            store.createHold(hold);
            auditAndPublish(context, now, "HOLD_PLACED", "HOLD", hold.id().externalForm(), Map.of(
                    "accountId", hold.accountId().externalForm(),
                    "amount", hold.amount().amount().toPlainString(),
                    "currency", hold.amount().currency().value(),
                    "expiresAt", hold.expiresAt().toString()));
            return holdView(hold);
        });
    }

    @Override
    @Transactional
    public HoldView releaseHold(CommandContext context, HoldId holdId) {
        return resolveHold(context, holdId, false);
    }

    @Override
    @Transactional
    public HoldView captureHold(CommandContext context, HoldId holdId) {
        return resolveHold(context, holdId, true);
    }

    private HoldView resolveHold(CommandContext context, HoldId holdId, boolean capture) {
        requireOwned(holdId);
        String operation = capture ? "CAPTURE_HOLD" : "RELEASE_HOLD";
        record HoldResolution(String holdId) {}
        return idempotency.execute(context, operation, new HoldResolution(holdId.externalForm()), HoldView.class, () -> {
            Instant now = Instant.now(clock);
            AccountHold current = store.lockHold(holdId);
            store.lockCustomerAccount(current.accountId());
            AccountHold resolved = capture ? current.capture(now) : current.release(now);
            store.updateHold(resolved);
            auditAndPublish(context, now, capture ? "HOLD_CAPTURED" : "HOLD_RELEASED", "HOLD",
                    holdId.externalForm(), Map.of("accountId", current.accountId().externalForm()));
            return holdView(resolved);
        });
    }

    @Override
    @Transactional
    public JournalView reverseJournal(CommandContext context, JournalId journalId, String reason) {
        requireOwned(journalId);
        record ReversalRequest(String journalId, String reason) {}
        var request = new ReversalRequest(journalId.externalForm(), boundedReason(reason));
        return idempotency.execute(context, "REVERSE_JOURNAL", request, JournalView.class, () -> {
            Instant now = Instant.now(clock);
            Journal original = store.requireJournal(journalId);
            if (original.reversalOf().isPresent()) {
                throw new IllegalStateException("A reversal journal cannot itself be reversed");
            }
            if (store.hasReversal(journalId)) {
                throw new IllegalStateException("Journal has already been reversed");
            }
            lockInOrder(original.postings().stream().map(line -> line.accountId()).distinct().toList());
            for (var line : original.postings()) {
                LedgerAccount account = store.lockLedgerAccount(line.accountId());
                if (line.side() == LedgerSide.CREDIT && account.accountClass() == LedgerAccountClass.LIABILITY) {
                    var balance = balanceLocked(account.id(), account.currency(), now);
                    funds.requireSufficient(balance.bookBalance(), balance.heldAmount(), line.amount());
                }
            }
            Journal reversal = journals.reversal(
                    JournalId.random(bankCode), CommandId.random(), now, original, request.reason());
            store.persistJournal(reversal);
            auditAndPublish(context, now, "JOURNAL_REVERSED", "JOURNAL", reversal.id().externalForm(),
                    Map.of("reversalOf", journalId.externalForm(), "reason", request.reason()));
            return journalView(reversal);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public JournalView getJournal(JournalId journalId) {
        requireOwned(journalId);
        return journalView(store.requireJournal(journalId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<JournalView> statement(AccountId accountId, int limit) {
        requireOwned(accountId);
        if (limit < 1 || limit > 200) {
            throw new IllegalArgumentException("Statement limit must be between 1 and 200");
        }
        store.requireCustomerAccount(accountId);
        return store.statement(accountId, limit).stream().map(this::journalView).toList();
    }

    private BalanceView balanceLocked(AccountId id, CurrencyCode currency, Instant now) {
        return balance(id, currency, now);
    }

    private BalanceView balanceUnlocked(AccountId id, CurrencyCode currency, Instant now) {
        return balance(id, currency, now);
    }

    private BalanceView balance(AccountId id, CurrencyCode currency, Instant now) {
        var stored = store.balance(id);
        Money held = store.activeHoldTotal(id, currency, now);
        return new BalanceView(id, stored.bookBalance(), held, funds.calculate(stored.bookBalance(), held),
                stored.version(), stored.asOf());
    }

    private void lockInOrder(List<AccountId> accountIds) {
        accountIds.stream()
                .distinct()
                .sorted(Comparator.comparing(AccountId::externalForm))
                .forEach(store::lockLedgerAccount);
    }

    private void requireMovable(LedgerAccount account) {
        if (account.status() != AccountStatus.ACTIVE) {
            throw new IllegalStateException("Account " + account.id().externalForm() + " is not active");
        }
        if (!account.currency().isMovementEnabled()) {
            throw new IllegalStateException("Account currency is not enabled for movements");
        }
    }

    private void requireOwned(AccountId id) {
        Objects.requireNonNull(id, "account id must not be null");
        if (!id.bankCode().equals(bankCode)) {
            throw new IllegalArgumentException("Account does not belong to " + bankCode);
        }
    }

    private void requireOwned(HoldId id) {
        Objects.requireNonNull(id, "hold id must not be null");
        if (!id.bankCode().equals(bankCode)) {
            throw new IllegalArgumentException("Hold does not belong to " + bankCode);
        }
    }

    private void requireOwned(JournalId id) {
        Objects.requireNonNull(id, "journal id must not be null");
        if (!id.bankCode().equals(bankCode)) {
            throw new IllegalArgumentException("Journal does not belong to " + bankCode);
        }
    }

    private void auditAndPublish(
            CommandContext context,
            Instant now,
            String event,
            String aggregateType,
            String aggregateId,
            Map<String, ?> details) {
        store.appendAudit(now, context.actorId(), event, aggregateType, aggregateId, details);
        store.appendOutbox(now, aggregateType, aggregateId, event, details);
    }

    private AccountView accountView(CoreBankingStore.StoredAccount account) {
        return new AccountView(
                account.ledger().id(), account.customerId(), account.accountNumber(), account.displayName(),
                account.ledger().currency(), account.ledger().status(), account.createdAt(), account.updatedAt());
    }

    private HoldView holdView(AccountHold hold) {
        return new HoldView(hold.id(), hold.accountId(), hold.amount(), hold.status(), hold.createdAt(),
                hold.expiresAt(), hold.resolvedAt());
    }

    private JournalView journalView(Journal journal) {
        return new JournalView(
                journal.id(),
                journal.bookedAt(),
                journal.description(),
                journal.reversalOf(),
                journal.postings().stream()
                        .map(line -> new JournalLine(line.accountId(), line.side(), line.amount(), line.narrative()))
                        .toList());
    }

    private static String accountNumber(AccountId id) {
        String suffix = id.externalForm().substring(id.externalForm().length() - 12);
        return "KZSIM" + id.bankCode().value() + suffix;
    }

    private static String boundedReason(String value) {
        Objects.requireNonNull(value, "reversal reason must not be null");
        value = value.strip();
        if (value.isEmpty() || value.length() > 180) {
            throw new IllegalArgumentException("Reversal reason must contain 1-180 characters");
        }
        return value;
    }
}
