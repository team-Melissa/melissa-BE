package com.melissa.diary.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
public class NotificationConfig {
    
    /**
     * Expo Push Notification API 호출용 WebClient
     * - Connection Timeout: 5초
     * - Read/Write Timeout: 10초
     * - Response Timeout: 10초
     */
    @Bean(name = "expoWebClient")
    public WebClient expoWebClient() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .responseTimeout(Duration.ofSeconds(10))
                .doOnConnected(conn -> 
                    conn.addHandlerLast(new ReadTimeoutHandler(10))
                        .addHandlerLast(new WriteTimeoutHandler(10))
                );
        
        return WebClient.builder()
                .baseUrl("https://exp.host/--/api/v2/push/send")
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}

