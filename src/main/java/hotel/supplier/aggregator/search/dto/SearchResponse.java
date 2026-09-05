package hotel.supplier.aggregator.search.dto;

import hotel.supplier.aggregator.domain.StandardRoomOffer;

import java.util.List;

public record SearchResponse(List<StandardRoomOffer> offers, List<PartialFailure> partialFailures) {
}
