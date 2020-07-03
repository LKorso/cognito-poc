package com.sdr.cognito.poc.dto;

public record ResponseWrapper<R, E> (R response, E exception) {}
