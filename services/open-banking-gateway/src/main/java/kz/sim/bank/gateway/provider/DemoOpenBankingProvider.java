package kz.sim.bank.gateway.provider;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "open-banking.provider", havingValue = "demo", matchIfMissing = true)
public final class DemoOpenBankingProvider implements OpenBankingProvider {
    private final Map<String, AccountView> accounts = new LinkedHashMap<>();
    private final Map<String, TransferReceipt> receipts = new LinkedHashMap<>();
    private final List<TransactionView> transactions = new ArrayList<>();

    public DemoOpenBankingProvider() {
        put(new AccountView("demo-current", "KZ12998A000000000001", "Расчётный счёт", "CURRENT", "KZT",
                new BigDecimal("1250000.00"), new BigDecimal("1250000.00"), "ACTIVE", "DEMO"));
        put(new AccountView("demo-savings", "KZ34998A000000000002", "Накопительный счёт", "SAVINGS", "KZT",
                new BigDecimal("680000.00"), new BigDecimal("680000.00"), "ACTIVE", "DEMO"));
    }

    private void put(AccountView account) {
        accounts.put(account.id(), account);
    }

    @Override
    public synchronized List<AccountView> accounts() {
        return List.copyOf(accounts.values());
    }

    @Override
    public synchronized List<TransactionView> transactions(int limit) {
        return transactions.stream().limit(Math.max(0, Math.min(limit, 100))).toList();
    }

    @Override
    public synchronized TransferReceipt transfer(TransferCommand command) {
        var prior = receipts.get(command.idempotencyKey());
        if (prior != null) {
            return prior;
        }
        if (command.amount() == null || command.amount().signum() <= 0 || command.amount().scale() > 2) {
            throw new IllegalArgumentException("Amount must be positive with at most two fractional digits");
        }
        var source = requiredAccount(command.sourceAccountId());
        var destination = requiredAccount(command.destinationAccountId());
        if (source.id().equals(destination.id())) {
            throw new IllegalArgumentException("Source and destination must differ");
        }
        if (source.availableBalance().compareTo(command.amount()) < 0) {
            throw new IllegalArgumentException("Insufficient available funds");
        }
        var amount = command.amount().setScale(2);
        put(withBalance(source, source.bookBalance().subtract(amount)));
        put(withBalance(destination, destination.bookBalance().add(amount)));
        var now = Instant.now();
        var transactionId = "demo-tx-" + UUID.randomUUID();
        transactions.addFirst(new TransactionView(transactionId, now, command.purpose(), destination.displayName(), amount,
                source.currency(), "DEBIT", "INTERNAL", "COMPLETED", command.idempotencyKey(), source.id(), transactionId));
        var receipt = new TransferReceipt(command.idempotencyKey(), transactionId, "COMPLETED", now);
        receipts.put(command.idempotencyKey(), receipt);
        return receipt;
    }

    private AccountView requiredAccount(String id) {
        var account = accounts.get(id);
        if (account == null) {
            throw new IllegalArgumentException("Unknown account: " + id);
        }
        return account;
    }

    private static AccountView withBalance(AccountView account, BigDecimal balance) {
        return new AccountView(account.id(), account.iban(), account.displayName(), account.type(), account.currency(),
                balance, balance, account.status(), account.provider());
    }

    @Override
    public ProviderStatus status() {
        return new ProviderStatus("DEMO", "CONNECTED", "DEMO_ONLY", "Deterministic local test data");
    }
}
