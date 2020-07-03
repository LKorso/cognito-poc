package com.sdr.cognito.poc.dto;

public record MultiFactorAuthenticationDto(boolean enabled, MultiFactorAuthenticationType type, String id, String accessToken) {}
