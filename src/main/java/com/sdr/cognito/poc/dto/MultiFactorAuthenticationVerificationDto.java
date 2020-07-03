package com.sdr.cognito.poc.dto;

public record MultiFactorAuthenticationVerificationDto(
        MultiFactorAuthenticationType multiFactorAuthenticationType,
        String userId,
        String code,
        String multiFactorAuthenticationSession
) {}
