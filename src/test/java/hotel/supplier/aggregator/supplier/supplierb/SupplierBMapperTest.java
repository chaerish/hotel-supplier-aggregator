package hotel.supplier.aggregator.supplier.supplierb;

import hotel.supplier.aggregator.domain.CatalogEntry;
import hotel.supplier.aggregator.domain.StandardRoomOffer;
import hotel.supplier.aggregator.domain.SupplierType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SupplierBMapperTest {

    private final SupplierBMapper mapper = new SupplierBMapper();

    @Test
    void 숙소_목록을_표준_카탈로그로_변환한다() {
        var response = new BPropertiesResponse("0000", "SUCCESS", new BPropertiesResponse.Data(List.of(
                new BPropertiesResponse.PropertyItem("TEST-P1", "Test Property One", List.of(
                        new BPropertiesResponse.RoomItem("RM-1", "Room One", 2),
                        new BPropertiesResponse.RoomItem("RM-2", "Room Two", 3)
                ))
        )));

        List<CatalogEntry> entries = mapper.toCatalogEntries(response);

        assertThat(entries).containsExactly(
                new CatalogEntry(SupplierType.SUPPLIER_B, "TEST-P1", "Test Property One", "RM-1", "Room One", 2),
                new CatalogEntry(SupplierType.SUPPLIER_B, "TEST-P1", "Test Property One", "RM-2", "Room Two", 3)
        );
    }

    @Test
    void 기간_전체_요금은_그대로_재고는_최솟값으로_변환한다() {
        var response = new BSearchResponse("0000", "SUCCESS", new BSearchResponse.Data(List.of(
                new BSearchResponse.SearchItem(
                        "TEST-P1", "Test Property One", "RM-1", "Room One",
                        2, false, "KRW", 180_000, true,
                        List.of(
                                new BSearchResponse.InventoryItem(LocalDate.of(2026, 1, 1), 4),
                                new BSearchResponse.InventoryItem(LocalDate.of(2026, 1, 2), 1),
                                new BSearchResponse.InventoryItem(LocalDate.of(2026, 1, 3), 6)
                        )
                )
        )));

        List<StandardRoomOffer> offers = mapper.toStandardRoomOffers(response);

        assertThat(offers).hasSize(1);
        StandardRoomOffer offer = offers.get(0);
        assertThat(offer.supplierType()).isEqualTo(SupplierType.SUPPLIER_B);
        assertThat(offer.supplierHotelCode()).isEqualTo("TEST-P1");
        assertThat(offer.supplierRoomTypeCode()).isEqualTo("RM-1");
        assertThat(offer.totalPrice().amount()).isEqualTo(180_000L); // 이미 기간 전체 총액으로 주어짐
        assertThat(offer.totalPrice().currency()).isEqualTo("KRW");
        assertThat(offer.availableRooms()).isEqualTo(1); // min(4, 1, 6)
    }
}
