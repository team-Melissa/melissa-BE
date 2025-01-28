package com.melissa.diary;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.StreamingChatModel;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.image.ImageOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiImageModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.openai.api.OpenAiImageApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories
public class MelissaDiaryAssistantApplication {

	public static void main(String[] args) {
		SpringApplication.run(MelissaDiaryAssistantApplication.class, args);
	}

	@Bean
	ImageModel imageModel(@Value("${spring.ai.openai.api-key}") String apiKey) {
		return new OpenAiImageModel(new OpenAiImageApi(apiKey));
	}

	@Bean
	ChatModel chatModel(@Value("${spring.ai.openai.api-key}") String apiKey) {
		return new OpenAiChatModel(new OpenAiApi(apiKey));
	}


}
