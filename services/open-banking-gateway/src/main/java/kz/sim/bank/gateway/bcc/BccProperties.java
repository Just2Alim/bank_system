package kz.sim.bank.gateway.bcc;

public record BccProperties(
        String tokenUrl,
        String apiBaseUrl,
        String clientId,
        String clientSecret,
        String appId,
        String scope,
        String product,
        String clientToken,
        String productCode) {
    public BccProperties {
        require(tokenUrl, "BCC_TOKEN_URL");
        require(apiBaseUrl, "BCC_API_BASE_URL");
        require(clientId, "BCC_CLIENT_ID");
        require(clientSecret, "BCC_CLIENT_SECRET");
        require(scope, "BCC_SCOPE");
        if (product == null || product.isBlank()) product = "financial";
        product = product.toLowerCase(java.util.Locale.ROOT);
        if (!product.equals("financial") && !product.equals("business-account-management")) {
            throw new IllegalArgumentException("BCC_API_PRODUCT must be financial or business-account-management");
        }
        if (product.equals("financial")) require(appId, "BCC_APP_ID");
        if (clientToken == null) clientToken = "";
        if (productCode == null || productCode.isBlank()) productCode = "BusinessApi";
        if (!tokenUrl.startsWith("https://api-sandbox.bcc.kz")
                && !tokenUrl.contains(".example")) {
            throw new IllegalArgumentException("Only the BCC sandbox token endpoint is allowed");
        }
        if (!apiBaseUrl.startsWith("https://api-sandbox.bcc.kz")
                && !apiBaseUrl.contains(".example")) {
            throw new IllegalArgumentException("Only BCC sandbox API endpoints are allowed");
        }
    }

    boolean isBusinessAccountManagement() {
        return product.equals("business-account-management");
    }

    private static void require(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required when OPEN_BANKING_PROVIDER=bcc");
        }
    }
}
