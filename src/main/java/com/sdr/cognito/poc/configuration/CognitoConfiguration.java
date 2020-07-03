package com.sdr.cognito.poc.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderAsyncClient;

@Configuration
public class CognitoConfiguration {

    @Bean
    public CognitoIdentityProviderAsyncClient cognitoClient() {
        return CognitoIdentityProviderAsyncClient.create();
    }

}
