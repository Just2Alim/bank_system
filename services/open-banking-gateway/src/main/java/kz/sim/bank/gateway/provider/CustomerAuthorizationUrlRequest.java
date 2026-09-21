package kz.sim.bank.gateway.provider;

public record CustomerAuthorizationUrlRequest(
        String redirectUri,
        String clientIdn,
        String lang,
        String scope) {
}
