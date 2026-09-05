package hotel.supplier.aggregator.domain;

public record StandardRoomOffer(
        SupplierType supplierType,
        String supplierHotelCode,
        String supplierRoomTypeCode,
        String hotelName,
        String roomTypeName,
        int maxOccupancy,
        boolean breakfastIncluded,
        Money totalPrice,
        int availableRooms
) {
}
