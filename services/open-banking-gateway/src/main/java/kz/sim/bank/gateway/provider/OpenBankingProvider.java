package kz.sim.bank.gateway.provider;

import java.util.List;

public interface OpenBankingProvider {
    List<AccountView> accounts();
    List<TransactionView> transactions(int limit);
    TransferReceipt transfer(TransferCommand command);
    ProviderStatus status();

    record ProviderStatus(String provider, String connection, String writeMode, String detail) {}
}
