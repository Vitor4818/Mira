package com.mira.mira_api.infrastructure.aws;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class AwsConfig {

    @Bean
    public S3Client s3Client(
            @Value("${aws.s3.region:us-east-1}") String region,
            @Value("${spring.ai.bedrock.aws.access-key:}") String accessKey,
            @Value("${spring.ai.bedrock.aws.secret-key:}") String secretKey) {

        var clientBuilder = S3Client.builder()
                .region(Region.of(region));

        if (accessKey != null && !accessKey.isBlank() && secretKey != null && !secretKey.isBlank()) {
            clientBuilder.credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(accessKey, secretKey)
            ));
        } else {
            clientBuilder.credentialsProvider(DefaultCredentialsProvider.create());
        }
        return clientBuilder.build();
    }
}