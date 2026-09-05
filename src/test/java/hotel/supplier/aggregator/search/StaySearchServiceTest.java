package hotel.supplier.aggregator.search;

import hotel.supplier.aggregator.domain.Money;
import hotel.supplier.aggregator.domain.StandardRoomOffer;
import hotel.supplier.aggregator.domain.SupplierType;
import hotel.supplier.aggregator.mapping.entity.StayMapping;
import hotel.supplier.aggregator.mapping.repository.StayMappingRepository;
import hotel.supplier.aggregator.supplier.SupplierAdapter;
import hotel.supplier.aggregator.supplier.error.SupplierAdapterException;
import hotel.supplier.aggregator.supplier.error.SupplierErrorCode;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StaySearchServiceTest {

    private final StayMappingRepository stayMappingRepository = mock(StayMappingRepository.class);
    private final Executor directExecutor = Runnable::run;
    private final LocalDate checkIn = LocalDate.of(2026, 1, 1);
    private final LocalDate checkOut = LocalDate.of(2026, 1, 2);

    @Test
    void 공급사별_보유_숙소로_그룹핑해서_조회한다() {
        SupplierAdapter adapterA = mock(SupplierAdapter.class);
        when(adapterA.getSupplierType()).thenReturn(SupplierType.SUPPLIER_A);
        when(stayMappingRepository.findBySupplierType(SupplierType.SUPPLIER_A))
                .thenReturn(List.of(new StayMapping(SupplierType.SUPPLIER_A, "A-1", "Hotel A")));
        StandardRoomOffer offerA = new StandardRoomOffer(
                SupplierType.SUPPLIER_A, "A-1", "RT-1", "Hotel A", "Standard", 2, false, new Money(100_000, "KRW"), 3);
        when(adapterA.fetchAvailability(eq(List.of("A-1")), eq(checkIn), eq(checkOut), eq(2), eq(0)))
                .thenReturn(List.of(offerA));

        SupplierAdapter adapterB = mock(SupplierAdapter.class);
        when(adapterB.getSupplierType()).thenReturn(SupplierType.SUPPLIER_B);
        when(stayMappingRepository.findBySupplierType(SupplierType.SUPPLIER_B)).thenReturn(List.of());

        StaySearchService service = new StaySearchService(
                List.of(adapterA, adapterB), stayMappingRepository, directExecutor);

        SearchResult result = service.search(checkIn, checkOut, 2, 0);

        assertThat(result.offers()).containsExactly(offerA);
        assertThat(result.partialFailures()).isEmpty();
        verify(adapterB, never()).fetchAvailability(anyList(), eq(checkIn), eq(checkOut), eq(2), eq(0));
    }

    @Test
    void 보유_숙소가_50개를_넘으면_배치로_나눠_호출한다() {
        SupplierAdapter adapter = mock(SupplierAdapter.class);
        when(adapter.getSupplierType()).thenReturn(SupplierType.SUPPLIER_A);
        List<StayMapping> mappings = new ArrayList<>();
        for (int i = 0; i < 120; i++) {
            mappings.add(new StayMapping(SupplierType.SUPPLIER_A, "A-" + i, "Hotel " + i));
        }
        when(stayMappingRepository.findBySupplierType(SupplierType.SUPPLIER_A)).thenReturn(mappings);
        when(adapter.fetchAvailability(anyList(), eq(checkIn), eq(checkOut), eq(2), eq(0)))
                .thenReturn(List.of());

        StaySearchService service = new StaySearchService(List.of(adapter), stayMappingRepository, directExecutor);
        service.search(checkIn, checkOut, 2, 0);

        verify(adapter, times(3)).fetchAvailability(anyList(), eq(checkIn), eq(checkOut), eq(2), eq(0));
    }

    @Test
    void 한_공급사가_실패해도_나머지_결과는_반환하고_부분_실패로_기록한다() {
        SupplierAdapter failingAdapter = mock(SupplierAdapter.class);
        when(failingAdapter.getSupplierType()).thenReturn(SupplierType.SUPPLIER_A);
        when(stayMappingRepository.findBySupplierType(SupplierType.SUPPLIER_A))
                .thenReturn(List.of(new StayMapping(SupplierType.SUPPLIER_A, "A-1", "Hotel A")));
        when(failingAdapter.fetchAvailability(anyList(), eq(checkIn), eq(checkOut), eq(2), eq(0)))
                .thenThrow(new SupplierAdapterException(SupplierType.SUPPLIER_A, SupplierErrorCode.TIMEOUT, "실패"));

        SupplierAdapter workingAdapter = mock(SupplierAdapter.class);
        when(workingAdapter.getSupplierType()).thenReturn(SupplierType.SUPPLIER_B);
        when(stayMappingRepository.findBySupplierType(SupplierType.SUPPLIER_B))
                .thenReturn(List.of(new StayMapping(SupplierType.SUPPLIER_B, "B-1", "Hotel B")));
        StandardRoomOffer offerB = new StandardRoomOffer(
                SupplierType.SUPPLIER_B, "B-1", "RT-2", "Hotel B", "Standard", 2, true, new Money(200_000, "KRW"), 4);
        when(workingAdapter.fetchAvailability(anyList(), eq(checkIn), eq(checkOut), eq(2), eq(0)))
                .thenReturn(List.of(offerB));

        StaySearchService service = new StaySearchService(
                List.of(failingAdapter, workingAdapter), stayMappingRepository, directExecutor);

        SearchResult result = service.search(checkIn, checkOut, 2, 0);

        assertThat(result.offers()).containsExactly(offerB);
        assertThat(result.partialFailures()).hasSize(1);
        assertThat(result.partialFailures().get(0).supplierType()).isEqualTo(SupplierType.SUPPLIER_A);
        assertThat(result.partialFailures().get(0).errorCode()).isEqualTo(SupplierErrorCode.TIMEOUT);
    }
}
