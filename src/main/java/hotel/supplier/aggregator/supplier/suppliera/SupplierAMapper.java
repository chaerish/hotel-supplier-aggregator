package hotel.supplier.aggregator.supplier.suppliera;

import hotel.supplier.aggregator.domain.CatalogEntry;
import hotel.supplier.aggregator.domain.Money;
import hotel.supplier.aggregator.domain.StandardRoomOffer;
import hotel.supplier.aggregator.domain.SupplierType;

import java.util.List;

class SupplierAMapper {

    List<CatalogEntry> toCatalogEntries(AHotelsResponse response) {
        return response.items().stream()
                .flatMap(hotel -> hotel.roomTypes().stream()
                        .map(roomType -> new CatalogEntry(
                                SupplierType.SUPPLIER_A,
                                hotel.hotelCode(),
                                hotel.hotelName(),
                                roomType.roomTypeCode(),
                                roomType.roomTypeName(),
                                roomType.maxOccupancy())))
                .toList();
    }

    List<StandardRoomOffer> toStandardRoomOffers(AAvailabilityResponse response) {
        return response.items().stream()
                .map(this::toStandardRoomOffer)
                .toList();
    }

    private StandardRoomOffer toStandardRoomOffer(AAvailabilityResponse.AvailabilityItem item) {
        long totalPrice = item.dailyRates().stream()
                .mapToLong(rate -> rate.nightlyRate() + rate.taxAmount())
                .sum();
        // 기간 중 하루라도 부족하면 그 방을 확실히 예약할 수 없으므로 최솟값을 노출한다 (DESIGN.md 1번 항목).
        int minAvailableRooms = item.dailyRates().stream()
                .mapToInt(AAvailabilityResponse.DailyRate::remainingRooms)
                .min()
                .orElse(0);

        return new StandardRoomOffer(
                SupplierType.SUPPLIER_A,
                item.hotelCode(),
                item.roomTypeCode(),
                item.hotelName(),
                item.roomTypeName(),
                item.maxOccupancy(),
                item.breakfastIncluded(),
                new Money(totalPrice, item.currency()),
                minAvailableRooms
        );
    }
}
