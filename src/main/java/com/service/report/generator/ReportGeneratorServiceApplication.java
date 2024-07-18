package com.service.report.generator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class ReportGeneratorServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ReportGeneratorServiceApplication.class, args);
	}

}
