package kz.sim.bank.platform.adapter.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import kz.sim.bank.platform.core.api.CoreBankingPort;
import kz.sim.bank.platform.core.application.CommandContext;
import kz.sim.bank.primitives.hold.HoldStatus;
import kz.sim.bank.primitives.identifier.AccountId;
import kz.sim.bank.primitives.identifier.HoldId;
import kz.sim.bank.primitives.identifier.JournalId;
import kz.sim.bank.primitives.ledger.AccountStatus;
import kz.sim.bank.primitives.ledger.LedgerSide;
import kz.sim.bank.primitives.money.CurrencyCode;
import kz.sim.bank.primitives.money.Money;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/internal/v1")
public class CoreBankingController {

    private static final String DEFAULT_ACTOR = "bank-platform-api";

    private final CoreBankingPort core;

    public CoreBankingController(CoreBankingPort core) {
        this.core = core;
    }

    @GetMapping("/accounts/{accountId}")
    CoreAccountResponse getAccount(@PathVariable String accountId) {
        AccountId parsed = AccountId.parse(accountId);
        return CoreAccountResponse.from(core.getAccount(parsed), core.getBalance(parsed));
    }

    @PostMapping("/transfers/internal")
    ResponseEntity<CommandReceiptResponse> transferInternal(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestHeader(name = "X-Authenticated-Actor", defaultValue = DEFAULT_ACTOR) String actor,
            @Valid @RequestBody InternalTransferRequest request) {
        var receipt = core.transferInternal(
                new CommandContext(actor, idempotencyKey),
                new CoreBankingPort.InternalTransferCommand(
                        AccountId.parse(request.sourceAccountId()),
                        AccountId.parse(request.destinationAccountId()),
                        request.money().toMoney().amount(),
                        request.narrative()));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CommandReceiptResponse.from(idempotencyKey, receipt));
    }

    @PostMapping("/holds")
    ResponseEntity<HoldResponse> placeHold(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestHeader(name = "X-Authenticated-Actor", defaultValue = DEFAULT_ACTOR) String actor,
            @Valid @RequestBody PlaceHoldRequest request) {
        var hold = core.placeHold(
                new CommandContext(actor, idempotencyKey),
                new CoreBankingPort.PlaceHoldCommand(
                        AccountId.parse(request.accountId()),
                        request.money().toMoney().amount(),
                        Duration.ofSeconds(request.ttlSeconds())));
        return ResponseEntity.status(HttpStatus.CREATED).body(HoldResponse.from(hold));
    }

    @PostMapping("/holds/{holdId}/release")
    HoldResponse releaseHold(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestHeader(name = "X-Authenticated-Actor", defaultValue = DEFAULT_ACTOR) String actor,
            @PathVariable String holdId) {
        return HoldResponse.from(core.releaseHold(new CommandContext(actor, idempotencyKey), HoldId.parse(holdId)));
    }

    @GetMapping("/journals/{journalId}")
    JournalResponse getJournal(@PathVariable String journalId) {
        return JournalResponse.from(core.getJournal(JournalId.parse(journalId)));
    }

    @GetMapping("/accounts/{accountId}/statement")
    List<JournalResponse> statement(@PathVariable String accountId) {
        return core.statement(AccountId.parse(accountId), 50).stream()
                .map(JournalResponse::from)
                .toList();
    }

    public record MoneyDto(@NotBlank String amount, @NotBlank String currency) {
        Money toMoney() {
            return Money.of(amount, CurrencyCode.of(currency));
        }

        static MoneyDto from(Money money) {
            return new MoneyDto(money.amount().toPlainString(), money.currency().value());
        }
    }

    public record CoreAccountResponse(
            String accountId,
            String customerId,
            String accountNumber,
            String displayName,
            AccountStatus status,
            MoneyDto bookBalance,
            MoneyDto availableBalance,
            MoneyDto activeHoldTotal,
            long version,
            Instant asOf) {

        static CoreAccountResponse from(CoreBankingPort.AccountView account, CoreBankingPort.BalanceView balance) {
            return new CoreAccountResponse(
                    account.id().externalForm(),
                    account.customerId().toString(),
                    account.accountNumber(),
                    account.displayName(),
                    account.status(),
                    MoneyDto.from(balance.bookBalance()),
                    MoneyDto.from(balance.availableBalance()),
                    MoneyDto.from(balance.heldAmount()),
                    balance.version(),
                    balance.asOf());
        }
    }

    public record InternalTransferRequest(
            @NotBlank String sourceAccountId,
            @NotBlank String destinationAccountId,
            @Valid @NotNull MoneyDto money,
            @NotBlank String narrative) {}

    public record PlaceHoldRequest(
            @NotBlank String accountId,
            @Valid @NotNull MoneyDto money,
            @Positive long ttlSeconds) {}

    public record CommandReceiptResponse(String commandId, String resourceId, String status, Instant bookedAt) {
        static CommandReceiptResponse from(String idempotencyKey, CoreBankingPort.TransferReceipt receipt) {
            return new CommandReceiptResponse(
                    idempotencyKey,
                    receipt.journalId().externalForm(),
                    receipt.status(),
                    receipt.bookedAt());
        }
    }

    public record HoldResponse(
            String holdId,
            String accountId,
            MoneyDto amount,
            HoldStatus status,
            Instant createdAt,
            Instant expiresAt,
            Instant resolvedAt) {

        static HoldResponse from(CoreBankingPort.HoldView hold) {
            return new HoldResponse(
                    hold.id().externalForm(),
                    hold.accountId().externalForm(),
                    MoneyDto.from(hold.amount()),
                    hold.status(),
                    hold.createdAt(),
                    hold.expiresAt(),
                    hold.resolvedAt().orElse(null));
        }
    }

    public record JournalResponse(
            String journalId,
            Instant bookedAt,
            String description,
            String reversalOf,
            List<JournalLineResponse> lines) {

        static JournalResponse from(CoreBankingPort.JournalView journal) {
            return new JournalResponse(
                    journal.id().externalForm(),
                    journal.bookedAt(),
                    journal.description(),
                    journal.reversalOf().map(JournalId::externalForm).orElse(null),
                    journal.lines().stream().map(JournalLineResponse::from).toList());
        }
    }

    public record JournalLineResponse(String accountId, LedgerSide side, MoneyDto amount, String narrative) {
        static JournalLineResponse from(CoreBankingPort.JournalLine line) {
            return new JournalLineResponse(
                    line.accountId().externalForm(),
                    line.side(),
                    MoneyDto.from(line.amount()),
                    line.narrative());
        }
    }
}
