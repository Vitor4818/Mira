package com.mira.mira_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class MiraApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(MiraApiApplication.class, args);
	}

}
