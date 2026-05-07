package com.multi.currency.wallet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.multi.currency.wallet.infrastructure.config.CorsProperties;
import com.multi.currency.wallet.infrastructure.config.JwtProperties;

@SpringBootApplication
@EnableConfigurationProperties({ JwtProperties.class, CorsProperties.class})
public class MultiCurrencyWalletApplication {

	public static void main(String[] args) {
		SpringApplication.run(MultiCurrencyWalletApplication.class, args);
	}

}
