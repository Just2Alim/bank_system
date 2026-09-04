package kz.sim.bank.primitives.ledger;

public enum LedgerAccountClass {
    ASSET(LedgerSide.DEBIT),
    LIABILITY(LedgerSide.CREDIT),
    EQUITY(LedgerSide.CREDIT),
    REVENUE(LedgerSide.CREDIT),
    EXPENSE(LedgerSide.DEBIT);

    private final LedgerSide normalSide;

    LedgerAccountClass(LedgerSide normalSide) {
        this.normalSide = normalSide;
    }

    public LedgerSide normalSide() {
        return normalSide;
    }
}
