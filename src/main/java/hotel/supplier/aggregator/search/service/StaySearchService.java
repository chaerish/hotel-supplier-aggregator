package hotel.supplier.aggregator.search.service;

import hotel.supplier.aggregator.domain.StandardRoomOffer;
import hotel.supplier.aggregator.domain.SupplierType;
import hotel.supplier.aggregator.mapping.entity.StayMapping;
import hotel.supplier.aggregator.mapping.repository.StayMappingRepository;
import hotel.supplier.aggregator.search.dto.PartialFailure;
import hotel.supplier.aggregator.search.dto.SearchResponse;
import hotel.supplier.aggregator.supplier.SupplierAdapter;
import hotel.supplier.aggregator.supplier.error.SupplierAdapterException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Service
@Profile("!mock")
public class StaySearchService {

    // 재고와 요금 조회 API의 1회 최대 숙소 코드 개수 제약 (DESIGN.md 2번 참조).
    private static final int MAX_HOTEL_CODES_PER_BATCH = 50;

    private final List<SupplierAdapter> adapters;
    private final StayMappingRepository stayMappingRepository;
    private final Executor supplierSearchExecutor;

    public StaySearchService(
            List<SupplierAdapter> adapters,
            StayMappingRepository stayMappingRepository,
            @Qualifier("supplierSearchExecutor") Executor supplierSearchExecutor) {
        this.adapters = adapters;
        this.stayMappingRepository = stayMappingRepository;
        this.supplierSearchExecutor = supplierSearchExecutor;
    }

    public SearchResponse search(LocalDate checkIn, LocalDate checkOut, int adults, int children) {
        List<CompletableFuture<BatchResult>> futures = new ArrayList<>();
        for (SupplierAdapter adapter : adapters) {
            List<String> hotelCodes = stayMappingRepository.findBySupplierType(adapter.getSupplierType()).stream()
                    .map(StayMapping::getSupplierHotelCode)
                    .toList();
            for (List<String> batch : partition(hotelCodes, MAX_HOTEL_CODES_PER_BATCH)) {
                futures.add(CompletableFuture.supplyAsync(
                        () -> fetchBatch(adapter, batch, checkIn, checkOut, adults, children),
                        supplierSearchExecutor));
            }
        }

        List<StandardRoomOffer> offers = new ArrayList<>();
        List<PartialFailure> partialFailures = new ArrayList<>();
        for (CompletableFuture<BatchResult> future : futures) {
            BatchResult result = future.join();
            offers.addAll(result.offers());
            if (result.failure() != null) {
                partialFailures.add(result.failure());
            }
        }
        return new SearchResponse(offers, partialFailures);
    }

    private BatchResult fetchBatch(
            SupplierAdapter adapter,
            List<String> hotelCodes,
            LocalDate checkIn,
            LocalDate checkOut,
            int adults,
            int children) {
        try {
            List<StandardRoomOffer> offers = adapter.fetchAvailability(hotelCodes, checkIn, checkOut, adults, children);
            return new BatchResult(offers, null);
        } catch (SupplierAdapterException e) {
            SupplierType supplierType = adapter.getSupplierType();
            return new BatchResult(List.of(), new PartialFailure(supplierType, e.errorCode(), e.getMessage()));
        }
    }

    private static List<List<String>> partition(List<String> items, int size) {
        List<List<String>> batches = new ArrayList<>();
        for (int i = 0; i < items.size(); i += size) {
            batches.add(items.subList(i, Math.min(i + size, items.size())));
        }
        return batches;
    }

    private record BatchResult(List<StandardRoomOffer> offers, PartialFailure failure) {
    }
}
