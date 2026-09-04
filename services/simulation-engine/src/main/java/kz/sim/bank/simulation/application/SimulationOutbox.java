package kz.sim.bank.simulation.application;

public interface SimulationOutbox {

    void append(SimulationEvent event);
}
