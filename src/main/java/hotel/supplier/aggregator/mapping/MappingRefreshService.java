package hotel.supplier.aggregator.mapping;

import hotel.supplier.aggregator.domain.CatalogEntry;
import hotel.supplier.aggregator.domain.SupplierType;
import hotel.supplier.aggregator.supplier.SupplierAdapter;
import hotel.supplier.aggregator.supplier.error.SupplierAdapterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// 앱 기동 시 1회 호출되는 것을 기본으로 하되(MappingStartupInitializer), 스케줄러 등 다른 호출부를 추가하기 쉽도록 별도 서비스로 분리했다 (DESIGN.md 2번 참조).
@Service
@Profile("!mock")
public class MappingRefreshService {

    private static final Logger log = LoggerFactory.getLogger(MappingRefreshService.class);

    private final List<SupplierAdapter> adapters;
    private final StayMappingRepository stayMappingRepository;
    private final RoomTypeMappingRepository roomTypeMappingRepository;

    public MappingRefreshService(
            List<SupplierAdapter> adapters,
            StayMappingRepository stayMappingRepository,
            RoomTypeMappingRepository roomTypeMappingRepository) {
        this.adapters = adapters;
        this.stayMappingRepository = stayMappingRepository;
        this.roomTypeMappingRepository = roomTypeMappingRepository;
    }

    public void refreshMapping() {
        for (SupplierAdapter adapter : adapters) {
            try {
                refreshMapping(adapter);
            } catch (SupplierAdapterException e) {
                // 일부 공급사의 숙소 목록 조회 실패가 앱 기동을 막지 않도록, 이 공급사만 건너뛰고 계속 진행한다 (DESIGN.md 2번 참조).
                log.warn("공급사 {} 매핑 생성 실패, 이 공급사는 건너뛴다: {}", adapter.getSupplierType(), e.getMessage());
            }
        }
    }

    @Transactional
    void refreshMapping(SupplierAdapter adapter) {
        SupplierType supplierType = adapter.getSupplierType();
        List<CatalogEntry> catalog = adapter.fetchCatalog();
        for (CatalogEntry entry : catalog) {
            StayMapping stayMapping = findOrCreateStayMapping(supplierType, entry);
            saveRoomTypeMappingIfAbsent(stayMapping, supplierType, entry);
        }
    }

    private StayMapping findOrCreateStayMapping(SupplierType supplierType, CatalogEntry entry) {
        return stayMappingRepository
                .findBySupplierTypeAndSupplierHotelCode(supplierType, entry.supplierHotelCode())
                .orElseGet(() -> stayMappingRepository.save(
                        new StayMapping(supplierType, entry.supplierHotelCode(), entry.hotelName())));
    }

    private void saveRoomTypeMappingIfAbsent(StayMapping stayMapping, SupplierType supplierType, CatalogEntry entry) {
        boolean exists = roomTypeMappingRepository.existsBySupplierTypeAndSupplierHotelCodeAndSupplierRoomTypeCode(
                supplierType, entry.supplierHotelCode(), entry.supplierRoomTypeCode());
        if (exists) {
            return;
        }
        roomTypeMappingRepository.save(new RoomTypeMapping(
                stayMapping, supplierType, entry.supplierHotelCode(), entry.supplierRoomTypeCode(), entry.roomTypeName()));
    }
}
