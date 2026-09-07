package hotel.supplier.aggregator.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.resilience.annotation.EnableResilientMethods;

// Spring Framework 7에 내장된 @Retryable 처리를 활성화한다 (DESIGN.md 5번 참조).
@Configuration
@EnableResilientMethods
class ResilienceConfig {
}
