package hotel.supplier.aggregator.supplier.suppliera;

import hotel.supplier.aggregator.domain.CatalogEntry;
import hotel.supplier.aggregator.domain.StandardRoomOffer;
import hotel.supplier.aggregator.domain.SupplierType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SupplierAMapperTest {

    private final SupplierAMapper mapper = new SupplierAMapper();

    @Test
    void 숙소_목록을_표준_카탈로그로_변환한다() {
        var response = new AHotelsResponse(List.of(
                new AHotelsResponse.HotelItem("TEST-H1", "Test Hotel One", List.of(
                        new AHotelsResponse.RoomTypeItem("RT-1", "Room Type One", 2),
                        new AHotelsResponse.RoomTypeItem("RT-2", "Room Type Two", 4)
                ))
        ));

        List<CatalogEntry> entries = mapper.toCatalogEntries(response);

        assertThat(entries).containsExactly(
                new CatalogEntry(SupplierType.SUPPLIER_A, "TEST-H1", "Test Hotel One", "RT-1", "Room Type One", 2),
                new CatalogEntry(SupplierType.SUPPLIER_A, "TEST-H1", "Test Hotel One", "RT-2", "Room Type Two", 4)
        );
    }

    @Test
    void 요금은_날짜별_합산_재고는_최솟값으로_변환한다() {
        var response = new AAvailabilityResponse(List.of(
                new AAvailabilityResponse.AvailabilityItem(
                        "TEST-H1", "Test Hotel One", "RT-1", "Room Type One",
                        2, true, "KRW",
                        List.of(
                                new AAvailabilityResponse.DailyRate(LocalDate.of(2026, 1, 1), 5, 50_000, 5_000),
                                new AAvailabilityResponse.DailyRate(LocalDate.of(2026, 1, 2), 2, 60_000, 6_000),
                                new AAvailabilityResponse.DailyRate(LocalDate.of(2026, 1, 3), 8, 50_000, 5_000)
                        )
                )
        ));

        List<StandardRoomOffer> offers = mapper.toStandardRoomOffers(response);

        assertThat(offers).hasSize(1);
        StandardRoomOffer offer = offers.get(0);
        assertThat(offer.supplierType()).isEqualTo(SupplierType.SUPPLIER_A);
        assertThat(offer.supplierHotelCode()).isEqualTo("TEST-H1");
        assertThat(offer.supplierRoomTypeCode()).isEqualTo("RT-1");
        assertThat(offer.breakfastIncluded()).isTrue();
        assertThat(offer.totalPrice().amount()).isEqualTo(176_000L); // (50000+5000)+(60000+6000)+(50000+5000)
        assertThat(offer.totalPrice().currency()).isEqualTo("KRW");
        assertThat(offer.availableRooms()).isEqualTo(2); // min(5, 2, 8)
    }
}
