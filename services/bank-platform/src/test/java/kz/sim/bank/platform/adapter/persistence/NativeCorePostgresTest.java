package kz.sim.bank.platform.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;
import kz.sim.bank.platform.BankPlatformApplication;
import kz.sim.bank.platform.core.api.CoreBankingPort;
import kz.sim.bank.platform.core.application.CommandContext;
import kz.sim.bank.platform.core.application.IdempotencyConflictException;
import kz.sim.bank.primitives.ledger.AccountStatus;
import kz.sim.bank.primitives.money.CurrencyCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(
        classes = BankPlatformApplication.class,
        properties = {
            "bank.code=NOMAD",
            "bank.core.mode=native",
            "bank.security.enabled=false",
            "bank.outbox.publisher.enabled=false"
        })
class NativeCorePostgresTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17.6-alpine")
            .withDatabaseName("nomad_core")
            .withUsername("nomad")
            .withPassword("nomad-test");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    CoreBankingPort core;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void openingAndReplayAreAtomicIdempotentAndEmitAuditAndOutbox() {
        long journalsBefore = count(Table.JOURNAL_TRANSACTION);
        long auditBefore = count(Table.AUDIT_EVENT);
        long outboxBefore = count(Table.OUTBOX_EVENT);
        var customerId = UUID.randomUUID();
        var command = new CoreBankingPort.OpenAccountCommand(
                customerId, "Synthetic Customer", CurrencyCode.KZT, new BigDecimal("50000.00"));
        var context = new CommandContext("scenario-generator", "open-" + customerId);

        var first = core.openAccount(context, command);
        var replay = core.openAccount(context, command);

        assertThat(replay).isEqualTo(first);
        assertThat(core.getBalance(first.id()).bookBalance().amount()).isEqualByComparingTo("50000.00");
        assertThat(count(Table.JOURNAL_TRANSACTION) - journalsBefore).isEqualTo(1);
        assertThat(count(Table.AUDIT_EVENT) - auditBefore).isEqualTo(1);
        assertThat(count(Table.OUTBOX_EVENT) - outboxBefore).isEqualTo(1);

        var conflicting = new CoreBankingPort.OpenAccountCommand(
                customerId, "Changed Name", CurrencyCode.KZT, new BigDecimal("50000.00"));
        assertThatThrownBy(() -> core.openAccount(context, conflicting))
                .isInstanceOf(IdempotencyConflictException.class);
    }

    @Test
    void transferLocksAccountsPostsBalancedJournalAndRespectsActiveHolds() {
        var sender = open("sender", "10000.00");
        var receiver = open("receiver", "1000.00");
        core.placeHold(
                new CommandContext("card-processor", "hold-one"),
                new CoreBankingPort.PlaceHoldCommand(
                        sender.id(), new BigDecimal("2500.00"), Duration.ofMinutes(15)));

        var receipt = core.transferInternal(
                new CommandContext("customer-api", "transfer-one"),
                new CoreBankingPort.InternalTransferCommand(
                        sender.id(), receiver.id(), new BigDecimal("7000.00"), "Synthetic rent"));

        assertThat(receipt.status()).isEqualTo("BOOKED");
        assertThat(core.getBalance(sender.id()).bookBalance().amount()).isEqualByComparingTo("3000.00");
        assertThat(core.getBalance(sender.id()).availableBalance().amount()).isEqualByComparingTo("500.00");
        assertThat(core.getBalance(receiver.id()).bookBalance().amount()).isEqualByComparingTo("8000.00");
        assertThat(jdbc.queryForObject(
                        "select sum(case when side='DEBIT' then amount else -amount end) "
                                + "from ledger_entry where journal_id=?",
                        BigDecimal.class,
                        receipt.journalId().value()))
                .isEqualByComparingTo("0.00");

        assertThatThrownBy(() -> core.transferInternal(
                        new CommandContext("customer-api", "transfer-two"),
                        new CoreBankingPort.InternalTransferCommand(
                                sender.id(), receiver.id(), new BigDecimal("500.01"), "Too much")))
                .hasMessageContaining("available");
    }

    @Test
    void reversalIsANewJournalAndDatabaseRejectsMutationOfHistory() {
        var sender = open("reversal-sender", "5000.00");
        var receiver = open("reversal-receiver", "0.00");
        var original = core.transferInternal(
                new CommandContext("customer-api", "original"),
                new CoreBankingPort.InternalTransferCommand(
                        sender.id(), receiver.id(), new BigDecimal("1250.00"), "Original"));

        var reversed = core.reverseJournal(
                new CommandContext("operator", "reverse-original"), original.journalId(), "Correction");

        assertThat(reversed.reversalOf()).contains(original.journalId());
        assertThat(reversed.id()).isNotEqualTo(original.journalId());
        assertThat(core.getBalance(sender.id()).bookBalance().amount()).isEqualByComparingTo("5000.00");
        assertThatThrownBy(() -> jdbc.update(
                        "update journal_transaction set description='tampered' where id=?",
                        original.journalId().value()))
                .isInstanceOf(org.springframework.dao.DataAccessException.class);
    }

    @Test
    void accountLifecycleIsValidatedAndClosedAccountCannotMoveMoney() {
        var account = core.openAccount(
                new CommandContext("operator", "pending-account"),
                new CoreBankingPort.OpenAccountCommand(
                        UUID.randomUUID(), "Pending Synthetic", CurrencyCode.KZT, BigDecimal.ZERO));

        var frozen = core.transitionAccount(
                new CommandContext("operator", "freeze-account"), account.id(), AccountStatus.FROZEN);
        assertThat(frozen.status()).isEqualTo(AccountStatus.FROZEN);
        var closed = core.transitionAccount(
                new CommandContext("operator", "close-account"), account.id(), AccountStatus.CLOSED);
        assertThat(closed.status()).isEqualTo(AccountStatus.CLOSED);
        assertThatThrownBy(() -> core.transitionAccount(
                        new CommandContext("operator", "reopen-account"), account.id(), AccountStatus.ACTIVE))
                .isInstanceOf(IllegalStateException.class);
    }

    private CoreBankingPort.AccountView open(String name, String balance) {
        return core.openAccount(
                new CommandContext("fixture", "open-" + name),
                new CoreBankingPort.OpenAccountCommand(
                        UUID.randomUUID(), name, CurrencyCode.KZT, new BigDecimal(balance)));
    }

    private long count(Table table) {
        return jdbc.queryForObject("select count(*) from " + table.name, Long.class);
    }

    private enum Table {
        JOURNAL_TRANSACTION("journal_transaction"),
        AUDIT_EVENT("audit_event"),
        OUTBOX_EVENT("outbox_event");

        private final String name;

        Table(String name) {
            this.name = name;
        }
    }
}
