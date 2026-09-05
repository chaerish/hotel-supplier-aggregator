package hotel.supplier.aggregator.supplier.suppliera;

import java.time.LocalDate;
import java.util.List;

record AAvailabilityResponse(List<AvailabilityItem> items) {

    record AvailabilityItem(
            String hotelCode,
            String hotelName,
            String roomTypeCode,
            String roomTypeName,
            int maxOccupancy,
            boolean breakfastIncluded,
            String currency,
            List<DailyRate> dailyRates
    ) {
    }

    record DailyRate(LocalDate date, int remainingRooms, long nightlyRate, long taxAmount) {
    }
}
