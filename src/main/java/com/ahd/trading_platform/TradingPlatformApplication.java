package com.ahd.trading_platform;

import org.camunda.bpm.spring.boot.starter.annotation.EnableProcessApplication;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableFeignClients
@EnableProcessApplication
@EnableAsync
public class TradingPlatformApplication {

	public static void main(String[] args) {
		SpringApplication.run(TradingPlatformApplication.class, args);
	}

}
