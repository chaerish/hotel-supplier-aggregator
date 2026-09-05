package hotel.supplier.aggregator.domain;

public record CatalogEntry(
        SupplierType supplierType,
        String supplierHotelCode,
        String hotelName,
        String supplierRoomTypeCode,
        String roomTypeName,
        int maxOccupancy
) {
}
