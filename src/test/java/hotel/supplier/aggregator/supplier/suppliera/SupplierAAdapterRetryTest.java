package hotel.supplier.aggregator.supplier.suppliera;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import hotel.supplier.aggregator.supplier.SupplierAdapter;
import hotel.supplier.aggregator.supplier.error.SupplierAdapterException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.resilience.annotation.EnableResilientMethods;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// @Retryable은 Spring AOP 프록시로 동작하기 때문에, new로 직접 생성하는 SupplierAAdapterTest와 달리
// 실제 스프링 컨텍스트를 띄워 프록시가 적용된 빈으로 검증한다 (DESIGN.md 5번 참조).
class SupplierAAdapterRetryTest {

    private HttpServer server;
    private AnnotationConfigApplicationContext context;
    private final AtomicInteger requestCount = new AtomicInteger();

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.start();
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
        if (context != null) {
            context.close();
        }
    }

    @Test
    void 첫_호출이_실패해도_재시도해서_성공한다() throws IOException {
        server.createContext("/a/v1/availability", exchange -> {
            if (requestCount.getAndIncrement() == 0) {
                exchange.sendResponseHeaders(503, -1);
                exchange.close();
                return;
            }
            respond(exchange, "{\"items\":[]}");
        });

        SupplierAdapter adapter = startContextAndGetAdapter();

        List<?> offers = adapter.fetchAvailability(
                List.of("A-1"), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 2), 2, 0);

        assertThat(offers).isEmpty();
        assertThat(requestCount.get()).isEqualTo(2);
    }

    @Test
    void 계속_실패하면_maxRetries만큼만_재시도하고_예외를_던진다() {
        server.createContext("/a/v1/availability", exchange -> {
            requestCount.incrementAndGet();
            try {
                exchange.sendResponseHeaders(503, -1);
            } finally {
                exchange.close();
            }
        });

        SupplierAdapter adapter = startContextAndGetAdapter();

        assertThatThrownBy(() -> adapter.fetchAvailability(
                List.of("A-1"), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 2), 2, 0))
                .isInstanceOf(SupplierAdapterException.class);

        assertThat(requestCount.get()).isEqualTo(2); // 최초 1회 + maxRetries(1)회
    }

    private SupplierAdapter startContextAndGetAdapter() {
        WebClient webClient = WebClient.builder()
                .baseUrl("http://localhost:" + server.getAddress().getPort())
                .build();

        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        Properties props = new Properties();
        props.setProperty("supplier.retry.max-retries", "1");
        props.setProperty("supplier.retry.delay-ms", "10");
        ctx.getEnvironment().getPropertySources().addFirst(new PropertiesPropertySource("test-retry", props));
        ctx.register(RetryEnablingConfig.class);
        ctx.registerBean(SupplierAAdapter.class, () -> new SupplierAAdapter(webClient));
        ctx.refresh();

        context = ctx;
        return ctx.getBean(SupplierAdapter.class);
    }

    private void respond(HttpExchange exchange, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    @Configuration
    @EnableResilientMethods
    static class RetryEnablingConfig {

        @Bean
        static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
            return new PropertySourcesPlaceholderConfigurer();
        }
    }
}
