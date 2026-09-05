package hotel.supplier.aggregator.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Configuration
class SearchExecutorConfig {

    // 공용 ForkJoinPool.commonPool()을 그대로 쓰면 다른 비동기 작업과 자원을 경합할 수 있어 전용 스레드풀을 둔다 (DESIGN.md 4번 참조).
    @Bean
    @Qualifier("supplierSearchExecutor")
    Executor supplierSearchExecutor() {
        return Executors.newFixedThreadPool(10);
    }
}
