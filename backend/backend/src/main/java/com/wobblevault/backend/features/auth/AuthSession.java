package com.wobblevault.backend.features.auth;

public record AuthSession(String accessToken, AuthUserDTO user) {
}
