package ai.ozzu.api.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Service
public class JwtService {

    private final SecretKey key;
    private final String issuer;
    private final long ttlSeconds;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.issuer:ozzu}") String issuer,
            @Value("${app.jwt.ttlMinutes:120}") long ttlMinutes
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.issuer = issuer;
        this.ttlSeconds = ttlMinutes * 60;
    }

    public String issue(UUID userId, String email, String provider) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(ttlSeconds);

        return Jwts.builder()
                .setIssuer(issuer)
                .setSubject(userId.toString())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(exp))
                .addClaims(Map.of(
                        "email", email == null ? "" : email,
                        "provider", provider
                ))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public int getTtlSeconds() {
        return (int) ttlSeconds;
    }

    public SecretKey getKey() {
        return key;
    }

    public String getIssuer() {
        return issuer;
    }
}