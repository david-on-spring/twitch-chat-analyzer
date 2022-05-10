package com.dlepe.twitchchatanalyzer.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

@Configuration
public class AwsConfiguration {
    @Bean
    public DynamoDbClient getDynamoDbClient() {
      AwsCredentialsProvider credentialsProvider = 
                DefaultCredentialsProvider.builder()
                 .profileName("default")
                 .build();
  
      return DynamoDbClient.builder()
              .region(Region.US_WEST_2)
              .credentialsProvider(credentialsProvider).build();
    }
}
