package kz.sim.bank.gateway.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.mockito.Mockito.when;

import kz.sim.bank.gateway.provider.OpenBankingProvider;
import org.junit.jupiter.api.Test;
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
}
