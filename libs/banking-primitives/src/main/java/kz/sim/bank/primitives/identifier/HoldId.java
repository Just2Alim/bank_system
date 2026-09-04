package kz.sim.bank.primitives.identifier;

public record HoldId(String value) implements TypedIdentifier {

    public HoldId {
        value = SyntheticBusinessId.requireType(value, "HOLD");
    }

    public static HoldId parse(String value) {
        return new HoldId(value);
    }

    public static HoldId random(BankCode bankCode) {
        return new HoldId(SyntheticBusinessId.next("HOLD", bankCode));
    }

    public BankCode bankCode() {
        return SyntheticBusinessId.bankCode(value, "HOLD");
    }

    @Override
    public String externalForm() {
        return value;
    }
}
