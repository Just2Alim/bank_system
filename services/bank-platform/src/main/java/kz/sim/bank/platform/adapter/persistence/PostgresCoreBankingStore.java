package kz.sim.bank.platform.adapter.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import kz.sim.bank.platform.core.application.CoreBankingStore;
import kz.sim.bank.platform.core.application.EntityNotFoundException;
import kz.sim.bank.primitives.hold.AccountHold;
import kz.sim.bank.primitives.hold.HoldStatus;
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
import kz.sim.bank.primitives.ledger.Posting;
import kz.sim.bank.primitives.money.CurrencyCode;
import kz.sim.bank.primitives.money.Money;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

/** PostgreSQL adapter. It never connects outside the database configured for this service instance. */
public final class PostgresCoreBankingStore implements CoreBankingStore {

    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;

    public PostgresCoreBankingStore(JdbcTemplate jdbc, ObjectMapper mapper) {
        this.jdbc = jdbc;
        this.mapper = mapper;
    }

    @Override
    public LedgerAccount ensureFundingAccount(BankCode bankCode, CurrencyCode currency, Instant now) {
        String systemKey = "SIMULATION_FUNDING:" + bankCode.value() + ":" + currency.value();
        Optional<LedgerAccount> existing = findLedgerBySystemKey(systemKey);
        if (existing.isPresent()) {
            return existing.get();
        }
        AccountId id = AccountId.random(bankCode);
        jdbc.update(
                """
                insert into ledger_account
                    (id, code, name, account_class, normal_side, currency, status, system_key, created_at, updated_at)
                values (?, ?, ?, 'ASSET', 'DEBIT', ?, 'ACTIVE', ?, ?, ?)
                on conflict (system_key) do nothing
                """,
                id.externalForm(),
                "SIM.FUNDING." + currency.value(),
                "Synthetic simulation funding asset",
                currency.value(),
                systemKey,
                Timestamp.from(now),
                Timestamp.from(now));
        jdbc.update(
                """
                insert into account_balance (account_id, book_balance, version, as_of)
                select id, 0.00, 0, ? from ledger_account where system_key = ?
                on conflict (account_id) do nothing
                """,
                Timestamp.from(now),
                systemKey);
        return findLedgerBySystemKey(systemKey)
                .orElseThrow(() -> new IllegalStateException("Funding account could not be initialized"));
    }

