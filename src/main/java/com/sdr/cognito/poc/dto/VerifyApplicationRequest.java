package com.sdr.cognito.poc.dto;

public record VerifyApplicationRequest(String code, String accessToken, String deviceName, String id) {
}
