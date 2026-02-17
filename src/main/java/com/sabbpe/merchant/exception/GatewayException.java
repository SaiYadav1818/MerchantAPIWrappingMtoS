package com.sabbpe.merchant.exception;

import com.sabbpe.merchant.dto.ErrorResponse;

public class GatewayException extends RuntimeException {

    private int statusCode;
    private ErrorResponse errorResponse;

    public GatewayException(String message) {
        super(message);
        this.statusCode = 502;
        this.errorResponse = null;
    }

    public GatewayException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
        this.errorResponse = null;
    }

    public GatewayException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = 502;
        this.errorResponse = null;
    }

    public GatewayException(String message, int statusCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
        this.errorResponse = null;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public ErrorResponse getErrorResponse() {
        return errorResponse;
    }

    public void setErrorResponse(ErrorResponse errorResponse) {
        this.errorResponse = errorResponse;
    }
}
