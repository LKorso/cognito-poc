package com.sdr.cognito.poc;

import com.sdr.cognito.poc.dto.*;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderAsyncClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;

import java.util.Map;

import static com.sdr.cognito.poc.dto.MultiFactorAuthenticationType.APPLICATION;
import static com.sdr.cognito.poc.dto.MultiFactorAuthenticationType.SMS;
import static software.amazon.awssdk.services.cognitoidentityprovider.model.ChallengeNameType.SMS_MFA;
import static software.amazon.awssdk.services.cognitoidentityprovider.model.ChallengeNameType.SOFTWARE_TOKEN_MFA;

@RestController
public class Controller {

    private final CognitoIdentityProviderAsyncClient cognitoClient;

    @Value("${aws.cognito.user.pool}")
    private String userPool;

    @Value("${aws.cognito.client.id}")
    private String clientId;

    public Controller(CognitoIdentityProviderAsyncClient cognitoClient) {
        this.cognitoClient = cognitoClient;
    }

    @GetMapping("/user/{userName}")
    public ResponseEntity getUser(@PathVariable String userName) {
        ResponseWrapper<AdminGetUserResponse, Throwable> wrapper = cognitoClient.adminGetUser(AdminGetUserRequest.builder()
                .username(userName)
                .userPoolId(userPool)
                .build())
                .handle(ResponseWrapper::new)
                .join();

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        AdminInitiateAuthRequest authRequest = AdminInitiateAuthRequest.builder()
                .authFlow(AuthFlowType.ADMIN_USER_PASSWORD_AUTH)
                .userPoolId(userPool)
                .clientId(clientId)
                .authParameters(Map.of("USERNAME", request.username(), "PASSWORD", request.password()))
                .build();
//SMS_MFA
        AdminInitiateAuthResponse response = cognitoClient
                .adminInitiateAuth(authRequest)
                .handle(ResponseWrapper::new)
                .join()
                .response();

//        AdminGetUserRequest getUserRequest = AdminGetUserRequest.builder()
//                .userPoolId(userPool)
//                .username("47bdbdab-5518-4585-84e8-c53bf8f0b6de")
//                .build();
//        cognitoClient.adminGetUser(AdminGetUserRequest.builder()
//                .userPoolId(userPool)
//                .username("47bdbdab-5518-4585-84e8-c53bf8f0b6de")
//                .build())

        if (SMS.getChallengeNameType() == response.challengeName()) {
            return ResponseEntity.ok(
                    LoginResponse.builder()
                            .multiFactorAuthenticationRequired(true)
                            .multiFactorAuthenticationSession(response.session())
                            .multiFactorAuthenticationType(SMS)
                            .userId(response.challengeParameters().get("USER_ID_FOR_SRP"))
                            .deliveryDestination(response.challengeParameters().get("CODE_DELIVERY_DESTINATION"))
                            .build()
            );
        } else if (APPLICATION.getChallengeNameType() == response.challengeName()) {
            return ResponseEntity.ok(
                    LoginResponse.builder()
                            .multiFactorAuthenticationRequired(true)
                            .multiFactorAuthenticationSession(response.session())
                            .multiFactorAuthenticationType(APPLICATION)
                            .userId(response.challengeParameters().get("USER_ID_FOR_SRP"))
                            .deliveryDestination(response.challengeParameters().get("FRIENDLY_DEVICE_NAME"))
                            .build()
            );
        }

        return ResponseEntity.ok(
                LoginResponse.builder()
                        .accessToken(response.authenticationResult().accessToken())
                        .idToken(response.authenticationResult().idToken())
                        .refreshToken(response.authenticationResult().refreshToken())
                        .build()
        );
    }

    @PostMapping("/tokens/refresh")
    public ResponseEntity refresh(@RequestBody RefreshTokenRequest request) {

        AdminInitiateAuthRequest authRequest = AdminInitiateAuthRequest.builder()
                .clientId(clientId)
                .userPoolId(userPool)
                .authFlow(AuthFlowType.REFRESH_TOKEN_AUTH)
                .authParameters(Map.of("REFRESH_TOKEN", request.getRefreshToken()))
                .build();

        ResponseWrapper<AdminInitiateAuthResponse, Throwable> responseWrapper = cognitoClient.adminInitiateAuth(authRequest)
                .handle(ResponseWrapper::new)
                .join();

        return ResponseEntity.ok(
                LoginResponse.builder()
                        .accessToken(responseWrapper.response().authenticationResult().accessToken())
                        .idToken(responseWrapper.response().authenticationResult().idToken())
                        .refreshToken(responseWrapper.response().authenticationResult().refreshToken())
                        .build()
        );
    }

    @PostMapping("/multi-factor-authentication/confirm")
    public ResponseEntity confirm(@RequestBody MultiFactorAuthenticationVerificationDto request) {
        RespondToAuthChallengeRequest challengeRequest = RespondToAuthChallengeRequest.builder()
                .clientId(clientId)
                .challengeName(request.multiFactorAuthenticationType().getChallengeNameType())
                .session(request.multiFactorAuthenticationSession())
                .challengeResponses(
                        Map.of(request.multiFactorAuthenticationType().getMfaCodeName(), request.code(), "USERNAME", request.userId())
                )
                .build();

        ResponseWrapper<RespondToAuthChallengeResponse, Throwable> response = cognitoClient
                .respondToAuthChallenge(challengeRequest)
                .handle(ResponseWrapper::new)
                .join();

        if (response.exception() != null) return ResponseEntity.status(500).body(response.exception());

        return ResponseEntity.ok(
                LoginResponse.builder()
                        .accessToken(response.response().authenticationResult().accessToken())
                        .idToken(response.response().authenticationResult().idToken())
                        .refreshToken(response.response().authenticationResult().refreshToken())
                        .build()
        );
    }

