package hotel.supplier.aggregator.supplier.supplierb;

import java.time.LocalDate;
import java.util.List;

record BSearchResponse(String resultCode, String resultMessage, Data data) {

    record Data(List<SearchItem> items) {
    }

    record SearchItem(
            String propertyId,
            String propertyName,
            String roomId,
            String roomName,
            int maxOccupancy,
            boolean breakfastIncluded,
            String currency,
            long totalPrice,
            boolean taxIncluded,
            List<InventoryItem> inventory
    ) {
    }

    record InventoryItem(LocalDate date, int remainingRooms) {
    }
}
