package kz.sim.bank.primitives.money;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class CurrencyCodeTest {

    @Test
    void factoryNormalizesAnIsoStyleCode() {
        assertThat(CurrencyCode.of(" kzt ")).isEqualTo(CurrencyCode.KZT);
        assertThat(CurrencyCode.KZT.toString()).isEqualTo("KZT");
        assertThat(CurrencyCode.KZT.isMovementEnabled()).isTrue();
        assertThat(CurrencyCode.USD.isMovementEnabled()).isFalse();
        assertThat(CurrencyCode.EUR.isMovementEnabled()).isFalse();
    }

    @Test
    void canonicalConstructorRejectsNonCanonicalCodes() {
        assertThatThrownBy(() -> new CurrencyCode("kzt"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("three uppercase");
        assertThatThrownBy(() -> CurrencyCode.of("US"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CurrencyCode.of(null))
                .isInstanceOf(NullPointerException.class);
    }
}
