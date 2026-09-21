package kz.sim.bank.gateway.provider;

public record CustomerToken(
        String accessToken,
        String tokenType,
        String refreshToken,
        long expiresIn,
        String scope) {
}
