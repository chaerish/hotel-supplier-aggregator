package hotel.supplier.aggregator.search.controller;

import hotel.supplier.aggregator.search.dto.SearchResponse;
import hotel.supplier.aggregator.search.service.StaySearchService;
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

    @GetMapping("/api/v1/stays/search")
    public SearchResponse search(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
            @RequestParam int adults,
            @RequestParam(defaultValue = "0") int children) {
        return staySearchService.search(checkIn, checkOut, adults, children);
    }
}
