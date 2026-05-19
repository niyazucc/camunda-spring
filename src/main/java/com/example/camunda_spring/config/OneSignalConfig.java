package com.example.camunda_spring.config;

import com.onesignal.client.ApiClient;
import com.onesignal.client.api.DefaultApi;
import com.onesignal.client.auth.HttpBearerAuth;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OneSignalConfig {

    @Bean
    public DefaultApi oneSignalApi(@Value("${onesignal.api-key}") String apiKey) {
        ApiClient apiClient = com.onesignal.client.Configuration.getDefaultApiClient();

        HttpBearerAuth restApiKey = (HttpBearerAuth) apiClient.getAuthentication("rest_api_key");
        restApiKey.setBearerToken(apiKey);

        return new DefaultApi(apiClient);
    }
}
