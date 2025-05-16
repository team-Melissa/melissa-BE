package com.melissa.diary.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Configuration
@EnableAsync

public class AsyncConfig {
    @Bean(name = "asyncTaskExecutor")
    public Executor taskExecutor() {
        return Executors.newFixedThreadPool(4);
    }
}
