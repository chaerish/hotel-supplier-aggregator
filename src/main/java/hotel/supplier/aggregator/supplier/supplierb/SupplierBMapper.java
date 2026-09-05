package hotel.supplier.aggregator.supplier.supplierb;

import hotel.supplier.aggregator.domain.CatalogEntry;
import hotel.supplier.aggregator.domain.Money;
import hotel.supplier.aggregator.domain.StandardRoomOffer;
import hotel.supplier.aggregator.domain.SupplierType;

import java.util.List;

class SupplierBMapper {

    List<CatalogEntry> toCatalogEntries(BPropertiesResponse response) {
        return response.data().items().stream()
                .flatMap(property -> property.rooms().stream()
                        .map(room -> new CatalogEntry(
                                SupplierType.SUPPLIER_B,
                                property.propertyId(),
                                property.propertyName(),
                                room.roomId(),
                                room.roomName(),
                                room.maxOccupancy())))
                .toList();
    }

    List<StandardRoomOffer> toStandardRoomOffers(BSearchResponse response) {
        return response.data().items().stream()
                .map(this::toStandardRoomOffer)
                .toList();
    }

    private StandardRoomOffer toStandardRoomOffer(BSearchResponse.SearchItem item) {
        // 기간 중 하루라도 부족하면 그 방을 확실히 예약할 수 없으므로 최솟값을 노출한다 (DESIGN.md 1번 항목).
        int minAvailableRooms = item.inventory().stream()
                .mapToInt(BSearchResponse.InventoryItem::remainingRooms)
                .min()
                .orElse(0);

        return new StandardRoomOffer(
                SupplierType.SUPPLIER_B,
                item.propertyId(),
                item.roomId(),
                item.propertyName(),
                item.roomName(),
                item.maxOccupancy(),
                item.breakfastIncluded(),
                new Money(item.totalPrice(), item.currency()),
                minAvailableRooms
        );
    }
}
