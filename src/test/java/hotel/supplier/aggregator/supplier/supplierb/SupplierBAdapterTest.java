package hotel.supplier.aggregator.supplier.supplierb;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import hotel.supplier.aggregator.domain.CatalogEntry;
import hotel.supplier.aggregator.domain.StandardRoomOffer;
import hotel.supplier.aggregator.supplier.SupplierAdapterException;
import hotel.supplier.aggregator.supplier.SupplierErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SupplierBAdapterTest {

    private HttpServer server;
    private SupplierBAdapter adapter;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.start();
        WebClient webClient = WebClient.builder()
                .baseUrl("http://localhost:" + server.getAddress().getPort())
                .build();
        adapter = new SupplierBAdapter(webClient);
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void 숙소_목록을_정상적으로_가져온다() throws IOException {
        server.createContext("/b/api/properties", exchange -> respond(exchange, 200, """
                {
                  "resultCode": "0000", "resultMessage": "SUCCESS",
                  "data": { "items": [
                    { "propertyId": "B-1", "propertyName": "Test Hotel", "rooms": [
                      { "roomId": "R-1", "roomName": "Standard", "maxOccupancy": 2 }
                    ] }
                  ] }
                }
                """));

        List<CatalogEntry> entries = adapter.fetchCatalog();

        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).supplierHotelCode()).isEqualTo("B-1");
    }

    @Test
    void 요금과_재고를_정상적으로_가져온다() throws IOException {
        server.createContext("/b/api/search", exchange -> respond(exchange, 200, """
                {
                  "resultCode": "0000", "resultMessage": "SUCCESS",
                  "data": { "items": [
                    {
                      "propertyId": "B-1", "propertyName": "Test Hotel",
                      "roomId": "R-1", "roomName": "Standard",
                      "maxOccupancy": 2, "breakfastIncluded": true, "currency": "KRW",
                      "totalPrice": 200000, "taxIncluded": true,
                      "inventory": [
                        { "date": "2026-01-01", "remainingRooms": 4 }
                      ]
                    }
                  ] }
                }
                """));

        List<StandardRoomOffer> offers = adapter.fetchAvailability(
                List.of("B-1"), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 2), 2, 0);

        assertThat(offers).hasSize(1);
        assertThat(offers.get(0).totalPrice().amount()).isEqualTo(200_000L);
        assertThat(offers.get(0).availableRooms()).isEqualTo(4);
    }

    @Test
    void HTTP_200과_실패_resultCode는_SupplierAdapterException으로_변환된다() throws IOException {
        server.createContext("/b/api/search", exchange -> respond(exchange, 200, """
                {"resultCode":"E503","resultMessage":"TEMPORARILY_UNAVAILABLE","data":null}
                """));

        assertThatThrownBy(() -> adapter.fetchAvailability(
                List.of("B-1"), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 2), 2, 0))
                .isInstanceOf(SupplierAdapterException.class)
                .satisfies(e -> assertThat(((SupplierAdapterException) e).errorCode())
                        .isEqualTo(SupplierErrorCode.TEMPORARILY_UNAVAILABLE));
    }

    private void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
