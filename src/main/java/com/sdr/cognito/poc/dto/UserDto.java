package com.sdr.cognito.poc.dto;

public record UserDto(String id, String email, String phoneNumber, String password, String oldPassword) { }
