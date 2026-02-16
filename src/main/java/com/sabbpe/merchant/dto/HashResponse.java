package com.sabbpe.merchant.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for hash generation response
 * Contains the generated SHA-256 hash
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HashResponse {

    private String hash;
    private String orderId;
    private String message;

    // Constructor with hash and orderId (for generateHash API response)
    public HashResponse(String hash, String orderId) {
        this.hash = hash;
        this.orderId = orderId;
    }
}
