package kz.sim.bank.gateway.bcc;

import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "open-banking.provider", havingValue = "bcc")
class BccConfiguration {
    @Bean
    BccProperties bccProperties(
            @Value("${bcc.token-url}") String tokenUrl,
            @Value("${bcc.financial-base-url}") String financialBaseUrl,
            @Value("${bcc.client-id}") String clientId,
            @Value("${bcc.client-secret}") String clientSecret,
            @Value("${bcc.app-id}") String appId,
            @Value("${bcc.scope}") String scope) {
        return new BccProperties(tokenUrl, financialBaseUrl, clientId, clientSecret, appId, scope);
    }

    @Bean
    BccOpenBankingProvider bccOpenBankingProvider(BccProperties properties) {
        var requestFactory = new JdkClientHttpRequestFactory();
        requestFactory.setReadTimeout(java.time.Duration.ofSeconds(10));
        return new BccOpenBankingProvider(properties,
                RestClient.builder().requestFactory(requestFactory).build(),
                RestClient.builder().requestFactory(requestFactory).build(), Clock.systemUTC());
    }
}
