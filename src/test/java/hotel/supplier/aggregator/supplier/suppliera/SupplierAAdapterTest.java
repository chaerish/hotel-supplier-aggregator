package hotel.supplier.aggregator.supplier.suppliera;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import hotel.supplier.aggregator.domain.CatalogEntry;
import hotel.supplier.aggregator.domain.StandardRoomOffer;
import hotel.supplier.aggregator.supplier.error.SupplierAdapterException;
import hotel.supplier.aggregator.supplier.error.SupplierErrorCode;
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

class SupplierAAdapterTest {

    private HttpServer server;
    private SupplierAAdapter adapter;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.start();
        WebClient webClient = WebClient.builder()
                .baseUrl("http://localhost:" + server.getAddress().getPort())
                .build();
        adapter = new SupplierAAdapter(webClient);
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void 숙소_목록을_정상적으로_가져온다() throws IOException {
        server.createContext("/a/v1/hotels", exchange -> respond(exchange, 200, """
                {
                  "items": [
                    { "hotelCode": "A-1", "hotelName": "Test Hotel", "roomTypes": [
                      { "roomTypeCode": "RT-1", "roomTypeName": "Standard", "maxOccupancy": 2 }
                    ] }
                  ]
                }
                """));

        List<CatalogEntry> entries = adapter.fetchCatalog();

        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).supplierHotelCode()).isEqualTo("A-1");
        assertThat(entries.get(0).supplierRoomTypeCode()).isEqualTo("RT-1");
    }

    @Test
    void 재고와_요금을_정상적으로_가져온다() throws IOException {
        server.createContext("/a/v1/availability", exchange -> respond(exchange, 200, """
                {
                  "items": [
                    {
                      "hotelCode": "A-1", "hotelName": "Test Hotel",
                      "roomTypeCode": "RT-1", "roomTypeName": "Standard",
                      "maxOccupancy": 2, "breakfastIncluded": false, "currency": "KRW",
                      "dailyRates": [
                        { "date": "2026-01-01", "remainingRooms": 3, "nightlyRate": 100000, "taxAmount": 10000 }
                      ]
                    }
                  ]
                }
                """));

        List<StandardRoomOffer> offers = adapter.fetchAvailability(
                List.of("A-1"), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 2), 2, 0);

        assertThat(offers).hasSize(1);
        assertThat(offers.get(0).totalPrice().amount()).isEqualTo(110_000L);
        assertThat(offers.get(0).availableRooms()).isEqualTo(3);
    }

    @Test
    void HTTP_503_응답은_SupplierAdapterException으로_변환된다() throws IOException {
        server.createContext("/a/v1/availability", exchange -> respond(exchange, 503, """
                {"error":"SERVICE_UNAVAILABLE","message":"temporarily unavailable"}
                """));

        assertThatThrownBy(() -> adapter.fetchAvailability(
                List.of("A-1"), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 2), 2, 0))
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
