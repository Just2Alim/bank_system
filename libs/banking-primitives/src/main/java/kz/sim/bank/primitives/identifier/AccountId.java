package kz.sim.bank.primitives.identifier;

public record AccountId(String value) implements TypedIdentifier {

    public AccountId {
        value = SyntheticBusinessId.requireType(value, "ACCOUNT");
    }

    public static AccountId parse(String value) {
        return new AccountId(value);
    }

    public static AccountId random(BankCode bankCode) {
        return new AccountId(SyntheticBusinessId.next("ACCOUNT", bankCode));
    }

    public BankCode bankCode() {
        return SyntheticBusinessId.bankCode(value, "ACCOUNT");
    }

    @Override
    public String externalForm() {
        return value;
    }
}
