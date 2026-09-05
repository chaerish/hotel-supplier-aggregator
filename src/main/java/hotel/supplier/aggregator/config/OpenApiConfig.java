package hotel.supplier.aggregator.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!mock")
class OpenApiConfig {

    @Bean
    OpenAPI aggregatorOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Hotel Supplier Aggregator API")
                .description("공급사 A/B의 숙소 재고와 요금을 통합 조회하는 API")
                .version("v0.0.1"));
    }
}
