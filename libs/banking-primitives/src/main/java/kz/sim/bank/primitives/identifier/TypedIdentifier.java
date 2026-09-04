package kz.sim.bank.primitives.identifier;

/** Marker contract that keeps identifiers from different domains distinct at compile time. */
public interface TypedIdentifier {

    String externalForm();
}
