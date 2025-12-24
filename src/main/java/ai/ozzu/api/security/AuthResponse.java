package ai.ozzu.api.security;

public record AuthResponse(String accessToken, String tokenType, long expiresInSeconds) {}
