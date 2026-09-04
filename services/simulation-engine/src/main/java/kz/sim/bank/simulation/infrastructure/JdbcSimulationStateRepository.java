package kz.sim.bank.simulation.infrastructure;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import kz.sim.bank.simulation.application.SimulationStateRepository;
import kz.sim.bank.simulation.domain.SimulationProfile;
import kz.sim.bank.simulation.domain.SimulationSpeed;
import kz.sim.bank.simulation.domain.SimulationState;
import kz.sim.bank.simulation.domain.SimulationStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcSimulationStateRepository implements SimulationStateRepository {

    private static final String SELECT_COLUMNS = """
            SELECT status, profile, seed, speed, simulation_time, wall_anchor, revision
            FROM simulation_state WHERE singleton = TRUE
            """;

    private final JdbcTemplate jdbc;

    public JdbcSimulationStateRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<SimulationState> findForUpdate() {
        return jdbc.query(SELECT_COLUMNS + " FOR UPDATE", this::map).stream().findFirst();
    }

    @Override
    public Optional<SimulationState> findCurrent() {
        return jdbc.query(SELECT_COLUMNS, this::map).stream().findFirst();
    }

    @Override
    public SimulationState save(SimulationState state) {
        int updated = jdbc.update(
                """
                UPDATE simulation_state
                SET status = ?, profile = ?, seed = ?, speed = ?, simulation_time = ?,
                    wall_anchor = ?, revision = ?, updated_at = CURRENT_TIMESTAMP
                WHERE singleton = TRUE
                """,
                state.status().name(),
                state.profile().name(),
                state.seed(),
                state.speed().multiplier(),
                state.simulationTime(),
                state.wallAnchor(),
                state.revision());
        if (updated != 1) {
            throw new IllegalStateException("Simulation state update affected " + updated + " rows");
        }
        return state;
    }

    private SimulationState map(ResultSet result, int rowNumber) throws SQLException {
        return new SimulationState(
                SimulationStatus.valueOf(result.getString("status")),
                SimulationProfile.valueOf(result.getString("profile")),
                result.getLong("seed"),
                SimulationSpeed.fromMultiplier(result.getInt("speed")),
                result.getTimestamp("simulation_time").toInstant(),
                result.getTimestamp("wall_anchor").toInstant(),
                result.getLong("revision"));
    }
}
