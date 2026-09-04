package kz.sim.bank.platform.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.UUID;
import kz.sim.bank.platform.adapter.persistence.PostgresCoreBankingStore;
import kz.sim.bank.platform.core.api.CoreBankingPort;
import kz.sim.bank.platform.core.application.CommandContext;
import kz.sim.bank.platform.core.application.CoreBankingStore;
import kz.sim.bank.platform.core.application.IdempotentCommandExecutor;
import kz.sim.bank.platform.core.application.NativeCoreBankingService;
import kz.sim.bank.platform.core.domain.AvailableFundsPolicy;
import kz.sim.bank.platform.core.domain.BankingJournalFactory;
import kz.sim.bank.primitives.identifier.BankCode;
import kz.sim.bank.primitives.money.CurrencyCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "bank.core.mode", havingValue = "native", matchIfMissing = true)
public class NativeCoreConfiguration {

    @Bean
    BankCode bankCode(@Value("${bank.code}") String value) {
        BankCode bankCode = BankCode.of(value);
        if (bankCode.equals(BankCode.NPP) || bankCode.equals(BankCode.SYSTEM)) {
            throw new IllegalArgumentException("bank.code must identify ORDA, NOMAD, or TENGRI");
        }
        return bankCode;
    }

    @Bean
    Clock bankingClock() {
        return Clock.systemUTC();
    }

    @Bean
    PostgresCoreBankingStore postgresCoreBankingStore(JdbcTemplate jdbc, ObjectMapper mapper) {
        return new PostgresCoreBankingStore(jdbc, mapper);
    }

    @Bean
    IdempotentCommandExecutor idempotentCommandExecutor(
            CoreBankingStore store, ObjectMapper mapper, Clock bankingClock) {
        return new IdempotentCommandExecutor(store, mapper, bankingClock);
    }

    @Bean
    CoreBankingPort coreBankingPort(
            BankCode bankCode,
            CoreBankingStore store,
            IdempotentCommandExecutor idempotency,
            Clock bankingClock) {
        return new NativeCoreBankingService(
                bankCode,
                store,
                idempotency,
                new BankingJournalFactory(),
                new AvailableFundsPolicy(),
                bankingClock);
    }

    @Bean
    @ConditionalOnProperty(name = "bank.seed.enabled", havingValue = "true", matchIfMissing = true)
    ApplicationRunner syntheticAccountSeeder(CoreBankingPort core, BankCode bankCode) {
        return arguments -> {
            seed(core, bankCode, "ALMATY-001", "Aruzhan S. (synthetic)", "250000.00");
            seed(core, bankCode, "ASTANA-002", "Dias K. (synthetic)", "175000.00");
        };
    }

    private static void seed(
            CoreBankingPort core, BankCode bankCode, String customerKey, String displayName, String amount) {
        UUID customerId = UUID.nameUUIDFromBytes(
                ("SIMULATOR:" + bankCode.value() + ":" + customerKey).getBytes(StandardCharsets.UTF_8));
        core.openAccount(
                new CommandContext("simulation-seeder", "seed-account-" + bankCode.value() + '-' + customerKey),
                new CoreBankingPort.OpenAccountCommand(
                        customerId, displayName, CurrencyCode.KZT, new java.math.BigDecimal(amount)));
    }
}
