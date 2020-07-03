package com.sdr.cognito.poc.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoginResponse {

    private String idToken;
    private String accessToken;
    private String refreshToken;
    private boolean multiFactorAuthenticationRequired;
    private MultiFactorAuthenticationType multiFactorAuthenticationType;
    private String multiFactorAuthenticationSession;
    private String userId;
    private String deliveryDestination;

}
