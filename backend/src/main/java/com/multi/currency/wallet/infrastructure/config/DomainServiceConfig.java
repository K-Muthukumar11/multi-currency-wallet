package com.multi.currency.wallet.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.multi.currency.wallet.domain.service.TransferDomainService;

@Configuration
public class DomainServiceConfig {

    @Bean
    public TransferDomainService transferDomainService() {
        return new TransferDomainService();
    }
}