    @PatchMapping("/user")
    public ResponseEntity updateEmail(@RequestBody UserDto request) {
        ResponseWrapper<? extends CognitoIdentityProviderResponse, Throwable> response;
        if (request.email() != null) {
            response = updateAttribute(request.id(), request.email(), "email");
        } else {
            return ResponseEntity.noContent().build();
        }
        return response.exception() != null ?
                ResponseEntity
                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(response.exception()) :
                ResponseEntity.noContent().build();
    }

    private ResponseWrapper<AdminUpdateUserAttributesResponse, Throwable> updateAttribute(String username, String value,
                                                                                          String attributeName) {
        AttributeType build = AttributeType.builder()
                .value(value)
                .name(attributeName)
                .build();

        AdminUpdateUserAttributesRequest attributesRequest = AdminUpdateUserAttributesRequest.builder()
                .userPoolId(userPool)
                .username(username)
                .userAttributes(build)
                .build();

        return cognitoClient.adminUpdateUserAttributes(attributesRequest)
                .handle(ResponseWrapper::new)
                .join();
    }

//    @PostMapping("/user")
//    public ResponseEntity getUser(@RequestBody UserDto request) {
//        ResponseWrapper<GetUserResponse, Throwable> response =
//                cognitoClient.getUser(GetUserRequest.builder().accessToken(request.accessToken()).build())
//                .handle(ResponseWrapper::new)
//                .join();
//
//        if (response.exception() != null) return ResponseEntity.status(500).body(response.exception());
//
//        return ResponseEntity.ok(response.response());
//    }

    @PutMapping("/user/authentication")
    public ResponseEntity authentication(@RequestBody MultiFactorAuthenticationDto request) {
        AdminSetUserMfaPreferenceRequest.Builder mfaRequestBuilder = AdminSetUserMfaPreferenceRequest.builder()
                .userPoolId(userPool)
                .username(request.id());
        if (request.type() == APPLICATION) {
            return ResponseEntity.ok(Map.of("secretCode", associateSoftwareToken(request.accessToken())));
        } else if (request.type() == SMS) {
            mfaRequestBuilder.smsMfaSettings(
                    SMSMfaSettingsType.builder()
                            .enabled(request.enabled())
                            .preferredMfa(true)
                            .build()
            );
        }

        ResponseWrapper<AdminSetUserMfaPreferenceResponse, Throwable> response = cognitoClient
                .adminSetUserMFAPreference(mfaRequestBuilder.build())
                .handle(ResponseWrapper::new)
                .join();

        if (response.exception() != null) return ResponseEntity.status(500).body(response.exception());

        return ResponseEntity.noContent().build();
    }

    private String associateSoftwareToken(String accessToken) {
        AssociateSoftwareTokenRequest softwareTokenRequest = AssociateSoftwareTokenRequest.builder()
                .accessToken(accessToken)
                .build();
        return cognitoClient.associateSoftwareToken(softwareTokenRequest)
                .handle(ResponseWrapper::new)
                .join()
                .response()
                .secretCode();
    }

    @PostMapping("/user/authentication/application/verify")
    public ResponseEntity verifyApplication(@RequestBody VerifyApplicationRequest request) {
        VerifySoftwareTokenRequest softwareTokenRequest = VerifySoftwareTokenRequest.builder()
                .accessToken(request.accessToken())
                .friendlyDeviceName(request.deviceName())
                .userCode(request.code())
                .build();
        ResponseWrapper<VerifySoftwareTokenResponse, Throwable> response = cognitoClient.verifySoftwareToken(softwareTokenRequest)
                .handle(ResponseWrapper::new)
                .join();

        if (response.exception() != null) return ResponseEntity.status(500).body(response.exception());

        AdminSetUserMfaPreferenceRequest.Builder mfaRequestBuilder = AdminSetUserMfaPreferenceRequest.builder()
                .userPoolId(userPool)
                .softwareTokenMfaSettings(SoftwareTokenMfaSettingsType.builder()
                        .enabled(true)
                        .preferredMfa(true)
                        .build())
                .username(request.id());

        ResponseWrapper<AdminSetUserMfaPreferenceResponse, Throwable> responseWrapper = cognitoClient
                .adminSetUserMFAPreference(mfaRequestBuilder.build())
                .handle(ResponseWrapper::new)
                .join();

        if (responseWrapper.exception() != null) return ResponseEntity.status(500).body(responseWrapper.exception());

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/changePassword")
    public ResponseEntity changePassword(@RequestBody LoginRequest request) {
        AdminSetUserPasswordRequest passwordRequest = AdminSetUserPasswordRequest.builder()
                .userPoolId(userPool)
                .password(request.password())
                .username(request.username())
                .permanent(true)
                .build();

        ResponseWrapper<AdminSetUserPasswordResponse, Throwable> response = cognitoClient.adminSetUserPassword(passwordRequest)
                .handle(ResponseWrapper::new)
                .join();

        return ResponseEntity.ok(response.response());
    }

}
