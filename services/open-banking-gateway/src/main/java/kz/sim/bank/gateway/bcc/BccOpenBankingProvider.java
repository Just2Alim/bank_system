package kz.sim.bank.gateway.bcc;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import kz.sim.bank.gateway.provider.AccountView;
import kz.sim.bank.gateway.provider.CustomerAuthorizationCodeRequest;
import kz.sim.bank.gateway.provider.CustomerAuthorizationUrl;
import kz.sim.bank.gateway.provider.CustomerAuthorizationUrlRequest;
import kz.sim.bank.gateway.provider.CustomerToken;
import kz.sim.bank.gateway.provider.ExternalBankException;
import kz.sim.bank.gateway.provider.OpenBankingProvider;
import kz.sim.bank.gateway.provider.TransactionView;
import kz.sim.bank.gateway.provider.TransferCommand;
import kz.sim.bank.gateway.provider.TransferReceipt;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;

public final class BccOpenBankingProvider implements OpenBankingProvider {
    private final BccProperties properties;
    private final RestClient tokenClient;
    private final RestClient apiClient;
    private final Clock clock;
    private volatile CachedToken cachedToken;

    public BccOpenBankingProvider(BccProperties properties, RestClient tokenClient, RestClient apiClient, Clock clock) {
        this.properties = properties;
        this.tokenClient = tokenClient;
        this.apiClient = apiClient;
        this.clock = clock;
    }

    @Override
    public List<AccountView> accounts() {
        requireBusinessClientToken();
        JsonNode root = apiClient.get()
                .uri(accountsUri())
                .header("Authorization", "Bearer " + accessToken())
                .headers(this::applyBusinessHeaders)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve().body(JsonNode.class);
        ensureSuccessful(root, "accounts");
        return array(root, "accounts", "data", "items").stream().map(this::mapAccount).toList();
    }

    @Override
    public List<TransactionView> transactions(int limit) {
        var result = new ArrayList<TransactionView>();
        var to = LocalDate.now(clock);
        var from = to.minusDays(30);
        for (var account : accounts()) {
            if (result.size() >= limit) break;
            requireBusinessClientToken();
            JsonNode root = apiClient.get()
                    .uri(statementUri(account, from, to))
                    .header("Authorization", "Bearer " + accessToken())
                    .headers(this::applyBusinessHeaders)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve().body(JsonNode.class);
            ensureSuccessful(root, "statement");
            for (var node : array(root, "transactions", "operations", "items", "data")) {
                result.add(mapTransaction(node, account));
                if (result.size() >= limit) break;
            }
        }
        return List.copyOf(result);
    }

    @Override
    public TransferReceipt transfer(TransferCommand command) {
        throw new UnsupportedOperationException(
                "BCC sandbox writes are disabled until the subscribed payment schema and approval flow are verified");
    }

    @Override
    public CustomerAuthorizationUrl customerAuthorizationUrl(CustomerAuthorizationUrlRequest request) {
        var lang = blankToDefault(request.lang(), "ru");
        var scope = blankToDefault(request.scope(), "oapi.business.account.api");
        var body = new LinkedHashMap<String, String>();
        body.put("redirect_uri", request.redirectUri());
        body.put("client_idn", request.clientIdn());
        body.put("lang", lang);
        body.put("scope", scope);
        JsonNode response = postAuthClient("/generate-auth-url", body);
        var authUrl = response.path("authUrl").asText();
        if (authUrl.isBlank()) {
            throw new ExternalBankException("BCC customer authorization response has no authUrl");
        }
        return new CustomerAuthorizationUrl(authUrl, request.redirectUri(), request.clientIdn(), lang, scope);
    }

