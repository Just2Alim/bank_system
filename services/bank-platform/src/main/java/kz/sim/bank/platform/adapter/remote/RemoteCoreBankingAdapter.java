package kz.sim.bank.platform.adapter.remote;

import java.util.Arrays;
import java.util.List;
import kz.sim.bank.platform.core.api.CoreBankingPort;
import kz.sim.bank.platform.core.application.CommandContext;
import kz.sim.bank.primitives.identifier.AccountId;
import kz.sim.bank.primitives.identifier.HoldId;
import kz.sim.bank.primitives.identifier.JournalId;
import kz.sim.bank.primitives.ledger.AccountStatus;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

/** Orda edge adapter. It has no repository or database credential and can reach only the legacy core API. */
public final class RemoteCoreBankingAdapter implements CoreBankingPort {

    private final RestClient client;

    public RemoteCoreBankingAdapter(RestClient client) {
        this.client = client;
    }

    @Override
    public AccountView openAccount(CommandContext context, OpenAccountCommand command) {
        return post("/accounts", context, command, AccountView.class);
    }

    @Override
    public AccountView transitionAccount(CommandContext context, AccountId accountId, AccountStatus targetStatus) {
        return post("/accounts/" + accountId.externalForm() + "/status", context,
                new StatusRequest(targetStatus), AccountView.class);
    }

    @Override
    public AccountView getAccount(AccountId accountId) {
        return get("/accounts/" + accountId.externalForm(), AccountView.class);
    }

    @Override
    public BalanceView getBalance(AccountId accountId) {
        return get("/accounts/" + accountId.externalForm() + "/balance", BalanceView.class);
    }

    @Override
    public TransferReceipt transferInternal(CommandContext context, InternalTransferCommand command) {
        return post("/transfers/internal", context, command, TransferReceipt.class);
    }

    @Override
    public HoldView placeHold(CommandContext context, PlaceHoldCommand command) {
        return post("/holds", context, command, HoldView.class);
    }

    @Override
    public HoldView releaseHold(CommandContext context, HoldId holdId) {
        return post("/holds/" + holdId.externalForm() + "/release", context, EmptyRequest.INSTANCE, HoldView.class);
    }

    @Override
    public HoldView captureHold(CommandContext context, HoldId holdId) {
        return post("/holds/" + holdId.externalForm() + "/capture", context, EmptyRequest.INSTANCE, HoldView.class);
    }

    @Override
    public JournalView reverseJournal(CommandContext context, JournalId journalId, String reason) {
        return post("/journals/" + journalId.externalForm() + "/reversals", context,
                new ReversalRequest(reason), JournalView.class);
    }

    @Override
    public JournalView getJournal(JournalId journalId) {
        return get("/journals/" + journalId.externalForm(), JournalView.class);
    }

    @Override
    public List<JournalView> statement(AccountId accountId, int limit) {
        JournalView[] values = get(
                "/accounts/" + accountId.externalForm() + "/statement?limit=" + limit, JournalView[].class);
        return List.copyOf(Arrays.asList(values));
    }

    private <T> T get(String uri, Class<T> responseType) {
        return client.get()
                .uri(uri)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(responseType);
    }

    private <T> T post(String uri, CommandContext context, Object body, Class<T> responseType) {
        return client.post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", context.idempotencyKey())
                .header("X-Authenticated-Actor", context.actorId())
                .body(body)
                .retrieve()
                .body(responseType);
    }

    public record StatusRequest(AccountStatus status) {}

    public record ReversalRequest(String reason) {}

    private enum EmptyRequest {
        INSTANCE
    }
}
