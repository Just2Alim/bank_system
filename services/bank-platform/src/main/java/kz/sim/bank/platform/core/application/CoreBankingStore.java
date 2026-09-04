package kz.sim.bank.platform.core.application;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import kz.sim.bank.primitives.hold.AccountHold;
import kz.sim.bank.primitives.identifier.AccountId;
import kz.sim.bank.primitives.identifier.BankCode;
import kz.sim.bank.primitives.identifier.HoldId;
import kz.sim.bank.primitives.identifier.JournalId;
import kz.sim.bank.primitives.ledger.Journal;
import kz.sim.bank.primitives.ledger.LedgerAccount;
import kz.sim.bank.primitives.money.CurrencyCode;
import kz.sim.bank.primitives.money.Money;

/** Persistence boundary used by the native core. All methods participate in the caller transaction. */
public interface CoreBankingStore {

    LedgerAccount ensureFundingAccount(BankCode bankCode, CurrencyCode currency, Instant now);

    StoredAccount createCustomerAccount(
            LedgerAccount account,
            UUID customerId,
            String accountNumber,
            String displayName,
            Instant now);

    StoredAccount requireCustomerAccount(AccountId accountId);

    StoredAccount lockCustomerAccount(AccountId accountId);

    LedgerAccount lockLedgerAccount(AccountId accountId);

    StoredAccount updateAccountStatus(AccountId accountId, kz.sim.bank.primitives.ledger.AccountStatus status, Instant now);

    BalanceRecord balance(AccountId accountId);

    Money activeHoldTotal(AccountId accountId, CurrencyCode currency, Instant now);

    void persistJournal(Journal journal);

    Journal requireJournal(JournalId journalId);

    boolean hasReversal(JournalId journalId);

    List<Journal> statement(AccountId accountId, int limit);

    AccountHold createHold(AccountHold hold);

    AccountHold lockHold(HoldId holdId);

    AccountHold updateHold(AccountHold hold);

    void lockIdempotency(String actorId, String operation, String key);

    Optional<StoredResponse> findIdempotentResponse(String actorId, String operation, String key);

    void saveIdempotentResponse(
            String actorId,
            String operation,
            String key,
            String requestHash,
            String responseJson,
            Instant createdAt);

    void appendAudit(
            Instant occurredAt,
            String actorId,
            String action,
            String aggregateType,
            String aggregateId,
            Map<String, ?> details);

    void appendOutbox(
            Instant occurredAt,
            String aggregateType,
            String aggregateId,
            String eventType,
            Map<String, ?> payload);

    record StoredAccount(
            LedgerAccount ledger,
            UUID customerId,
            String accountNumber,
            String displayName,
            Instant createdAt,
            Instant updatedAt) {}

    record BalanceRecord(Money bookBalance, long version, Instant asOf) {}

    record StoredResponse(String requestHash, String responseJson) {}
}
