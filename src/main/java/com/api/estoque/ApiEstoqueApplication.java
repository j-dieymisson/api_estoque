package com.api.estoque;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class ApiEstoqueApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApiEstoqueApplication.class, args);
	}

}
