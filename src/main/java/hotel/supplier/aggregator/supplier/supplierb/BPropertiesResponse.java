package hotel.supplier.aggregator.supplier.supplierb;

import java.util.List;

record BPropertiesResponse(String resultCode, String resultMessage, Data data) {

    record Data(List<PropertyItem> items) {
    }

    record PropertyItem(String propertyId, String propertyName, List<RoomItem> rooms) {
    }

    record RoomItem(String roomId, String roomName, int maxOccupancy) {
    }
}
