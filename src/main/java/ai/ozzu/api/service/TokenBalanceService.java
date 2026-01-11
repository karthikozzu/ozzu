package ai.ozzu.api.service;

import ai.ozzu.api.exceptions.EntityNotFoundException;
import ai.ozzu.api.generated.model.TokenBalanceResponse;
import ai.ozzu.api.persistence.repo.TokenLedgerRepository;
import ai.ozzu.api.persistence.repo.UserRepository;
import ai.ozzu.api.persistence.repo.UserTokenBalanceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class TokenBalanceService {

    private static final Logger log = LoggerFactory.getLogger(TokenBalanceService.class);

    private final UserRepository userRepository;
    private final UserTokenBalanceRepository userTokenBalanceRepository;
    private final TokenLedgerRepository tokenLedgerRepository;

    public TokenBalanceService(
            UserRepository userRepository,
            UserTokenBalanceRepository userTokenBalanceRepository,
            TokenLedgerRepository tokenLedgerRepository
    ) {
        this.userRepository = userRepository;
        this.userTokenBalanceRepository = userTokenBalanceRepository;
        this.tokenLedgerRepository = tokenLedgerRepository;
    }

    /**
     * MVP: balance is read-only.
     * Source of truth: user_token_balances.balance (fast).
     * Fallback: SUM(token_ledger.amount) if balance row missing.
     */
    @Transactional(readOnly = true)
    public TokenBalanceResponse getTokenBalance(UUID userId) {
        log.info("Fetching token balance for userId={}", userId);

        ensureUserExists(userId);

        BalanceResult result = resolveBalance(userId);

        int safeBalance = (int) Math.min(Integer.MAX_VALUE, Math.max(0, result.balance));

        log.info(
                "Resolved token balance for userId={} balance={} source={}",
                userId,
                safeBalance,
                result.source
        );

        TokenBalanceResponse resp = new TokenBalanceResponse();
        resp.setUserId(userId);
        resp.setBalanceTokens(safeBalance);

        // Optional internalProperties (safe + useful for debugging)
        Map<String, Object> ip = new LinkedHashMap<>();
        ip.put("source", result.source);
        ip.put("asOf", OffsetDateTime.now().toString());
        resp.setInternalProperties(ip);

        return resp;
    }

    private BalanceResult resolveBalance(UUID userId) {
        // Try balance table first
        return userTokenBalanceRepository.findById(userId)
                .map(b -> {
                    long balance = b.getBalance() == 0 ? 0L : b.getBalance();
                    log.debug(
                            "Token balance resolved from user_token_balances: userId={} balance={}",
                            userId,
                            balance
                    );
                    return new BalanceResult(balance, "user_token_balances");
                })
                .orElseGet(() -> {
                    long ledgerSum = tokenLedgerRepository.getBalance(userId);
                    log.debug(
                            "Token balance resolved via ledger fallback: userId={} ledgerSum={}",
                            userId,
                            ledgerSum
                    );
                    return new BalanceResult(ledgerSum, "token_ledger_sum_fallback");
                });
    }

    private void ensureUserExists(UUID userId) {
        if (!userRepository.existsById(userId)) {
            log.warn("User not found while fetching token balance: userId={}", userId);
            throw new EntityNotFoundException("User not found: " + userId);
        }
    }

    private static final class BalanceResult {
        private final long balance;
        private final String source;

        private BalanceResult(long balance, String source) {
            this.balance = balance;
            this.source = source;
        }
    }
}