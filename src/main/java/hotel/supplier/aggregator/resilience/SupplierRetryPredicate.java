package hotel.supplier.aggregator.resilience;

import hotel.supplier.aggregator.supplier.error.SupplierAdapterException;
import hotel.supplier.aggregator.supplier.error.SupplierErrorCode;
import org.springframework.resilience.retry.MethodRetryPredicate;

import java.lang.reflect.Method;

// TIMEOUT/TEMPORARILY_UNAVAILABLE만 재시도 대상으로 삼는다 (DESIGN.md 5번 참조).
// UNKNOWN은 같은 원인으로 다시 실패할 가능성이 높아 제외한다.
public class SupplierRetryPredicate implements MethodRetryPredicate {

    @Override
    public boolean shouldRetry(Method method, Throwable throwable) {
        if (!(throwable instanceof SupplierAdapterException e)) {
            return false;
        }
        return e.errorCode() == SupplierErrorCode.TIMEOUT
                || e.errorCode() == SupplierErrorCode.TEMPORARILY_UNAVAILABLE;
    }
}
