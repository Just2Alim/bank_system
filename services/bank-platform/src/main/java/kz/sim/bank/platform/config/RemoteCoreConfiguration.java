package kz.sim.bank.platform.config;

import kz.sim.bank.platform.adapter.remote.RemoteCoreBankingAdapter;
import kz.sim.bank.platform.core.api.CoreBankingPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "bank.core.mode", havingValue = "remote")
public class RemoteCoreConfiguration {

    @Bean
    CoreBankingPort remoteCoreBankingPort(
            RestClient.Builder builder,
            @Value("${bank.core.remote.base-url}") String baseUrl) {
        var requestFactory = new JdkClientHttpRequestFactory();
        requestFactory.setReadTimeout(java.time.Duration.ofSeconds(5));
        RestClient client = builder
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
        return new RemoteCoreBankingAdapter(client);
    }
}
