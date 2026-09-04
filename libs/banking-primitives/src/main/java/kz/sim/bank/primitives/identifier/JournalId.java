package kz.sim.bank.primitives.identifier;

public record JournalId(String value) implements TypedIdentifier {

    public JournalId {
        value = SyntheticBusinessId.requireType(value, "JOURNAL");
    }

    public static JournalId parse(String value) {
        return new JournalId(value);
    }

    public static JournalId random(BankCode bankCode) {
        return new JournalId(SyntheticBusinessId.next("JOURNAL", bankCode));
    }

    public BankCode bankCode() {
        return SyntheticBusinessId.bankCode(value, "JOURNAL");
    }

    @Override
    public String externalForm() {
        return value;
    }
}
