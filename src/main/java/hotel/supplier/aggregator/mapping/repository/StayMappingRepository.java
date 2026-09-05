package hotel.supplier.aggregator.mapping.repository;

import hotel.supplier.aggregator.domain.SupplierType;
import hotel.supplier.aggregator.mapping.entity.StayMapping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StayMappingRepository extends JpaRepository<StayMapping, Long> {

    Optional<StayMapping> findBySupplierTypeAndSupplierHotelCode(SupplierType supplierType, String supplierHotelCode);

    List<StayMapping> findBySupplierType(SupplierType supplierType);
}
