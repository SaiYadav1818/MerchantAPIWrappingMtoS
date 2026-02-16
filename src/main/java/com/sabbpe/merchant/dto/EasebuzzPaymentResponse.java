package com.sabbpe.merchant.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * DTO for Easebuzz payment initiation response
 * Contains payment status, message, error code, payment URL, and transaction ID
 * 
 * This class uses static factory methods for creating instances rather than
 * multiple constructors to avoid conflicts.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EasebuzzPaymentResponse {

    @JsonProperty("status")
    private String status;

    @JsonProperty("message")
    private String message;

    @JsonProperty("error_code")
    private String errorCode;

    @JsonProperty("payment_url")
    private String paymentUrl;

    @JsonProperty("txn_id")
    private String txnId;

    /**
     * Default constructor
     */
    public EasebuzzPaymentResponse() {
    }

    /**
     * Full constructor with all fields
     * 
     * @param status The payment status ("SUCCESS" or "FAILURE")
     * @param message The response message
     * @param errorCode The error code if failure
     * @param paymentUrl The payment URL for successful initiation
     * @param txnId The transaction ID
     */
    public EasebuzzPaymentResponse(String status, String message, String errorCode, 
                                   String paymentUrl, String txnId) {
        this.status = status;
        this.message = message;
        this.errorCode = errorCode;
        this.paymentUrl = paymentUrl;
        this.txnId = txnId;
    }

    /**
     * Backward-compatible constructor that accepts Integer status and String URL
     * Converts Integer status to String representation
     * 
     * @param status Integer status (1 for success, 0 for failure)
     * @param url The payment URL
     */
    public EasebuzzPaymentResponse(Integer status, String url) {
        this.status = (status != null && status == 1) ? "SUCCESS" : "FAILURE";
        this.paymentUrl = url;
        this.message = (status != null && status == 1) ? "Payment initiated successfully" : "Payment initiation failed";
    }

    /**
     * Static factory method for successful payment response
     * 
     * @param txnId The transaction ID
     * @param paymentUrl The payment URL
     * @return EasebuzzPaymentResponse with success status
     */
    public static EasebuzzPaymentResponse success(String txnId, String paymentUrl) {
        return new EasebuzzPaymentResponse(
            "SUCCESS",
            "Payment initiated successfully",
            null,
            paymentUrl,
            txnId
        );
    }

    /**
     * Static factory method for failure response
     * 
     * @param message The error message
     * @param errorCode The error code
     * @return EasebuzzPaymentResponse with failure status
     */
    public static EasebuzzPaymentResponse failure(String message, String errorCode) {
        return new EasebuzzPaymentResponse(
            "FAILURE",
            message,
            errorCode,
            null,
            null
        );
    }

    /**
     * Static factory method for error response with full parameters
     * Provides backward compatibility with existing code
     * 
     * @param status The status ("SUCCESS" or "FAILURE")
     * @param message The response message
     * @param errorCode The error code
     * @return EasebuzzPaymentResponse with specified values
     */
    public static EasebuzzPaymentResponse error(String status, String message, String errorCode) {
        return new EasebuzzPaymentResponse(
            status,
            message,
            errorCode,
            null,
            null
        );
    }

    // Getters and Setters

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getPaymentUrl() {
        return paymentUrl;
    }

    public void setPaymentUrl(String paymentUrl) {
        this.paymentUrl = paymentUrl;
    }

    public String getTxnId() {
        return txnId;
    }

    public void setTxnId(String txnId) {
        this.txnId = txnId;
    }

    /**
     * Builder method for fluent API
     * Provides backward compatibility with existing code using builder pattern
     * 
     * @return EasebuzzPaymentResponseBuilder builder instance
     */
    public static EasebuzzPaymentResponseBuilder builder() {
        return new EasebuzzPaymentResponseBuilder();
    }

    @Override
    public String toString() {
        return "EasebuzzPaymentResponse{" +
                "status='" + status + '\'' +
                ", message='" + message + '\'' +
                ", errorCode='" + errorCode + '\'' +
                ", paymentUrl='" + paymentUrl + '\'' +
                ", txnId='" + txnId + '\'' +
                '}';
    }

    /**
     * Builder class for EasebuzzPaymentResponse
     * Provides backward compatibility with @Builder annotation
     */
    public static class EasebuzzPaymentResponseBuilder {
        private String status;
        private String message;
        private String errorCode;
        private String paymentUrl;
        private String txnId;

        public EasebuzzPaymentResponseBuilder() {
        }

        public EasebuzzPaymentResponseBuilder status(String status) {
            this.status = status;
            return this;
        }

        public EasebuzzPaymentResponseBuilder message(String message) {
            this.message = message;
            return this;
        }

        public EasebuzzPaymentResponseBuilder errorCode(String errorCode) {
            this.errorCode = errorCode;
            return this;
        }

        public EasebuzzPaymentResponseBuilder paymentUrl(String paymentUrl) {
            this.paymentUrl = paymentUrl;
            return this;
        }

        public EasebuzzPaymentResponseBuilder txnId(String txnId) {
            this.txnId = txnId;
            return this;
        }

        public EasebuzzPaymentResponse build() {
            return new EasebuzzPaymentResponse(status, message, errorCode, paymentUrl, txnId);
        }

        @Override
        public String toString() {
            return "EasebuzzPaymentResponseBuilder{" +
                    "status='" + status + '\'' +
                    ", message='" + message + '\'' +
                    ", errorCode='" + errorCode + '\'' +
                    ", paymentUrl='" + paymentUrl + '\'' +
                    ", txnId='" + txnId + '\'' +
                    '}';
        }
    }
}
