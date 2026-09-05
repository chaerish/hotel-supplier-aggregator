package hotel.supplier.aggregator.supplier;

import hotel.supplier.aggregator.domain.SupplierType;

public class SupplierAdapterException extends RuntimeException {

    private final SupplierType supplierType;
    private final SupplierErrorCode errorCode;

    public SupplierAdapterException(SupplierType supplierType, SupplierErrorCode errorCode, String message) {
        super(message);
        this.supplierType = supplierType;
        this.errorCode = errorCode;
    }

    public SupplierAdapterException(SupplierType supplierType, SupplierErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.supplierType = supplierType;
        this.errorCode = errorCode;
    }

    public SupplierType supplierType() {
        return supplierType;
    }

    public SupplierErrorCode errorCode() {
        return errorCode;
    }
}
