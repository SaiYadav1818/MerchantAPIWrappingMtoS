package com.sabbpe.merchant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EasebuzzInitiateRequest {

    private String txnid;
    private BigDecimal amount;
    private String productinfo;
    private String firstname;
    private String email;
    private String phone;
    private String surl;
    private String furl;

    // -------------------------
    // UDF FIELDS (1 to 10)
    // -------------------------

    private String udf1;
    private String udf2;
    private String udf3;
    private String udf4;
    private String udf5;

    // ✔ input supported
    private String udf6;
    private String udf7;

    // ✔ optional / can remain null or empty
    private String udf8;
    private String udf9;
    private String udf10;

    // Additional fields used by the controller
    private String key;
    private String hash;
}
