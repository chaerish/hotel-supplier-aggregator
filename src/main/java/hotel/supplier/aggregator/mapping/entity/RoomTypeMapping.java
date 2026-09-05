package hotel.supplier.aggregator.mapping.entity;

import hotel.supplier.aggregator.domain.SupplierType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "room_type_mapping", uniqueConstraints =
        @UniqueConstraint(columnNames = {"supplier_code", "supplier_hotel_code", "supplier_room_type_code"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoomTypeMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "internal_stay_id", nullable = false)
    private StayMapping stayMapping;

    @Enumerated(EnumType.STRING)
    @Column(name = "supplier_code", nullable = false)
    private SupplierType supplierType;

    // 객실 타입 코드가 숙소 안에서만 유일하므로 함께 필요 (DESIGN.md 2번 참조)
    @Column(name = "supplier_hotel_code", nullable = false)
    private String supplierHotelCode;

    @Column(name = "supplier_room_type_code", nullable = false)
    private String supplierRoomTypeCode;

    @Column(name = "room_type_name", nullable = false)
    private String roomTypeName;

    public RoomTypeMapping(
            StayMapping stayMapping,
            SupplierType supplierType,
            String supplierHotelCode,
            String supplierRoomTypeCode,
            String roomTypeName) {
        this.stayMapping = stayMapping;
        this.supplierType = supplierType;
        this.supplierHotelCode = supplierHotelCode;
        this.supplierRoomTypeCode = supplierRoomTypeCode;
        this.roomTypeName = roomTypeName;
    }
}
