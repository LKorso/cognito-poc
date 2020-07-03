package com.sdr.cognito.poc;

public enum UserAttribute {

    EMAIL("userId"),
    PHONE_NUMBER("phone_number"),
    PASSWORD("password");

    private final String name;

    UserAttribute(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

}
