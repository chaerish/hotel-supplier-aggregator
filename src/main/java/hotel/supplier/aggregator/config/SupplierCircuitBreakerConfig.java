package hotel.supplier.aggregator.config;

import hotel.supplier.aggregator.supplier.error.SupplierAdapterException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

// resilience4j-circuitbreaker 코어 모듈만 사용하고, 오토컨피그 스타터는 쓰지 않는다 (DESIGN.md 5번 참조).
// Supplier 두 곳뿐인 이 구현 규모에 맞춰, 기본값(슬라이딩 윈도우 100)보다 훨씬 작은 값으로 낮췄다.
@Configuration
class SupplierCircuitBreakerConfig {

    @Bean
    CircuitBreakerRegistry supplierCircuitBreakerRegistry(
            @Value("${supplier.circuit-breaker.sliding-window-size}") int slidingWindowSize,
            @Value("${supplier.circuit-breaker.minimum-number-of-calls}") int minimumNumberOfCalls,
            @Value("${supplier.circuit-breaker.failure-rate-threshold}") float failureRateThreshold,
            @Value("${supplier.circuit-breaker.wait-duration-in-open-state-seconds}") long waitDurationInOpenStateSeconds,
            @Value("${supplier.circuit-breaker.permitted-calls-in-half-open-state}") int permittedCallsInHalfOpenState) {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slidingWindowSize(slidingWindowSize)
                .minimumNumberOfCalls(minimumNumberOfCalls)
                .failureRateThreshold(failureRateThreshold)
                .waitDurationInOpenState(Duration.ofSeconds(waitDurationInOpenStateSeconds))
                .permittedNumberOfCallsInHalfOpenState(permittedCallsInHalfOpenState)
                .recordException(t -> t instanceof SupplierAdapterException)
                .build();
        return CircuitBreakerRegistry.of(config);
    }
}
