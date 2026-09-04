package kz.sim.bank.simulation.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kz.sim.bank.simulation.application.SimulationEvent;
import kz.sim.bank.simulation.application.SimulationOutbox;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcSimulationOutbox implements SimulationOutbox {

    private static final String AGGREGATE_ID = "SIM-SIM-NPP-CONTROL";

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;
    private final UuidV7Generator identifiers;

    public JdbcSimulationOutbox(
            JdbcTemplate jdbc, ObjectMapper objectMapper, UuidV7Generator identifiers) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
        this.identifiers = identifiers;
    }

    @Override
    public void append(SimulationEvent event) {
        jdbc.update(
                """
                INSERT INTO outbox_event (
                    event_id, event_type, event_version, aggregate_type, aggregate_id,
                    aggregate_version, occurred_at, simulation_time, payload, status,
                    available_at, attempts)
                VALUES (?, ?, 1, 'SIMULATION', ?, ?, ?, ?, CAST(? AS jsonb), 'PENDING', ?, 0)
                """,
                identifiers.next(),
                event.eventType(),
                AGGREGATE_ID,
                event.aggregateVersion(),
                event.occurredAt(),
                event.simulationTime(),
                serialize(event),
                event.occurredAt());
    }

    private String serialize(SimulationEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize simulation event", exception);
        }
    }
}
