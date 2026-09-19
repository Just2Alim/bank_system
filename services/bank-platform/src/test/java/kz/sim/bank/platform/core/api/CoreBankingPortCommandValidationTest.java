package kz.sim.bank.platform.core.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Duration;
import kz.sim.bank.primitives.identifier.AccountId;
import kz.sim.bank.primitives.identifier.BankCode;
import org.junit.jupiter.api.Test;

class CoreBankingPortCommandValidationTest {

    private static final AccountId SENDER = AccountId.random(BankCode.NOMAD);
    private static final AccountId RECEIVER = AccountId.random(BankCode.NOMAD);

    @Test
    void internalTransferCommand_whenAmountHasSingleFractionDigit_shouldNormalizeToLedgerScale() {
        var command = new CoreBankingPort.InternalTransferCommand(
                SENDER, RECEIVER, new BigDecimal("1500.5"), "Utility payment");

        assertThat(command.amount()).isEqualByComparingTo("1500.50");
        assertThat(command.amount().scale()).isEqualTo(2);
    }

    @Test
    void placeHoldCommand_whenAmountHasMoreThanTwoFractionDigits_shouldRejectIt() {
        assertThatThrownBy(() -> new CoreBankingPort.PlaceHoldCommand(
                        SENDER, new BigDecimal("100.001"), Duration.ofMinutes(5)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("two fractional digits");
    }
}
