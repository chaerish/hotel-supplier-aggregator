package hotel.supplier.aggregator.search.dto;

import hotel.supplier.aggregator.domain.SupplierType;
import hotel.supplier.aggregator.supplier.error.SupplierErrorCode;

public record PartialFailure(SupplierType supplierType, SupplierErrorCode errorCode, String message) {
}
