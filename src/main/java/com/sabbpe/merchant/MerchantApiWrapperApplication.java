package com.sabbpe.merchant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MerchantApiWrapperApplication {

    public static void main(String[] args) {
        SpringApplication.run(MerchantApiWrapperApplication.class, args);
    }
}
