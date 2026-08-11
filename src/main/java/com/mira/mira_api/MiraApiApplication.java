package com.mira.mira_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;

@SpringBootApplication
public class MiraApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(MiraApiApplication.class, args);
	}

}
