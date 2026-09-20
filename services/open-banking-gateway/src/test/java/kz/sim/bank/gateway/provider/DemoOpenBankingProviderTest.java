package kz.sim.bank.gateway.provider;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class DemoOpenBankingProviderTest {

    @Test
    void transferIsIdempotentAndConservesMoney() {
        var provider = new DemoOpenBankingProvider();
        var before = provider.accounts().stream()
                .map(AccountView::bookBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        var source = provider.accounts().get(0).id();
        var destination = provider.accounts().get(1).id();
        var command = new TransferCommand("demo-command-0001", source, destination,
                new BigDecimal("12500.00"), "Test transfer");

        var first = provider.transfer(command);
        var repeated = provider.transfer(command);
        var after = provider.accounts().stream()
                .map(AccountView::bookBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertThat(repeated).isEqualTo(first);
        assertThat(provider.transactions(100)).hasSize(1);
        assertThat(after).isEqualByComparingTo(before);
    }

    @Test
    void exposesDeterministicKazakhstanDemoAccounts() {
        var accounts = new DemoOpenBankingProvider().accounts();

        assertThat(accounts).hasSizeGreaterThanOrEqualTo(2);
        assertThat(accounts).allSatisfy(account -> {
            assertThat(account.iban()).startsWith("KZ");
            assertThat(account.currency()).isEqualTo("KZT");
            assertThat(account.provider()).isEqualTo("DEMO");
        });
    }
}
