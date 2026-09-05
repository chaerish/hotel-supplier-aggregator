package hotel.supplier.aggregator.mapping;

import hotel.supplier.aggregator.domain.SupplierType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "stay_mapping", uniqueConstraints =
        @UniqueConstraint(columnNames = {"supplier_code", "supplier_hotel_code"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StayMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "supplier_code", nullable = false)
    private SupplierType supplierType;

    @Column(name = "supplier_hotel_code", nullable = false)
    private String supplierHotelCode;

    @Column(name = "stay_name", nullable = false)
    private String stayName;

    public StayMapping(SupplierType supplierType, String supplierHotelCode, String stayName) {
        this.supplierType = supplierType;
        this.supplierHotelCode = supplierHotelCode;
        this.stayName = stayName;
    }

    public Long getId() {
        return id;
    }

    public SupplierType getSupplierType() {
        return supplierType;
    }

    public String getSupplierHotelCode() {
        return supplierHotelCode;
    }

    public String getStayName() {
        return stayName;
    }
}
