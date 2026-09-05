package hotel.supplier.aggregator.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
class WebClientConfig {

    @Bean
    @Qualifier("supplierAWebClient")
    WebClient supplierAWebClient(
            @Value("${supplier.a.base-url}") String baseUrl,
            @Value("${supplier.connect-timeout-ms}") int connectTimeoutMs,
            @Value("${supplier.response-timeout-ms}") int responseTimeoutMs) {
        return buildWebClient(baseUrl, connectTimeoutMs, responseTimeoutMs);
    }

    @Bean
    @Qualifier("supplierBWebClient")
    WebClient supplierBWebClient(
            @Value("${supplier.b.base-url}") String baseUrl,
            @Value("${supplier.connect-timeout-ms}") int connectTimeoutMs,
            @Value("${supplier.response-timeout-ms}") int responseTimeoutMs) {
        return buildWebClient(baseUrl, connectTimeoutMs, responseTimeoutMs);
    }

    private WebClient buildWebClient(String baseUrl, int connectTimeoutMs, int responseTimeoutMs) {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutMs)
                .responseTimeout(Duration.ofMillis(responseTimeoutMs))
                .doOnConnected(connection -> connection
                        .addHandlerLast(new ReadTimeoutHandler(responseTimeoutMs, TimeUnit.MILLISECONDS)));

        return WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}
