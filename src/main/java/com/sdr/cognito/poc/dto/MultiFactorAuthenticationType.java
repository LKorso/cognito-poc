package com.sdr.cognito.poc.dto;

import software.amazon.awssdk.services.cognitoidentityprovider.model.ChallengeNameType;

import static software.amazon.awssdk.services.cognitoidentityprovider.model.ChallengeNameType.SMS_MFA;
import static software.amazon.awssdk.services.cognitoidentityprovider.model.ChallengeNameType.SOFTWARE_TOKEN_MFA;

public enum MultiFactorAuthenticationType {
    SMS(SMS_MFA, "SMS_MFA_CODE"),
    APPLICATION(SOFTWARE_TOKEN_MFA, "SOFTWARE_TOKEN_MFA_CODE");

    private final ChallengeNameType challengeNameType;
    private final String mfaCodeName;

    MultiFactorAuthenticationType(ChallengeNameType challengeNameType, String mfaCodeName) {
        this.challengeNameType = challengeNameType;
        this.mfaCodeName = mfaCodeName;
    }

    public ChallengeNameType getChallengeNameType() {
        return challengeNameType;
    }

    public String getMfaCodeName() {
        return mfaCodeName;
    }

}
