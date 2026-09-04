package kz.sim.bank.simulation.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kz.sim.bank.simulation.application.SimulationFaultEvent;
import kz.sim.bank.simulation.application.SimulationFaultOutbox;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcSimulationFaultOutbox implements SimulationFaultOutbox {

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;
    private final UuidV7Generator identifiers;

    public JdbcSimulationFaultOutbox(
            JdbcTemplate jdbc, ObjectMapper objectMapper, UuidV7Generator identifiers) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
        this.identifiers = identifiers;
    }

    @Override
    public void append(SimulationFaultEvent event) {
        jdbc.update(
                """
                INSERT INTO outbox_event (
                    event_id, event_type, event_version, aggregate_type, aggregate_id,
                    aggregate_version, occurred_at, simulation_time, payload, status,
                    available_at, attempts)
                VALUES (?, ?, 1, 'SIMULATION_FAULT', ?, ?, ?, ?, CAST(? AS jsonb), 'PENDING', ?, 0)
                """,
                identifiers.next(),
                event.eventType(),
                event.faultId(),
                event.aggregateVersion(),
                event.occurredAt(),
                event.simulationTime(),
                serialize(event),
                event.occurredAt());
    }

    private String serialize(SimulationFaultEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize simulation fault event", exception);
        }
    }
}
