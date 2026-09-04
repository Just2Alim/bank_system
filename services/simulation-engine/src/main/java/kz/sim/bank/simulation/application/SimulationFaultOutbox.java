package kz.sim.bank.simulation.application;

public interface SimulationFaultOutbox {

    void append(SimulationFaultEvent event);
}