    @Override
    public CustomerToken exchangeCustomerAuthorizationCode(CustomerAuthorizationCodeRequest request) {
        var body = new LinkedHashMap<String, String>();
        body.put("redirect_uri", request.redirectUri());
        if (request.refreshToken() == null || request.refreshToken().isBlank()) {
            body.put("grant_type", "authorization_code");
            body.put("client_secret", properties.clientSecret());
            body.put("code", request.code());
        } else {
            body.put("grant_type", "refresh_token");
            body.put("client_secret", properties.clientSecret());
            body.put("refresh_token", request.refreshToken());
        }
        JsonNode response = postAuthClient("/token", body);
        var accessToken = response.path("access_token").asText();
        if (accessToken.isBlank()) {
            throw new ExternalBankException("BCC customer token response has no access_token");
        }
        return new CustomerToken(accessToken, response.path("token_type").asText("bearer"),
                response.path("refresh_token").asText(""), response.path("expires_in").asLong(300),
                response.path("scope").asText(""));
    }

    @Override
    public ProviderStatus status() {
        var product = properties.isBusinessAccountManagement()
                ? "Business Account Management API sandbox"
                : "Financial API sandbox";
        return new ProviderStatus("BCC_SANDBOX", "CONFIGURED", "READ_ONLY",
                "Accounts and statements are read from Bank CenterCredit " + product);
    }

    private String accountsUri() {
        if (properties.isBusinessAccountManagement()) {
            return properties.apiBaseUrl() + "/accounts";
        }
        return properties.apiBaseUrl() + "/" + properties.appId() + "/accounts";
    }

    private String statementUri(AccountView account, LocalDate from, LocalDate to) {
        if (properties.isBusinessAccountManagement()) {
            return properties.apiBaseUrl() + "/accounts/" + account.iban()
                    + "/statements?date_from=" + from + "&date_to=" + to + "&currency=" + account.currency();
        }
        return properties.apiBaseUrl() + "/" + properties.appId() + "/accounts/"
                + account.iban() + "/statement/v2?dateFrom=" + from + "&dateTo=" + to;
    }

    private JsonNode postAuthClient(String path, Map<String, String> body) {
        JsonNode response = apiClient.post()
                .uri(properties.authClientBaseUrl() + path)
                .header("Authorization", "Bearer " + accessToken())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve().body(JsonNode.class);
        ensureSuccessful(response, "customer authorization");
        if (response == null) {
            throw new ExternalBankException("BCC customer authorization returned an empty response");
        }
        return response;
    }

    private void requireBusinessClientToken() {
        if (properties.isBusinessAccountManagement() && properties.clientToken().isBlank()) {
            throw new ExternalBankException("BCC Business Account Management requires BCC_CLIENT_TOKEN "
                    + "(x-client-token from the customer authorization flow)");
        }
    }

    private void applyBusinessHeaders(org.springframework.http.HttpHeaders headers) {
        if (!properties.isBusinessAccountManagement()) return;
        headers.set("x-client-token", properties.clientToken());
        headers.set("productCode", properties.productCode());
    }

    private AccountView mapAccount(JsonNode node) {
        var iban = text(node, "iban", "accountNumber", "account").orElseThrow(
                () -> new IllegalStateException("BCC account response has no IBAN"));
        var currency = text(node, "currency", "currencyCode", "ccy").orElse("KZT").toUpperCase(Locale.ROOT);
        var balance = decimal(node, "balance", "bookBalance", "actualBalance", "saldo").orElse(BigDecimal.ZERO);
        var available = decimal(node, "availableBalance", "available", "balanceAvailable").orElse(balance);
        return new AccountView(iban, iban,
                text(node, "name", "accountName", "description").orElse("BCC account"),
                text(node, "type", "accountType").orElse("CURRENT"), currency, balance, available,
                text(node, "status", "state").orElse("ACTIVE"), "BCC_SANDBOX");
    }

