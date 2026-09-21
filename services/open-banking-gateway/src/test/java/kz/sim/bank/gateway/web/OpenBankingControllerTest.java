package kz.sim.bank.gateway.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;

import kz.sim.bank.gateway.provider.CustomerAuthorizationCodeRequest;
import kz.sim.bank.gateway.provider.CustomerAuthorizationUrl;
import kz.sim.bank.gateway.provider.CustomerAuthorizationUrlRequest;
import kz.sim.bank.gateway.provider.CustomerToken;
import kz.sim.bank.gateway.provider.OpenBankingProvider;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(OpenBankingController.class)
class OpenBankingControllerTest {

    @Autowired MockMvc mvc;
    @MockitoBean OpenBankingProvider provider;

    @Test
    void statusReportsProviderWithoutLeakingCredentials() throws Exception {
        when(provider.status()).thenReturn(new OpenBankingProvider.ProviderStatus(
                "DEMO", "CONNECTED", "DEMO_ONLY", "Deterministic local test data"));
        mvc.perform(get("/api/v1/integration/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.provider").value("DEMO"))
                .andExpect(jsonPath("$.data.writeMode").value("DEMO_ONLY"));
    }

    @Test
    void generatesCustomerAuthorizationUrl() throws Exception {
        when(provider.customerAuthorizationUrl(argThat((CustomerAuthorizationUrlRequest request) ->
                request.redirectUri().equals("http://localhost:5173/customer/accounts")
                        && request.clientIdn().equals("011110001110"))))
                .thenReturn(new CustomerAuthorizationUrl("https://auth.example/start",
                        "http://localhost:5173/customer/accounts", "011110001110",
                        "ru", "oapi.business.account.api"));

        mvc.perform(post("/api/v1/integration/customer-authorization-url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"redirectUri":"http://localhost:5173/customer/accounts",
                                "clientIdn":"011110001110","lang":"ru","scope":"oapi.business.account.api"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.authUrl").value("https://auth.example/start"))
                .andExpect(jsonPath("$.data.clientIdn").value("011110001110"));
    }

    @Test
    void exchangesCustomerAuthorizationCodeWithoutLoggingSecretsInTheStatusContract() throws Exception {
        when(provider.exchangeCustomerAuthorizationCode(argThat((CustomerAuthorizationCodeRequest request) ->
                request.redirectUri().equals("http://localhost:5173/customer/accounts")
                        && request.code().equals("auth-code-42"))))
                .thenReturn(new CustomerToken("client-token", "bearer", "refresh-token", 300, "openid"));

        mvc.perform(post("/api/v1/integration/customer-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"redirectUri":"http://localhost:5173/customer/accounts","code":"auth-code-42"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("client-token"))
                .andExpect(jsonPath("$.data.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.data.expiresIn").value(300));
    }
}
