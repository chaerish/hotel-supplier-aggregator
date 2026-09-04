package hotel.supplier.aggregator.mock;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * "mock" 프로필로 기동했을 때만 활성화된다 (application-mock.properties: server.port=9090).
 * 본체 애플리케이션과 같은 포트에서 뜨면 어댑터가 자기 자신을 호출하며 스레드가 묶일 수 있어 분리했다.
 */
@Profile("mock")
@RestController
class MockSupplierController {

    /** normal | error | no-response — POST /control/{a|b}/mode?value=... 로 바꾼다 */
    private final Map<String, String> modes = new ConcurrentHashMap<>();

    @PostMapping("/control/{supplier}/mode")
    public Map<String, String> setMode(@PathVariable String supplier, @RequestParam String value) {
        modes.put(supplier, value);
        return Map.of(supplier, value);
    }

    // ── ① 숙소 목록 (정적 콘텐츠) ─────────────────────────────

    @GetMapping(value = "/a/v1/hotels", produces = "application/json")
    public ResponseEntity<String> hotelsA() {
        return ResponseEntity.ok(A_HOTELS);
    }

    @GetMapping(value = "/b/api/properties", produces = "application/json")
    public ResponseEntity<String> propertiesB() {
        return ResponseEntity.ok(B_PROPERTIES);
    }

    // ── ② 재고·요금 조회 (숙소 코드 목록을 받는다) ──────────────

    @GetMapping(value = "/a/v1/availability", produces = "application/json")
    public ResponseEntity<String> availabilityA(@RequestParam String hotelCodes) {
        String mode = modes.getOrDefault("a", "normal");
        return switch (mode) {
            case "error" -> ResponseEntity.status(503)
                    .body("""
                    {"error":"SERVICE_UNAVAILABLE","message":"temporarily unavailable"}
                    """);
            case "no-response" -> {
                sleep(600_000);
                yield ResponseEntity.ok("{}");
            }
            default -> ResponseEntity.ok(A_AVAILABILITY);
        };
    }

    @GetMapping(value = "/b/api/search", produces = "application/json")
    public ResponseEntity<String> searchB(@RequestParam String propertyIds) {
        String mode = modes.getOrDefault("b", "normal");
        return switch (mode) {
            // B는 장애 상황에서도 HTTP 200 이다
            case "error" -> ResponseEntity.ok("""
                {"resultCode":"E503","resultMessage":"TEMPORARILY_UNAVAILABLE","data":null}
                """);
            case "no-response" -> {
                sleep(600_000);
                yield ResponseEntity.ok("{}");
            }
            default -> ResponseEntity.ok(B_SEARCH);
        };
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // 부록 A.1 / A.2 의 응답 예시를 그대로 문자열로 넣었다 (요청 파라미터는 무시하고 고정 응답)
    private static final String A_HOTELS = """
        {
          "items": [
            {
              "hotelCode": "A-10023",
              "hotelName": "Riverside Hotel Seoul",
              "roomTypes": [
                { "roomTypeCode": "DLX-TWN", "roomTypeName": "Deluxe Twin", "maxOccupancy": 2 }
              ]
            },
            {
              "hotelCode": "A-10044",
              "hotelName": "Namsan Garden Stay",
              "roomTypes": [
                { "roomTypeCode": "STD-DBL", "roomTypeName": "Standard Double", "maxOccupancy": 2 }
              ]
            }
          ]
        }
        """;
    private static final String A_AVAILABILITY = """
        {
          "items": [
            {
              "hotelCode": "A-10023",
              "hotelName": "Riverside Hotel Seoul",
              "roomTypeCode": "DLX-TWN",
              "roomTypeName": "Deluxe Twin",
              "maxOccupancy": 2,
              "breakfastIncluded": false,
              "currency": "KRW",
              "dailyRates": [
                { "date": "2026-09-01", "remainingRooms": 3, "nightlyRate": 120000, "taxAmount": 12000 },
                { "date": "2026-09-02", "remainingRooms": 1, "nightlyRate": 150000, "taxAmount": 15000 },
                { "date": "2026-09-03", "remainingRooms": 5, "nightlyRate": 120000, "taxAmount": 12000 }
              ]
            },
            {
              "hotelCode": "A-10044",
              "hotelName": "Namsan Garden Stay",
              "roomTypeCode": "STD-DBL",
              "roomTypeName": "Standard Double",
              "maxOccupancy": 2,
              "breakfastIncluded": false,
              "currency": "KRW",
              "dailyRates": [
                { "date": "2026-09-01", "remainingRooms": 2, "nightlyRate": 88000, "taxAmount": 8800 },
                { "date": "2026-09-02", "remainingRooms": 0, "nightlyRate": 99000, "taxAmount": 9900 },
                { "date": "2026-09-03", "remainingRooms": 4, "nightlyRate": 88000, "taxAmount": 8800 }
              ]
            }
          ]
        }
        """;
    private static final String B_PROPERTIES = """
        {
          "resultCode": "0000",
          "resultMessage": "SUCCESS",
          "data": {
            "items": [
              {
                "propertyId": "B77120",
                "propertyName": "Riverside Hotel Seoul",
                "rooms": [
                  { "roomId": "R-401", "roomName": "Deluxe Twin Room", "maxOccupancy": 2 }
                ]
              }
            ]
          }
        }
        """;
    private static final String B_SEARCH = """
        {
          "resultCode": "0000",
          "resultMessage": "SUCCESS",
          "data": {
            "items": [
              {
                "propertyId": "B77120",
                "propertyName": "Riverside Hotel Seoul",
                "roomId": "R-401",
                "roomName": "Deluxe Twin Room",
                "maxOccupancy": 2,
                "breakfastIncluded": true,
                "currency": "KRW",
                "totalPrice": 452000,
                "taxIncluded": true,
                "inventory": [
                  { "date": "2026-09-01", "remainingRooms": 3 },
                  { "date": "2026-09-02", "remainingRooms": 1 },
                  { "date": "2026-09-03", "remainingRooms": 5 }
                ]
              }
            ]
          }
        }
        """;
}