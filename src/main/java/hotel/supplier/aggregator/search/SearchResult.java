package hotel.supplier.aggregator.search;

import hotel.supplier.aggregator.domain.StandardRoomOffer;

import java.util.List;

public record SearchResult(List<StandardRoomOffer> offers, List<PartialFailure> partialFailures) {
}
