package com.melissa.diary.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.OpenAiImageModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.openai.api.OpenAiImageApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    @Value("${spring.ai.openai.api-key}") String apiKey;
    @Bean
    ImageModel imageModel() {
        return new OpenAiImageModel(new OpenAiImageApi(apiKey));
    }

    @Bean
    ChatModel chatModel() {
        return new OpenAiChatModel(new OpenAiApi(apiKey));
    }

    @Bean
    ChatClient chatClient() {
        OpenAiApi api = new OpenAiApi(apiKey);
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model("gpt-4o")
                .temperature(0.5)
                .build();

        return ChatClient.builder(new OpenAiChatModel(api, options))
                .defaultSystem("사용자와 채팅을 나누면서, 일기를 작성할 정보를 추출하거나 공감해줘")
                .build();
    }
}
