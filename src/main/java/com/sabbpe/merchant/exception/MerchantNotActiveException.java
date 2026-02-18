package com.sabbpe.merchant.exception;

/**
 * Exception thrown when merchant is not active
 */
public class MerchantNotActiveException extends RuntimeException {
    public MerchantNotActiveException(String message) {
        super(message);
    }
}
