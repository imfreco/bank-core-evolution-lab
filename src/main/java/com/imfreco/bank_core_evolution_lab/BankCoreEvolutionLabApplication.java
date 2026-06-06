package com.imfreco.bank_core_evolution_lab;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class BankCoreEvolutionLabApplication {

	public static void main(String[] args) {
		SpringApplication.run(BankCoreEvolutionLabApplication.class, args);
	}

}
