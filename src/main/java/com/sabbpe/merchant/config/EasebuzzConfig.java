package com.sabbpe.merchant.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "easebuzz")
public class EasebuzzConfig {

    private UrlConfig url = new UrlConfig();
    private int timeout = 5000;
    private String surl = "http://localhost:8080/payment/easebuzz/callback";
    private String furl = "http://localhost:8080/payment/easebuzz/callback";

    public static class UrlConfig {
        private String initiate = "https://testpay.easebuzz.in/payment/initiate";
        private String payment = "https://testpay.easebuzz.in/pay/";

        // Getters and Setters
        public String getInitiate() {
            return initiate;
        }

        public void setInitiate(String initiate) {
            this.initiate = initiate;
        }

        public String getPayment() {
            return payment;
        }

        public void setPayment(String payment) {
            this.payment = payment;
        }
    }

    // Getters and Setters
    public UrlConfig getUrl() {
        return url;
    }

    public void setUrl(UrlConfig url) {
        this.url = url;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public String getSurl() {
        return surl;
    }

    public void setSurl(String surl) {
        this.surl = surl;
    }

    public String getFurl() {
        return furl;
    }

    public void setFurl(String furl) {
        this.furl = furl;
    }
}
