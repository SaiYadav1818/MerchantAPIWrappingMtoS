package com.sabbpe.merchant.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

/**
 * DTO for Easebuzz Payment Initiation Request
 * 
 * Handles camelCase JSON fields from client and maps to proper Java naming conventions.
 * Lombok @Data generates getFirstName(), not getFirstname().
 * 
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class EasebuzzPaymentInitiateRequest {

    /**
     * Unique order identifier
     */
    @NotBlank(message = "Order ID cannot be blank")
    private String orderId;

    /**
     * Payment amount (must be positive)
     */
    @NotNull(message = "Amount cannot be null")
    @Positive(message = "Amount must be greater than 0")
    private BigDecimal amount;

    /**
     * Product/service description
     */
    @NotBlank(message = "Product info cannot be blank")
    private String productInfo;

    /**
     * Customer first name
     * Lombok generates: getFirstName(), setFirstName(String) - NOT getFirstname()
     */
    @NotBlank(message = "First name cannot be blank")
    @JsonProperty("firstName")
    private String firstName;

    /**
     * Customer email address
     */
    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Email must be a valid email address")
    private String email;

    /**
     * Customer phone number
     */
    @NotBlank(message = "Phone cannot be blank")
    @Pattern(regexp = "^[0-9]{10}$", message = "Phone must be 10 digits")
    private String phone;

    /**
     * URL to redirect to on successful payment
     */
    @NotBlank(message = "Success URL cannot be blank")
    private String successUrl;

    /**
     * URL to redirect to on failed payment
     */
    @NotBlank(message = "Failure URL cannot be blank")
    private String failureUrl;
}
