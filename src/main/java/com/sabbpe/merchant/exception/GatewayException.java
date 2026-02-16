package com.sabbpe.merchant.exception;

public class GatewayException extends RuntimeException {

    private int statusCode;

    public GatewayException(String message) {
        super(message);
        this.statusCode = 502;
    }

    public GatewayException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public GatewayException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = 502;
    }

    public GatewayException(String message, int statusCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
