package com.sabbpe.merchant.config;

import com.sabbpe.merchant.entity.Merchant;
import com.sabbpe.merchant.entity.MerchantStatus;
import com.sabbpe.merchant.repository.MerchantRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializationConfig {

    @Bean
    public CommandLineRunner initializeMerchants(MerchantRepository merchantRepository) {
        return args -> {
            if (merchantRepository.findByMerchantId("M123").isEmpty()) {
                Merchant merchant = Merchant.builder()
                    .merchantId("M123")
                    .merchantName("Test Merchant")
                    .saltKey("secret_salt_key_123")
                    .status(MerchantStatus.ACTIVE)
                    .build();
                
                merchantRepository.save(merchant);
                System.out.println("[INFO] Sample merchant created: M123");
            }
        };
    }
}
