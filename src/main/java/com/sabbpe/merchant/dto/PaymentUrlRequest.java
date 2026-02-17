package com.sabbpe.merchant.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for payment URL request
 * Contains internal token and payment details needed to generate Easebuzz payment URL
 * Supports both camelCase and snake_case JSON field names
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentUrlRequest {

    @NotBlank(message = "Internal token is required")
    @JsonProperty("internal_token")
    private String internalToken;

    @NotBlank(message = "Product info is required")
    @JsonProperty("product_info")
    private String productInfo;

    @NotBlank(message = "First name is required")
    @JsonProperty("first_name")
    private String firstName;

    @NotBlank(message = "Email is required")
    private String email;

    @NotBlank(message = "Phone is required")
    private String phone;
}
