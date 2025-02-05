package com.melissa.diary.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.OpenAiImageModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.openai.api.OpenAiImageApi;
import org.springframework.beans.factory.annotation.Qualifier;
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

    @Bean(name = "profileClient")
    ChatClient profileClient(){
        OpenAiApi api = new OpenAiApi(apiKey);
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(OpenAiApi.ChatModel.GPT_4_O_MINI)
                .temperature(0.5)
                .build();

        String system = """
                너는 사용자의 요구사항을 분석하여 맞춤형 캐릭터 프로필을 생성하는 전문가야. 각 프로필은 독특하고 매력적이며, 사용자가 제시한 특성을 정확히 반영해야 해. 특히 다음 사항에 주의해줘:
                - 프로필 이름은 기억하기 쉽고 특징을 잘 나타내야 함
                - 첫 대화는 캐릭터의 성격이 잘 드러나야 함
                - 해시태그는 핵심 특징을 함축적으로 표현해야 함
                - 기계적이거나 형식적인 답변 대신 실제 사람 작성하는 것처럼 자연스럽게 응답할 것""";

        return ChatClient.builder(new OpenAiChatModel(api, options))
                .defaultSystem(system)
                .build();
    }


    @Bean(name = "aiChatClient")
    ChatClient chatClient() {
        OpenAiApi api = new OpenAiApi(apiKey);
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(OpenAiApi.ChatModel.GPT_4_O_MINI)
                .temperature(0.5)
                .build();

        return ChatClient.builder(new OpenAiChatModel(api, options))
                .defaultSystem("사용자와 채팅을 나누면서, 일기를 작성할 정보를 추출하거나 공감해줘. 대답에서 해시태그는 사용하지마. {system}")
                .build();
    }
}
