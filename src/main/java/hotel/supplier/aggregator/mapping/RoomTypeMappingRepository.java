package hotel.supplier.aggregator.mapping;

import hotel.supplier.aggregator.domain.SupplierType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoomTypeMappingRepository extends JpaRepository<RoomTypeMapping, Long> {

    boolean existsBySupplierTypeAndSupplierHotelCodeAndSupplierRoomTypeCode(
            SupplierType supplierType, String supplierHotelCode, String supplierRoomTypeCode);

    List<RoomTypeMapping> findByStayMapping(StayMapping stayMapping);
}
