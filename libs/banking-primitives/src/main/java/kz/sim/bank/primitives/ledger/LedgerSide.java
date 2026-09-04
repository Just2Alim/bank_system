package kz.sim.bank.primitives.ledger;

public enum LedgerSide {
    DEBIT,
    CREDIT;

    public LedgerSide opposite() {
        return this == DEBIT ? CREDIT : DEBIT;
    }
}
