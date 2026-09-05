package hotel.supplier.aggregator.mapping.service;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

// 매핑 갱신 시점: 앱 기동 시 1회 (DESIGN.md 2번 참조).
@Component
@Profile("!mock")
class MappingStartupInitializer implements ApplicationRunner {

    private final MappingRefreshService mappingRefreshService;

    MappingStartupInitializer(MappingRefreshService mappingRefreshService) {
        this.mappingRefreshService = mappingRefreshService;
    }

    @Override
    public void run(ApplicationArguments args) {
        mappingRefreshService.refreshMapping();
    }
}
