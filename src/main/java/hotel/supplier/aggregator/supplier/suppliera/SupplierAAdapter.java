package hotel.supplier.aggregator.supplier.suppliera;

import hotel.supplier.aggregator.domain.CatalogEntry;
import hotel.supplier.aggregator.domain.StandardRoomOffer;
import hotel.supplier.aggregator.domain.SupplierType;
import hotel.supplier.aggregator.supplier.SupplierAdapter;
import hotel.supplier.aggregator.supplier.SupplierAdapterException;
import hotel.supplier.aggregator.supplier.SupplierErrorCode;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.LocalDate;
import java.util.List;

@Component
public class SupplierAAdapter implements SupplierAdapter {

    private final WebClient webClient;
    private final SupplierAMapper mapper = new SupplierAMapper();

    public SupplierAAdapter(@Qualifier("supplierAWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    @Override
    public SupplierType getSupplierType() {
        return SupplierType.SUPPLIER_A;
    }

    @Override
    public List<CatalogEntry> fetchCatalog() {
        AHotelsResponse response = webClient.get()
                .uri("/a/v1/hotels")
                .retrieve()
                .bodyToMono(AHotelsResponse.class)
                .onErrorMap(this::toSupplierException)
                .block();
        return mapper.toCatalogEntries(response);
    }

    @Override
    public List<StandardRoomOffer> fetchAvailability(
            List<String> hotelCodes, LocalDate checkIn, LocalDate checkOut, int adults, int children) {
        AAvailabilityResponse response = webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/a/v1/availability")
                        .queryParam("hotelCodes", String.join(",", hotelCodes))
                        .queryParam("checkIn", checkIn)
                        .queryParam("checkOut", checkOut)
                        .queryParam("adults", adults)
                        .queryParam("children", children)
                        .build())
                .retrieve()
                .bodyToMono(AAvailabilityResponse.class)
                .onErrorMap(this::toSupplierException)
                .block();
        return mapper.toStandardRoomOffers(response);
    }

    private Throwable toSupplierException(Throwable error) {
        if (error instanceof SupplierAdapterException) {
            return error;
        }
        if (error instanceof WebClientResponseException e) {
            return new SupplierAdapterException(
                    SupplierType.SUPPLIER_A,
                    SupplierErrorCode.TEMPORARILY_UNAVAILABLE,
                    "Supplier A 응답 실패: HTTP " + e.getStatusCode(),
                    e);
        }
        if (error instanceof WebClientRequestException e) {
            return new SupplierAdapterException(
                    SupplierType.SUPPLIER_A,
                    SupplierErrorCode.TIMEOUT,
                    "Supplier A 요청 실패(연결/응답 타임아웃 포함): " + e.getMessage(),
                    e);
        }
        return new SupplierAdapterException(
                SupplierType.SUPPLIER_A,
                SupplierErrorCode.UNKNOWN,
                "Supplier A 알 수 없는 실패: " + error.getMessage(),
                error);
    }
}
