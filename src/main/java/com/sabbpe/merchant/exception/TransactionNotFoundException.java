package com.sabbpe.merchant.exception;

/**
 * Exception thrown when a transaction is not found
 */
public class TransactionNotFoundException extends RuntimeException {
    public TransactionNotFoundException(String message) {
        super(message);
    }

    public TransactionNotFoundException(String txnid, Throwable cause) {
        super("Transaction not found with txnid: " + txnid, cause);
    }
}
