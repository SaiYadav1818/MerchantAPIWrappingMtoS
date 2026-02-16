package com.sabbpe.merchant.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for Easebuzz payload hash generation
 * 
 * Contains the generated hash and status information
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GatewayHashResponse {

    private String status;
    private String hash;
    private String message;
    private Long timestamp;
}
