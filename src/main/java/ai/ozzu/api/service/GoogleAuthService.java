package ai.ozzu.api.service;

import ai.ozzu.api.exceptions.UnauthorizedException;
import ai.ozzu.api.generated.model.AuthResponse;
import ai.ozzu.api.generated.model.GoogleLoginRequest;
import ai.ozzu.api.persistence.entity.UserEntity;
import ai.ozzu.api.persistence.enums.AuthProvider;
import ai.ozzu.api.persistence.repo.UserRepository;
import ai.ozzu.api.security.GoogleTokenVerifierService;
import ai.ozzu.api.security.JwtService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
public class GoogleAuthService {

    private final GoogleTokenVerifierService googleVerifier;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    public GoogleAuthService(GoogleTokenVerifierService googleVerifier,
                             UserRepository userRepository,
                             JwtService jwtService) {
        this.googleVerifier = googleVerifier;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse googleLogin(GoogleLoginRequest req) {
        var payload = googleVerifier.verifyOrNull(req.getIdToken());
        if (payload == null) {
            throw new UnauthorizedException("Invalid Google ID token");
        }

        String googleSub = payload.getSubject();
        String email = payload.getEmail();
        String name = (String) payload.get("name");

        UserEntity user = userRepository
                .findByProviderAndProviderUserId(AuthProvider.GOOGLE, googleSub)
                .orElseGet(() -> createGoogleUser(req, googleSub, email, name));

        String jwt = jwtService.issue(user.getId(), user.getEmail(), user.getProvider().name());

        AuthResponse out = new AuthResponse();
        out.setAccessToken(jwt);
        out.setTokenType("Bearer");
        out.setExpiresInSeconds(jwtService.getTtlSeconds());
        return out;
    }

    private UserEntity createGoogleUser(GoogleLoginRequest req, String googleSub, String email, String name) {
        UserEntity u = new UserEntity();
        u.setId(UUID.randomUUID());
        u.setProvider(AuthProvider.GOOGLE);
        u.setProviderUserId(googleSub);
        u.setEmail(email);
        u.setDisplayName(name);

        // Referral (if provided)
        if (req.getReferralCode() != null && !req.getReferralCode().isBlank()) {
            userRepository.findByReferralCode(req.getReferralCode().trim().toUpperCase(Locale.ROOT))
                    .ifPresent(u::setReferredByUser);
        }

        // Generate unique referralCode for this new user
        u.setReferralCode(generateUniqueReferralCode());

        return userRepository.save(u);
    }

    private String generateUniqueReferralCode() {
        // MVP: try a few times; collision is extremely unlikely but handled
        for (int i = 0; i < 10; i++) {
            String code = UUID.randomUUID()
                    .toString()
                    .replace("-", "")
                    .substring(0, 8)
                    .toUpperCase(Locale.ROOT);

            if (!userRepository.existsByReferralCode(code)) {
                return code;
            }
        }
        throw new IllegalStateException("Could not generate unique referral code");
    }
}