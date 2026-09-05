package hotel.supplier.aggregator.supplier.suppliera;

import java.util.List;

record AHotelsResponse(List<HotelItem> items) {

    record HotelItem(String hotelCode, String hotelName, List<RoomTypeItem> roomTypes) {
    }

    record RoomTypeItem(String roomTypeCode, String roomTypeName, int maxOccupancy) {
    }
}
