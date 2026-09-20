package kz.sim.bank.gateway.bcc;

public record BccProperties(
        String tokenUrl,
        String financialBaseUrl,
        String clientId,
        String clientSecret,
        String appId,
        String scope) {
    public BccProperties {
        require(tokenUrl, "BCC_TOKEN_URL");
        require(financialBaseUrl, "BCC_FINANCIAL_BASE_URL");
        require(clientId, "BCC_CLIENT_ID");
        require(clientSecret, "BCC_CLIENT_SECRET");
        require(appId, "BCC_APP_ID");
        require(scope, "BCC_SCOPE");
        if (!tokenUrl.startsWith("https://api-sandbox.bcc.kz")
                && !tokenUrl.contains(".example")) {
            throw new IllegalArgumentException("Only the BCC sandbox token endpoint is allowed");
        }
        if (!financialBaseUrl.startsWith("https://api-sandbox.bcc.kz")
                && !financialBaseUrl.contains(".example")) {
            throw new IllegalArgumentException("Only the BCC sandbox Financial API is allowed");
        }
    }

    private static void require(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required when OPEN_BANKING_PROVIDER=bcc");
        }
    }
}
