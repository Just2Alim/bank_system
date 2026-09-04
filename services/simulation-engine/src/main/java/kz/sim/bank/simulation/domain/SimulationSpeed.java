package kz.sim.bank.simulation.domain;

public enum SimulationSpeed {
    X1(1),
    X10(10),
    X60(60),
    X360(360);

    private final int multiplier;

    SimulationSpeed(int multiplier) {
        this.multiplier = multiplier;
    }

    public int multiplier() {
        return multiplier;
    }

    public static SimulationSpeed fromMultiplier(int multiplier) {
        for (SimulationSpeed speed : values()) {
            if (speed.multiplier == multiplier) {
                return speed;
            }
        }
        throw new IllegalArgumentException("Unsupported simulation speed: " + multiplier);
    }
}
