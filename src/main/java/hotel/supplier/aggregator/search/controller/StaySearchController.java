package hotel.supplier.aggregator.search.controller;

import hotel.supplier.aggregator.search.dto.SearchResponse;
import hotel.supplier.aggregator.search.service.StaySearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.context.annotation.Profile;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@Profile("!mock")
public class StaySearchController {

    private final StaySearchService staySearchService;

    public StaySearchController(StaySearchService staySearchService) {
        this.staySearchService = staySearchService;
    }

    @Operation(
            summary = "통합 숙소 검색",
            description = "보유 숙소를 공급사별로 조회해 표준 모델로 병합한 결과를 반환한다. "
                    + "Mock Supplier 데이터 기준 예시 값이 기본으로 채워져 있어 그대로 실행해도 결과를 볼 수 있다.")
    @GetMapping("/api/v1/stays/search")
    public SearchResponse search(
            @Parameter(description = "체크인 날짜", example = "2026-09-01")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @Parameter(description = "체크아웃 날짜", example = "2026-09-04")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
            @Parameter(description = "성인 인원 수", example = "2")
            @RequestParam int adults,
            @Parameter(description = "아동 인원 수", example = "0")
            @RequestParam(defaultValue = "0") int children) {
        return staySearchService.search(checkIn, checkOut, adults, children);
    }
}
