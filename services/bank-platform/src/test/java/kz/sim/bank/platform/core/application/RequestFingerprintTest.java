package kz.sim.bank.platform.core.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.json.JsonMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RequestFingerprintTest {

    private final RequestFingerprint fingerprint = new RequestFingerprint(JsonMapper.builder().build());

    @Test
    void isStableForEquivalentObjectKeyOrdering() {
        Map<String, Object> first = new LinkedHashMap<>();
        first.put("amount", "100.00");
        first.put("currency", "KZT");
        Map<String, Object> second = new LinkedHashMap<>();
        second.put("currency", "KZT");
        second.put("amount", "100.00");

        assertThat(fingerprint.of(first)).isEqualTo(fingerprint.of(second));
    }

    @Test
    void changesWhenRequestMeaningChanges() {
        assertThat(fingerprint.of(Map.of("amount", "100.00")))
                .isNotEqualTo(fingerprint.of(Map.of("amount", "100.01")));
    }

    @Test
    void rejectsNullPayloads() {
        assertThatThrownBy(() -> fingerprint.of(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("payload");
    }
}
