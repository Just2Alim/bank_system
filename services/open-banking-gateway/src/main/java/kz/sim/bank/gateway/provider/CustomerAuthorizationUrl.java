package kz.sim.bank.gateway.provider;

public record CustomerAuthorizationUrl(
        String authUrl,
        String redirectUri,
        String clientIdn,
        String lang,
        String scope) {
}
