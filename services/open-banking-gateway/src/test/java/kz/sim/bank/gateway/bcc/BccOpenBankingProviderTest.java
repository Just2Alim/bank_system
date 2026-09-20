package kz.sim.bank.gateway.bcc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import kz.sim.bank.gateway.provider.ExternalBankException;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class BccOpenBankingProviderTest {

    @Test
    void acquiresTokenOnceAndNormalizesAccounts() {
        var tokenBuilder = RestClient.builder();
        var apiBuilder = RestClient.builder();
        var tokenServer = MockRestServiceServer.bindTo(tokenBuilder).build();
        var apiServer = MockRestServiceServer.bindTo(apiBuilder).build();
        tokenServer.expect(once(), requestTo("https://token.example/oauth/token"))
                .andExpect(method(POST))
                .andExpect(header("Authorization", "Basic Y2xpZW50OnNlY3JldA=="))
                .andRespond(withSuccess("{\"access_token\":\"sandbox-token\",\"expires_in\":300}", MediaType.APPLICATION_JSON));
        apiServer.expect(once(), requestTo("https://api.example/financial/app-42/accounts"))
                .andExpect(method(GET))
                .andExpect(header("Authorization", "Bearer sandbox-token"))
                .andRespond(withSuccess("""
                        {"accounts":[{"iban":"KZ123456789012345678","name":"Основной счёт",
                        "currency":"KZT","balance":150000.25,"availableBalance":149000.25,"status":"OPEN"}]}
                        """, MediaType.APPLICATION_JSON));

        var provider = new BccOpenBankingProvider(
                new BccProperties("https://token.example/oauth/token", "https://api.example/financial",
                        "client", "secret", "app-42", "bcc.application.financial.api",
                        "financial", "", "BusinessApi"),
                tokenBuilder.build(), apiBuilder.build(),
                Clock.fixed(Instant.parse("2026-09-21T00:00:00Z"), ZoneOffset.UTC));

        var accounts = provider.accounts();

        assertThat(accounts).singleElement().satisfies(account -> {
            assertThat(account.iban()).isEqualTo("KZ123456789012345678");
            assertThat(account.displayName()).isEqualTo("Основной счёт");
            assertThat(account.bookBalance()).isEqualByComparingTo("150000.25");
            assertThat(account.provider()).isEqualTo("BCC_SANDBOX");
        });
        tokenServer.verify();
        apiServer.verify();
    }

    @Test
    void surfacesBccSuccessFalseAsExternalBankError() {
        var tokenBuilder = RestClient.builder();
        var apiBuilder = RestClient.builder();
        var tokenServer = MockRestServiceServer.bindTo(tokenBuilder).build();
        var apiServer = MockRestServiceServer.bindTo(apiBuilder).build();
        tokenServer.expect(once(), requestTo("https://token.example/oauth/token"))
                .andExpect(method(POST))
                .andRespond(withSuccess("{\"access_token\":\"sandbox-token\",\"expires_in\":300}", MediaType.APPLICATION_JSON));
        apiServer.expect(once(), requestTo("https://api.example/financial/app-42/accounts"))
                .andExpect(method(GET))
                .andRespond(withSuccess("{\"success\":false,\"code\":9999,\"reason\":\"Unknow error\"}",
                        MediaType.APPLICATION_JSON));

        var provider = new BccOpenBankingProvider(
                new BccProperties("https://token.example/oauth/token", "https://api.example/financial",
                        "client", "secret", "app-42", "bcc.application.financial.api",
                        "financial", "", "BusinessApi"),
                tokenBuilder.build(), apiBuilder.build(),
                Clock.fixed(Instant.parse("2026-09-21T00:00:00Z"), ZoneOffset.UTC));

        assertThatThrownBy(provider::accounts)
                .isInstanceOf(ExternalBankException.class)
                .hasMessageContaining("9999")
                .hasMessageContaining("Unknow error");
        tokenServer.verify();
        apiServer.verify();
    }

    @Test
    void businessAccountManagementRequiresClientToken() {
        var provider = new BccOpenBankingProvider(
                new BccProperties("https://token.example/oauth/token", "https://api.example/business-account-management",
                        "client", "secret", "", "bcc.application.business.account.management",
                        "business-account-management", "", "BusinessApi"),
                RestClient.builder().build(), RestClient.builder().build(),
                Clock.fixed(Instant.parse("2026-09-21T00:00:00Z"), ZoneOffset.UTC));

        assertThatThrownBy(provider::accounts)
                .isInstanceOf(ExternalBankException.class)
                .hasMessageContaining("BCC_CLIENT_TOKEN");
    }
}
