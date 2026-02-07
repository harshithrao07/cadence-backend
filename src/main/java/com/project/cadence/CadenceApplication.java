package com.project.cadence;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class CadenceApplication {

	public static void main(String[] args) {
		SpringApplication.run(CadenceApplication.class, args);
	}

}
