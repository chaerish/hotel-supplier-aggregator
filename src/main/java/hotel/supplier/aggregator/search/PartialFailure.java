package hotel.supplier.aggregator.search;

import hotel.supplier.aggregator.domain.SupplierType;
import hotel.supplier.aggregator.supplier.error.SupplierErrorCode;

public record PartialFailure(SupplierType supplierType, SupplierErrorCode errorCode, String message) {
}
