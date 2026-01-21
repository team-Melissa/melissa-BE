package com.melissa.diary;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableJpaRepositories
@EnableScheduling
@EnableJpaAuditing
public class MelissaDiaryAssistantApplication {

	public static void main(String[] args) {
		SpringApplication.run(MelissaDiaryAssistantApplication.class, args);
	}




}
