package com.sabbpe.merchant.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Generic Error Response DTO
 * Used for API error responses
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorResponse {

    @JsonProperty("success")
    private Boolean success = false;

    @JsonProperty("message")
    private String message;

    @JsonProperty("error_code")
    private String errorCode;

    public ErrorResponse(String message) {
        this.success = false;
        this.message = message;
    }
}
