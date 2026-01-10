package ai.ozzu.api.controller;

import ai.ozzu.api.generated.api.TokensApi;
import ai.ozzu.api.generated.model.TokenBalanceResponse;
import ai.ozzu.api.service.TokenBalanceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.NativeWebRequest;

import java.util.Optional;
import java.util.UUID;

@RestController
public class TokensController implements TokensApi {

    private final TokenBalanceService tokenBalanceService;

    public TokensController(TokenBalanceService tokenBalanceService) {
        this.tokenBalanceService = tokenBalanceService;
    }


    @Override
    public Optional<NativeWebRequest> getRequest() {
        return TokensApi.super.getRequest();
    }

    @Override
    public ResponseEntity<TokenBalanceResponse> ozzuUsersUserIdTokensBalanceGet(UUID userId) {
        TokenBalanceResponse resp = tokenBalanceService.getTokenBalance(userId);
        return ResponseEntity.ok(resp);    }
}
