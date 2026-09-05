package hotel.supplier.aggregator.supplier;

import hotel.supplier.aggregator.domain.CatalogEntry;
import hotel.supplier.aggregator.domain.StandardRoomOffer;
import hotel.supplier.aggregator.domain.SupplierType;

import java.time.LocalDate;
import java.util.List;

public interface SupplierAdapter {

    SupplierType getSupplierType();

    List<StandardRoomOffer> fetchAvailability(
            List<String> hotelCodes, LocalDate checkIn, LocalDate checkOut, int adults, int children);

    List<CatalogEntry> fetchCatalog();
}
