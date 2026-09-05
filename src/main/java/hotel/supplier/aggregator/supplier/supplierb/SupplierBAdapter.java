package hotel.supplier.aggregator.supplier.supplierb;

import hotel.supplier.aggregator.domain.CatalogEntry;
import hotel.supplier.aggregator.domain.StandardRoomOffer;
import hotel.supplier.aggregator.domain.SupplierType;
import hotel.supplier.aggregator.supplier.SupplierAdapter;
import hotel.supplier.aggregator.supplier.error.SupplierAdapterException;
import hotel.supplier.aggregator.supplier.error.SupplierErrorCode;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.LocalDate;
import java.util.List;

@Component
public class SupplierBAdapter implements SupplierAdapter {

    private static final String SUCCESS_RESULT_CODE = "0000";

    private final WebClient webClient;
    private final SupplierBMapper mapper = new SupplierBMapper();

    public SupplierBAdapter(@Qualifier("supplierBWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    @Override
    public SupplierType getSupplierType() {
        return SupplierType.SUPPLIER_B;
    }

    @Override
    public List<CatalogEntry> fetchCatalog() {
        BPropertiesResponse response = webClient.get()
                .uri("/b/api/properties")
                .retrieve()
                .bodyToMono(BPropertiesResponse.class)
                .onErrorMap(this::toSupplierException)
                .block();
        ensureSuccess(response.resultCode(), response.resultMessage());
        return mapper.toCatalogEntries(response);
    }

    @Override
    public List<StandardRoomOffer> fetchAvailability(
            List<String> hotelCodes, LocalDate checkIn, LocalDate checkOut, int adults, int children) {
        BSearchResponse response = webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/b/api/search")
                        .queryParam("propertyIds", String.join(",", hotelCodes))
                        .queryParam("checkIn", checkIn)
                        .queryParam("checkOut", checkOut)
                        .queryParam("adults", adults)
                        .queryParam("children", children)
                        .build())
                .retrieve()
                .bodyToMono(BSearchResponse.class)
                .onErrorMap(this::toSupplierException)
                .block();
        ensureSuccess(response.resultCode(), response.resultMessage());
        return mapper.toStandardRoomOffers(response);
    }

    // Supplier B는 항상 HTTP 200으로 응답하고, 본문의 resultCode로 성공/실패를 구분한다.
    private void ensureSuccess(String resultCode, String resultMessage) {
        if (SUCCESS_RESULT_CODE.equals(resultCode)) {
            return;
        }
        throw new SupplierAdapterException(
                SupplierType.SUPPLIER_B,
                SupplierErrorCode.TEMPORARILY_UNAVAILABLE,
                "Supplier B 응답 실패: resultCode=" + resultCode + ", resultMessage=" + resultMessage);
    }

    private Throwable toSupplierException(Throwable error) {
        if (error instanceof SupplierAdapterException) {
            return error;
        }
        if (error instanceof WebClientResponseException e) {
            return new SupplierAdapterException(
                    SupplierType.SUPPLIER_B,
                    SupplierErrorCode.TEMPORARILY_UNAVAILABLE,
                    "Supplier B 응답 실패: HTTP " + e.getStatusCode(),
                    e);
        }
        if (error instanceof WebClientRequestException e) {
            return new SupplierAdapterException(
                    SupplierType.SUPPLIER_B,
                    SupplierErrorCode.TIMEOUT,
                    "Supplier B 요청 실패(연결/응답 타임아웃 포함): " + e.getMessage(),
                    e);
        }
        return new SupplierAdapterException(
                SupplierType.SUPPLIER_B,
                SupplierErrorCode.UNKNOWN,
                "Supplier B 알 수 없는 실패: " + error.getMessage(),
                error);
    }
}
