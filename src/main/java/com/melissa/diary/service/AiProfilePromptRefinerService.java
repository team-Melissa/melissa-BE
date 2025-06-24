package com.melissa.diary.service;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class AiProfilePromptRefinerService {

    private final ChatClient client;

    public AiProfilePromptRefinerService(
            @Qualifier("profilePromptRefinerClient") ChatClient client) {
        this.client = client;
    }

    public String refine(String raw) {
        try { return client.prompt().user(raw).call().content().trim(); }
        catch (Exception e) { return raw; }
    }
}