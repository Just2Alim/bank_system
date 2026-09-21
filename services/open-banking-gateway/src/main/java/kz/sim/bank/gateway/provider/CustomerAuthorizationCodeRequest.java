package kz.sim.bank.gateway.provider;

public record CustomerAuthorizationCodeRequest(
        String redirectUri,
        String code,
        String refreshToken) {
}
