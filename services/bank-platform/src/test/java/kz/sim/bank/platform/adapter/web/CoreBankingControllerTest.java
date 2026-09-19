package kz.sim.bank.platform.adapter.web;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;
import kz.sim.bank.platform.core.api.CoreBankingPort;
import kz.sim.bank.platform.core.application.CommandContext;
import kz.sim.bank.primitives.identifier.AccountId;
import kz.sim.bank.primitives.identifier.BankCode;
import kz.sim.bank.primitives.identifier.JournalId;
import kz.sim.bank.primitives.ledger.AccountStatus;
import kz.sim.bank.primitives.money.CurrencyCode;
import kz.sim.bank.primitives.money.Money;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CoreBankingController.class)
@AutoConfigureMockMvc(addFilters = false)
class CoreBankingControllerTest {

    private static final AccountId ACCOUNT_ID = AccountId.random(BankCode.NOMAD);
    private static final AccountId RECEIVER_ID = AccountId.random(BankCode.NOMAD);

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    CoreBankingPort core;

    @Test
    void getAccount_whenAccountExists_shouldReturnAccountAndBalanceView() throws Exception {
        var now = Instant.parse("2026-09-19T10:15:30Z");
        when(core.getAccount(ACCOUNT_ID)).thenReturn(new CoreBankingPort.AccountView(
                ACCOUNT_ID,
                UUID.fromString("00000000-0000-0000-0000-000000000123"),
                "KZ-SIM-NOMAD-001",
                "Synthetic current account",
                CurrencyCode.KZT,
                AccountStatus.ACTIVE,
                now.minusSeconds(60),
                now));
        when(core.getBalance(ACCOUNT_ID)).thenReturn(new CoreBankingPort.BalanceView(
                ACCOUNT_ID,
                Money.of("125000.00", CurrencyCode.KZT),
                Money.of("2500.00", CurrencyCode.KZT),
                Money.of("122500.00", CurrencyCode.KZT),
                7L,
                now));

        mockMvc.perform(get("/internal/v1/accounts/{accountId}", ACCOUNT_ID.externalForm())
                        .header("X-Correlation-ID", "SIM-CORR-TEST-0001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId", is(ACCOUNT_ID.externalForm())))
                .andExpect(jsonPath("$.status", is("ACTIVE")))
                .andExpect(jsonPath("$.bookBalance.amount", is("125000.00")))
                .andExpect(jsonPath("$.availableBalance.amount", is("122500.00")))
                .andExpect(jsonPath("$.activeHoldTotal.amount", is("2500.00")))
                .andExpect(jsonPath("$.version", is(7)));
    }

    @Test
    void postInternalTransfer_whenValidCommand_shouldUseIdempotencyContextAndReturnReceipt() throws Exception {
        var bookedAt = Instant.parse("2026-09-19T11:00:00Z");
        var journalId = JournalId.random(BankCode.NOMAD);
        when(core.transferInternal(any(), any())).thenReturn(new CoreBankingPort.TransferReceipt(
                journalId, "BOOKED", bookedAt));

        mockMvc.perform(post("/internal/v1/transfers/internal")
                        .header("X-Correlation-ID", "SIM-CORR-TEST-0002")
                        .header("Idempotency-Key", "SIM-IDEM-TRANSFER-0001")
                        .header("X-Authenticated-Actor", "customer-api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sourceAccountId": "%s",
                                  "destinationAccountId": "%s",
                                  "money": {"amount": "1500.50", "currency": "KZT"},
                                  "narrative": "Synthetic rent"
                                }
                                """.formatted(ACCOUNT_ID.externalForm(), RECEIVER_ID.externalForm())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.commandId", is("SIM-IDEM-TRANSFER-0001")))
                .andExpect(jsonPath("$.resourceId", is(journalId.externalForm())))
                .andExpect(jsonPath("$.status", is("BOOKED")));

        var context = ArgumentCaptor.forClass(CommandContext.class);
        verify(core).transferInternal(context.capture(), any());
        org.assertj.core.api.Assertions.assertThat(context.getValue().actorId()).isEqualTo("customer-api");
        org.assertj.core.api.Assertions.assertThat(context.getValue().idempotencyKey()).isEqualTo("SIM-IDEM-TRANSFER-0001");
    }
}
