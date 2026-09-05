package hotel.supplier.aggregator.mapping;

import hotel.supplier.aggregator.domain.CatalogEntry;
import hotel.supplier.aggregator.domain.SupplierType;
import hotel.supplier.aggregator.supplier.SupplierAdapter;
import hotel.supplier.aggregator.supplier.error.SupplierAdapterException;
import hotel.supplier.aggregator.supplier.error.SupplierErrorCode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MappingRefreshServiceTest {

    private final StayMappingRepository stayMappingRepository = mock(StayMappingRepository.class);
    private final RoomTypeMappingRepository roomTypeMappingRepository = mock(RoomTypeMappingRepository.class);

    @Test
    void 신규_숙소는_매핑을_새로_저장한다() {
        SupplierAdapter adapterA = mock(SupplierAdapter.class);
        when(adapterA.getSupplierType()).thenReturn(SupplierType.SUPPLIER_A);
        when(adapterA.fetchCatalog()).thenReturn(List.of(
                new CatalogEntry(SupplierType.SUPPLIER_A, "A-1", "Test Hotel", "RT-1", "Standard", 2)));
        when(stayMappingRepository.findBySupplierTypeAndSupplierHotelCode(SupplierType.SUPPLIER_A, "A-1"))
                .thenReturn(Optional.empty());
        StayMapping saved = new StayMapping(SupplierType.SUPPLIER_A, "A-1", "Test Hotel");
        when(stayMappingRepository.save(any())).thenReturn(saved);
        when(roomTypeMappingRepository.existsBySupplierTypeAndSupplierHotelCodeAndSupplierRoomTypeCode(
                SupplierType.SUPPLIER_A, "A-1", "RT-1")).thenReturn(false);

        MappingRefreshService service = new MappingRefreshService(
                List.of(adapterA), stayMappingRepository, roomTypeMappingRepository);
        service.refreshMapping();

        verify(stayMappingRepository, times(1)).save(any());
        verify(roomTypeMappingRepository, times(1)).save(any());
    }

    @Test
    void 이미_있는_객실타입_매핑은_다시_저장하지_않는다() {
        SupplierAdapter adapterA = mock(SupplierAdapter.class);
        when(adapterA.getSupplierType()).thenReturn(SupplierType.SUPPLIER_A);
        when(adapterA.fetchCatalog()).thenReturn(List.of(
                new CatalogEntry(SupplierType.SUPPLIER_A, "A-1", "Test Hotel", "RT-1", "Standard", 2)));
        StayMapping existingStay = new StayMapping(SupplierType.SUPPLIER_A, "A-1", "Test Hotel");
        when(stayMappingRepository.findBySupplierTypeAndSupplierHotelCode(SupplierType.SUPPLIER_A, "A-1"))
                .thenReturn(Optional.of(existingStay));
        when(roomTypeMappingRepository.existsBySupplierTypeAndSupplierHotelCodeAndSupplierRoomTypeCode(
                SupplierType.SUPPLIER_A, "A-1", "RT-1")).thenReturn(true);

        MappingRefreshService service = new MappingRefreshService(
                List.of(adapterA), stayMappingRepository, roomTypeMappingRepository);
        service.refreshMapping();

        verify(stayMappingRepository, never()).save(any());
        verify(roomTypeMappingRepository, never()).save(any());
    }

    @Test
    void 한_공급사가_실패해도_나머지_공급사는_계속_처리한다() {
        SupplierAdapter failingAdapter = mock(SupplierAdapter.class);
        when(failingAdapter.getSupplierType()).thenReturn(SupplierType.SUPPLIER_A);
        when(failingAdapter.fetchCatalog()).thenThrow(new SupplierAdapterException(
                SupplierType.SUPPLIER_A, SupplierErrorCode.TEMPORARILY_UNAVAILABLE, "실패"));

        SupplierAdapter workingAdapter = mock(SupplierAdapter.class);
        when(workingAdapter.getSupplierType()).thenReturn(SupplierType.SUPPLIER_B);
        when(workingAdapter.fetchCatalog()).thenReturn(List.of(
                new CatalogEntry(SupplierType.SUPPLIER_B, "B-1", "Test Hotel", "RT-2", "Standard", 2)));
        when(stayMappingRepository.findBySupplierTypeAndSupplierHotelCode(SupplierType.SUPPLIER_B, "B-1"))
                .thenReturn(Optional.empty());
        when(stayMappingRepository.save(any()))
                .thenReturn(new StayMapping(SupplierType.SUPPLIER_B, "B-1", "Test Hotel"));
        when(roomTypeMappingRepository.existsBySupplierTypeAndSupplierHotelCodeAndSupplierRoomTypeCode(
                SupplierType.SUPPLIER_B, "B-1", "RT-2")).thenReturn(false);

        MappingRefreshService service = new MappingRefreshService(
                List.of(failingAdapter, workingAdapter), stayMappingRepository, roomTypeMappingRepository);
        service.refreshMapping();

        verify(stayMappingRepository, times(1)).save(any());
        verify(roomTypeMappingRepository, times(1)).save(any());
    }
}