    private TransactionView mapTransaction(JsonNode node, AccountView account) {
        var id = text(node, "referenceId", "id", "documentNumber").orElse("bcc-" + node.hashCode());
        var amount = decimal(node, "amount", "transactionAmount", "sum").orElse(BigDecimal.ZERO).abs();
        var date = text(node, "bookingDate", "date", "operationDate").flatMap(BccOpenBankingProvider::instant)
                .orElse(Instant.now(clock));
        var direction = text(node, "direction", "type", "debitCreditIndicator").orElse("DEBIT");
        direction = direction.toUpperCase(Locale.ROOT).contains("CREDIT") || direction.equalsIgnoreCase("C")
                ? "CREDIT" : "DEBIT";
        return new TransactionView(id, date,
                text(node, "purpose", "description", "paymentPurpose").orElse("BCC operation"),
                text(node, "counterparty", "counterpartyName", "beneficiaryName").orElse("BCC counterparty"),
                amount, text(node, "currency", "currencyCode").orElse(account.currency()), direction,
                "BCC_FINANCIAL_API", text(node, "status", "state").orElse("COMPLETED"), id,
                account.id(), id);
    }

    private String accessToken() {
        var now = Instant.now(clock);
        var current = cachedToken;
        if (current != null && now.isBefore(current.expiresAt().minusSeconds(30))) return current.value();
        synchronized (this) {
            current = cachedToken;
            if (current != null && now.isBefore(current.expiresAt().minusSeconds(30))) return current.value();
            var form = new LinkedMultiValueMap<String, String>();
            form.add("grant_type", "client_credentials");
            form.add("scope", properties.scope());
            var basic = Base64.getEncoder().encodeToString(
                    (properties.clientId() + ":" + properties.clientSecret()).getBytes(StandardCharsets.UTF_8));
            JsonNode response = tokenClient.post().uri(properties.tokenUrl())
                    .header("Authorization", "Basic " + basic)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form).retrieve().body(JsonNode.class);
            if (response == null || response.path("access_token").asText().isBlank()) {
                throw new IllegalStateException("BCC OAuth response has no access_token");
            }
            cachedToken = new CachedToken(response.path("access_token").asText(),
                    now.plusSeconds(Math.max(60, response.path("expires_in").asLong(300))));
            return cachedToken.value();
        }
    }

    private static List<JsonNode> array(JsonNode root, String... candidates) {
        if (root == null) return List.of();
        if (root.isArray()) return iterable(root.elements());
        for (String candidate : candidates) {
            var value = root.path(candidate);
            if (value.isArray()) return iterable(value.elements());
            if (value.isObject()) {
                var nested = array(value, "items", "accounts", "transactions", "operations");
                if (!nested.isEmpty()) return nested;
            }
        }
        return List.of();
    }

    private static void ensureSuccessful(JsonNode root, String operation) {
        if (root == null || !root.has("success") || root.path("success").asBoolean(true)) return;
        var code = root.path("code").asText("UNKNOWN");
        var reason = text(root, "reason", "description", "message").orElse("Bank sandbox returned an error");
        throw new ExternalBankException("BCC " + operation + " request failed: " + code + " " + reason);
    }

    private static List<JsonNode> iterable(Iterator<JsonNode> iterator) {
        var values = new ArrayList<JsonNode>();
        iterator.forEachRemaining(values::add);
        return values;
    }

    private static Optional<String> text(JsonNode node, String... names) {
        for (String name : names) {
            var value = node.path(name);
            if (!value.isMissingNode() && !value.isNull() && !value.asText().isBlank()) return Optional.of(value.asText());
        }
        return Optional.empty();
    }

    private static Optional<BigDecimal> decimal(JsonNode node, String... names) {
        for (String name : names) {
            var value = node.path(name);
            if (value.isNumber() || value.isTextual()) {
                try { return Optional.of(new BigDecimal(value.asText()).setScale(2)); }
                catch (NumberFormatException ignored) { /* try the next documented alias */ }
            }
        }
        return Optional.empty();
    }

    private static String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private static Optional<Instant> instant(String value) {
        try { return Optional.of(Instant.parse(value)); }
        catch (RuntimeException ignored) {
            try { return Optional.of(LocalDate.parse(value).atStartOfDay().toInstant(ZoneOffset.UTC)); }
            catch (RuntimeException ignoredAgain) { return Optional.empty(); }
        }
    }

    private record CachedToken(String value, Instant expiresAt) {}
}
