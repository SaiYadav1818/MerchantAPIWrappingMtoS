package com.sabbpe.merchant.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for payment response after successful hash verification
 * Contains status, internal token and order ID
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentResponse {

    private String status;
    private String internalToken;
    private String orderId;
    private String message;
    private String errorCode;
    private Long timestamp;
}