    @Override
    public StoredAccount createCustomerAccount(
            LedgerAccount account,
            UUID customerId,
            String accountNumber,
            String displayName,
            Instant now) {
        jdbc.update(
                """
                insert into ledger_account
                    (id, code, name, account_class, normal_side, currency, status, created_at, updated_at)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                account.id().externalForm(),
                account.code(),
                account.name(),
                account.accountClass().name(),
                account.accountClass().normalSide().name(),
                account.currency().value(),
                account.status().name(),
                Timestamp.from(now),
                Timestamp.from(now));
        jdbc.update(
                "insert into customer_account (account_id, customer_id, account_number, display_name) values (?, ?, ?, ?)",
                account.id().externalForm(),
                customerId,
                accountNumber,
                displayName);
        jdbc.update(
                "insert into account_balance (account_id, book_balance, version, as_of) values (?, 0.00, 0, ?)",
                account.id().externalForm(),
                Timestamp.from(now));
        return requireCustomerAccount(account.id());
    }

    @Override
    public StoredAccount requireCustomerAccount(AccountId accountId) {
        return customerAccount(accountId, false);
    }

    @Override
    public StoredAccount lockCustomerAccount(AccountId accountId) {
        return customerAccount(accountId, true);
    }

    private StoredAccount customerAccount(AccountId accountId, boolean lock) {
        String sql = """
                select l.id, l.code, l.name, l.account_class, l.currency, l.status,
                       c.customer_id, c.account_number, c.display_name, l.created_at, l.updated_at
                from ledger_account l
                join customer_account c on c.account_id = l.id
                where l.id = ?
                """ + (lock ? " for update of l" : "");
        return jdbc.query(sql, (rs, row) -> storedAccount(rs), accountId.externalForm()).stream()
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Customer account not found: " + accountId.externalForm()));
    }

    @Override
    public LedgerAccount lockLedgerAccount(AccountId accountId) {
        return jdbc.query(
                        """
                        select id, code, name, account_class, currency, status
                        from ledger_account where id = ? for update
                        """,
                        (rs, row) -> ledgerAccount(rs),
                        accountId.externalForm())
                .stream()
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Ledger account not found: " + accountId.externalForm()));
    }

    @Override
    public StoredAccount updateAccountStatus(AccountId accountId, AccountStatus status, Instant now) {
        int updated = jdbc.update(
                "update ledger_account set status = ?, updated_at = ? where id = ?",
                status.name(),
                Timestamp.from(now),
                accountId.externalForm());
        if (updated != 1) {
            throw new EntityNotFoundException("Customer account not found: " + accountId.externalForm());
        }
        return requireCustomerAccount(accountId);
    }

    @Override
    public BalanceRecord balance(AccountId accountId) {
        return jdbc.query(
                        "select book_balance, version, as_of, l.currency from account_balance b "
                                + "join ledger_account l on l.id=b.account_id where b.account_id=?",
                        (rs, row) -> new BalanceRecord(
                                new Money(rs.getBigDecimal("book_balance"), CurrencyCode.of(rs.getString("currency"))),
                                rs.getLong("version"),
                                rs.getTimestamp("as_of").toInstant()),
                        accountId.externalForm())
                .stream()
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Balance not found: " + accountId.externalForm()));
    }

    @Override
    public Money activeHoldTotal(AccountId accountId, CurrencyCode currency, Instant now) {
        BigDecimal held = jdbc.queryForObject(
                """
                select coalesce(sum(amount), 0.00)
                from account_hold
                where account_id = ? and status = 'ACTIVE' and expires_at > ?
                """,
                BigDecimal.class,
                accountId.externalForm(),
                Timestamp.from(now));
        return new Money(held, currency);
    }

    @Override
    public void persistJournal(Journal journal) {
        jdbc.update(
                """
                insert into journal_transaction
                    (id, source_command_id, booked_at, description, reversal_of, currency)
                values (?, ?, ?, ?, ?, ?)
                """,
                journal.id().externalForm(),
                journal.sourceCommandId().value(),
                Timestamp.from(journal.bookedAt()),
                journal.description(),
                journal.reversalOf().map(JournalId::externalForm).orElse(null),
                journal.currency().value());

        int line = 0;
        for (Posting posting : journal.postings()) {
            line++;
            jdbc.update(
                    """
                    insert into ledger_entry (journal_id, line_number, account_id, side, amount, narrative)
                    values (?, ?, ?, ?, ?, ?)
                    """,
                    journal.id().externalForm(),
                    line,
                    posting.accountId().externalForm(),
                    posting.side().name(),
                    posting.amount().amount(),
                    posting.narrative());
            int direction = normalSide(posting.accountId()) == posting.side() ? 1 : -1;
            int changed = jdbc.update(
                    """
                    update account_balance
                    set book_balance = book_balance + (? * ?), version = version + 1, as_of = ?
                    where account_id = ?
                    """,
                    posting.amount().amount(),
                    direction,
                    Timestamp.from(journal.bookedAt()),
                    posting.accountId().externalForm());
            if (changed != 1) {
                throw new EntityNotFoundException(
                        "Balance projection not found: " + posting.accountId().externalForm());
            }
        }
    }

    @Override
    public Journal requireJournal(JournalId journalId) {
        var header = jdbc.query(
                        "select id, source_command_id, booked_at, description, reversal_of from journal_transaction where id=?",
                        (rs, row) -> new JournalHeader(
                                JournalId.parse(rs.getString("id")),
                                CommandId.of(rs.getObject("source_command_id", UUID.class)),
                                rs.getTimestamp("booked_at").toInstant(),
                                rs.getString("description"),
                                Optional.ofNullable(rs.getString("reversal_of")).map(JournalId::parse)),
                        journalId.externalForm())
                .stream()
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Journal not found: " + journalId.externalForm()));
        List<Posting> entries = jdbc.query(
                """
                select e.account_id, e.side, e.amount, e.narrative, j.currency
                from ledger_entry e join journal_transaction j on j.id=e.journal_id
                where e.journal_id=? order by e.line_number
                """,
                (rs, row) -> new Posting(
                        AccountId.parse(rs.getString("account_id")),
                        LedgerSide.valueOf(rs.getString("side")),
                        new Money(rs.getBigDecimal("amount"), CurrencyCode.of(rs.getString("currency"))),
                        rs.getString("narrative")),
                journalId.externalForm());
        return new Journal(
                header.id(), header.commandId(), header.bookedAt(), header.description(), header.reversalOf(), entries);
    }

    @Override
    public boolean hasReversal(JournalId journalId) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "select exists(select 1 from journal_transaction where reversal_of=?)",
                Boolean.class,
                journalId.externalForm()));
    }

    @Override
    public List<Journal> statement(AccountId accountId, int limit) {
        List<String> ids = jdbc.queryForList(
                """
                select distinct j.id
                from journal_transaction j join ledger_entry e on e.journal_id=j.id
                where e.account_id=? order by j.id desc limit ?
                """,
                String.class,
                accountId.externalForm(),
                limit);
        List<Journal> result = new ArrayList<>(ids.size());
        ids.forEach(id -> result.add(requireJournal(JournalId.parse(id))));
        return List.copyOf(result);
    }

    @Override
    public AccountHold createHold(AccountHold hold) {
        jdbc.update(
                """
                insert into account_hold
                    (id, account_id, amount, currency, status, created_at, expires_at, resolved_at)
                values (?, ?, ?, ?, ?, ?, ?, null)
                """,
                hold.id().externalForm(),
                hold.accountId().externalForm(),
                hold.amount().amount(),
                hold.amount().currency().value(),
                hold.status().name(),
                Timestamp.from(hold.createdAt()),
                Timestamp.from(hold.expiresAt()));
        return hold;
    }

    @Override
    public AccountHold lockHold(HoldId holdId) {
        return jdbc.query(
                        "select * from account_hold where id=? for update",
                        (rs, row) -> accountHold(rs),
                        holdId.externalForm())
                .stream()
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Hold not found: " + holdId.externalForm()));
    }

    @Override
    public AccountHold updateHold(AccountHold hold) {
        int updated = jdbc.update(
                "update account_hold set status=?, resolved_at=? where id=? and status='ACTIVE'",
                hold.status().name(),
                hold.resolvedAt().map(Timestamp::from).orElse(null),
                hold.id().externalForm());
        if (updated != 1) {
            throw new IllegalStateException("Hold is not active: " + hold.id().externalForm());
        }
        return hold;
    }

    @Override
    public void lockIdempotency(String actorId, String operation, String key) {
        jdbc.queryForObject(
                "select pg_advisory_xact_lock(hashtextextended(?, 0))",
                Long.class,
                actorId + '\u001f' + operation + '\u001f' + key);
    }

    @Override
    public Optional<StoredResponse> findIdempotentResponse(String actorId, String operation, String key) {
        return jdbc.query(
                        """
                        select request_hash, response_json::text from idempotency_record
                        where actor_id=? and operation=? and idempotency_key=?
                        """,
                        (rs, row) -> new StoredResponse(rs.getString(1), rs.getString(2)),
                        actorId,
                        operation,
                        key)
                .stream()
                .findFirst();
    }

    @Override
    public void saveIdempotentResponse(
            String actorId,
            String operation,
            String key,
            String requestHash,
            String responseJson,
            Instant createdAt) {
        try {
            jdbc.update(
                    """
                    insert into idempotency_record
                        (actor_id, operation, idempotency_key, request_hash, response_json, created_at)
                    values (?, ?, ?, ?, cast(? as jsonb), ?)
                    """,
                    actorId,
                    operation,
                    key,
                    requestHash,
                    responseJson,
                    Timestamp.from(createdAt));
        } catch (DuplicateKeyException duplicate) {
            throw new IllegalStateException("Concurrent idempotency write escaped the advisory lock", duplicate);
        }
    }

    @Override
    public void appendAudit(
            Instant occurredAt,
            String actorId,
            String action,
            String aggregateType,
            String aggregateId,
            Map<String, ?> details) {
        jdbc.update(
                """
                insert into audit_event
                    (id, occurred_at, actor_id, action, aggregate_type, aggregate_id, details)
                values (?, ?, ?, ?, ?, ?, cast(? as jsonb))
                """,
                CommandId.random().value(),
                Timestamp.from(occurredAt),
                actorId,
                action,
                aggregateType,
                aggregateId,
                json(details));
    }

    @Override
    public void appendOutbox(
            Instant occurredAt,
            String aggregateType,
            String aggregateId,
            String eventType,
            Map<String, ?> payload) {
        jdbc.update(
                """
                insert into outbox_event
                    (id, occurred_at, aggregate_type, aggregate_id, event_type, payload)
                values (?, ?, ?, ?, ?, cast(? as jsonb))
                """,
                CommandId.random().value(),
                Timestamp.from(occurredAt),
                aggregateType,
                aggregateId,
                eventType,
                json(payload));
    }

    public List<OutboxRecord> lockUnpublishedOutbox(int limit) {
        return jdbc.query(
                """
                select id, aggregate_id, event_type, occurred_at, payload::text
                from outbox_event where published_at is null
                order by occurred_at, id limit ? for update skip locked
                """,
                (rs, row) -> new OutboxRecord(
                        rs.getObject("id", UUID.class),
                        rs.getString("aggregate_id"),
                        rs.getString("event_type"),
                        rs.getTimestamp("occurred_at").toInstant(),
                        rs.getString("payload")),
                limit);
    }

    public void markOutboxPublished(UUID id, Instant at) {
        jdbc.update("update outbox_event set published_at=? where id=? and published_at is null", Timestamp.from(at), id);
    }

    private Optional<LedgerAccount> findLedgerBySystemKey(String systemKey) {
        return jdbc.query(
                        "select id, code, name, account_class, currency, status from ledger_account where system_key=?",
                        (rs, row) -> ledgerAccount(rs),
                        systemKey)
                .stream()
                .findFirst();
    }

    private LedgerSide normalSide(AccountId accountId) {
        return jdbc.queryForObject(
                "select normal_side from ledger_account where id=?",
                (rs, row) -> LedgerSide.valueOf(rs.getString(1)),
                accountId.externalForm());
    }

    private StoredAccount storedAccount(ResultSet rs) throws SQLException {
        return new StoredAccount(
                ledgerAccount(rs),
                rs.getObject("customer_id", UUID.class),
                rs.getString("account_number"),
                rs.getString("display_name"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant());
    }

    private LedgerAccount ledgerAccount(ResultSet rs) throws SQLException {
        return new LedgerAccount(
                AccountId.parse(rs.getString("id")),
                rs.getString("code"),
                rs.getString("name"),
                LedgerAccountClass.valueOf(rs.getString("account_class")),
                CurrencyCode.of(rs.getString("currency")),
                AccountStatus.valueOf(rs.getString("status")));
    }

    private AccountHold accountHold(ResultSet rs) throws SQLException {
        Timestamp resolved = rs.getTimestamp("resolved_at");
        return new AccountHold(
                HoldId.parse(rs.getString("id")),
                AccountId.parse(rs.getString("account_id")),
                new Money(rs.getBigDecimal("amount"), CurrencyCode.of(rs.getString("currency"))),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("expires_at").toInstant(),
                HoldStatus.valueOf(rs.getString("status")),
                resolved == null ? Optional.empty() : Optional.of(resolved.toInstant()));
    }

    private String json(Map<String, ?> value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Event details cannot be serialized", exception);
        }
    }

    private record JournalHeader(
            JournalId id,
            CommandId commandId,
            Instant bookedAt,
            String description,
            Optional<JournalId> reversalOf) {}

    public record OutboxRecord(
            UUID id, String aggregateId, String eventType, Instant occurredAt, String payloadJson) {}
}
