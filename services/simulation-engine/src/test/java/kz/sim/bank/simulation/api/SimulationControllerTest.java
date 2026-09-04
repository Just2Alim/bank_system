package kz.sim.bank.simulation.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import kz.sim.bank.simulation.application.InvalidSimulationCommandException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class SimulationControllerTest {

    @ParameterizedTest
    @CsvSource({"42,42", "'\"42\"',42", "'W/\"42\"',42", "0,0"})
    void parsesStrongAndWeakRevisionTags(String header, long expected) {
        assertThat(SimulationController.parseRevision(header)).isEqualTo(expected);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "-1", "revision", "\"not-a-number\""})
    void rejectsInvalidRevisionTags(String header) {
        assertThatThrownBy(() -> SimulationController.parseRevision(header))
                .isInstanceOf(InvalidSimulationCommandException.class);
    }
}
