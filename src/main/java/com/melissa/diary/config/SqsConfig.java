package com.melissa.diary.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;

@Configuration
@EnableConfigurationProperties(SqsProperties.class)
public class SqsConfig {

    @Bean
    @ConditionalOnProperty(prefix = "melissa.sqs", name = "enabled", havingValue = "true")
    public SqsClient sqsClient(AwsCredentialsProvider awsCredentialsProvider, SqsProperties sqsProperties) {
        return SqsClient.builder()
                .region(Region.of(sqsProperties.getRegion()))
                .credentialsProvider(awsCredentialsProvider)
                .build();
    }
}
