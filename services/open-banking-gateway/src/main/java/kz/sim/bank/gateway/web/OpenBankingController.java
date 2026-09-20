package kz.sim.bank.gateway.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.util.List;
import kz.sim.bank.gateway.provider.AccountView;
import kz.sim.bank.gateway.provider.OpenBankingProvider;
import kz.sim.bank.gateway.provider.TransactionView;
import kz.sim.bank.gateway.provider.TransferCommand;
import kz.sim.bank.gateway.provider.TransferReceipt;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class OpenBankingController {
    private final OpenBankingProvider provider;

    public OpenBankingController(OpenBankingProvider provider) {
        this.provider = provider;
    }

    @GetMapping("/integration/status")
    Envelope<OpenBankingProvider.ProviderStatus> status() {
        return new Envelope<>(provider.status());
    }

    @GetMapping("/me/accounts")
    Envelope<List<AccountResponse>> accounts() {
        return new Envelope<>(provider.accounts().stream().map(AccountResponse::from).toList());
    }

    @GetMapping("/me/transactions")
    Envelope<List<TransactionResponse>> transactions(@RequestParam(defaultValue = "50") int limit) {
        return new Envelope<>(provider.transactions(limit).stream().map(TransactionResponse::from).toList());
    }

    @PostMapping("/transfers")
    ResponseEntity<Envelope<TransferReceipt>> transfer(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody TransferRequest request) {
        var receipt = provider.transfer(new TransferCommand(idempotencyKey, request.sourceAccountId(),
                request.destinationAccountId(), request.amount(), request.purpose()));
        return ResponseEntity.status(HttpStatus.CREATED).body(new Envelope<>(receipt));
    }

    public record Envelope<T>(T data) {}

    public record TransferRequest(
            @NotBlank String sourceAccountId,
            @NotBlank String destinationAccountId,
            @DecimalMin(value = "0.01") @Digits(integer = 16, fraction = 2) BigDecimal amount,
            @NotBlank String purpose) {}

    public record AccountResponse(String id, String displayName, String accountNumberMasked, String type,
                                  String currency, String bookBalance, String availableBalance, String status,
                                  String provider) {
        static AccountResponse from(AccountView account) {
            var iban = account.iban();
            var masked = iban.length() <= 8 ? iban : iban.substring(0, 4) + "••••" + iban.substring(iban.length() - 4);
            return new AccountResponse(account.id(), account.displayName(), masked, account.type(), account.currency(),
                    account.bookBalance().toPlainString(), account.availableBalance().toPlainString(),
                    account.status(), account.provider());
        }
    }

    public record TransactionResponse(String id, String createdAt, String description, String counterparty,
                                      String amount, String currency, String direction, String rail, String status,
                                      String correlationId, String accountId, String bookingDate, String valueDate,
                                      String reference, String rejectionReason) {
        static TransactionResponse from(TransactionView transaction) {
            var timestamp = transaction.createdAt().toString();
            return new TransactionResponse(transaction.id(), timestamp, transaction.description(), transaction.counterparty(),
                    transaction.amount().toPlainString(), transaction.currency(), transaction.direction(), transaction.rail(),
                    transaction.status(), transaction.correlationId(), transaction.accountId(), timestamp, timestamp,
                    transaction.reference(), null);
        }
    }
}
