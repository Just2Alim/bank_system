package kz.sim.bank.simulation.infrastructure;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import kz.sim.bank.simulation.application.SimulationFaultRepository;
import kz.sim.bank.simulation.domain.FaultType;
import kz.sim.bank.simulation.domain.SimulationFault;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcSimulationFaultRepository implements SimulationFaultRepository {

    private static final String COLUMNS = """
            SELECT fault_id, fault_type, target_service, activated_at, expires_at,
                   cleared_at, activated_by, revision
            FROM simulation_fault
            """;

    private final JdbcTemplate jdbc;

    public JdbcSimulationFaultRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public SimulationFault save(SimulationFault fault) {
        jdbc.update(
                """
                INSERT INTO simulation_fault (
                    fault_id, fault_type, target_service, activated_at, expires_at,
                    cleared_at, activated_by, revision)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (fault_id) DO UPDATE SET
                    cleared_at = EXCLUDED.cleared_at,
                    revision = EXCLUDED.revision
                """,
                fault.id(),
                fault.type().name(),
                fault.targetService(),
                fault.activatedAt(),
                fault.expiresAt(),
                fault.clearedAt().orElse(null),
                fault.activatedBy(),
                fault.revision());
        return fault;
    }

    @Override
    public Optional<SimulationFault> findForUpdate(String faultId) {
        return jdbc.query(COLUMNS + " WHERE fault_id = ? FOR UPDATE", this::map, faultId)
                .stream()
                .findFirst();
    }

    @Override
    public List<SimulationFault> findActive(Instant wallTime) {
        return jdbc.query(
                COLUMNS + " WHERE cleared_at IS NULL AND expires_at > ? ORDER BY activated_at, fault_id",
                this::map,
                wallTime);
    }

    private SimulationFault map(ResultSet result, int rowNumber) throws SQLException {
        var cleared = Optional.ofNullable(result.getTimestamp("cleared_at"))
                .map(timestamp -> timestamp.toInstant());
        return new SimulationFault(
                result.getString("fault_id"),
                FaultType.valueOf(result.getString("fault_type")),
                result.getString("target_service"),
                result.getTimestamp("activated_at").toInstant(),
                result.getTimestamp("expires_at").toInstant(),
                cleared,
                result.getString("activated_by"),
                result.getLong("revision"));
    }
}
