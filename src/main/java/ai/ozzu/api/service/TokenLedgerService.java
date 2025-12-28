package ai.ozzu.api.service;

import ai.ozzu.api.persistence.entity.*;
import ai.ozzu.api.persistence.enums.TokenTxnType;
import ai.ozzu.api.persistence.repo.TokenLedgerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class TokenLedgerService {

    private static final Logger log = LoggerFactory.getLogger(TokenLedgerService.class);

    private final TokenLedgerRepository tokenLedgerRepo;

    public TokenLedgerService(TokenLedgerRepository tokenLedgerRepo) {
        this.tokenLedgerRepo = tokenLedgerRepo;
    }

    @Transactional(readOnly = true)
    public long balance(UUID userId) {
        return tokenLedgerRepo.getBalance(userId);
    }

    /**
     * Idempotent debit:
     * - If (userId, WAGER_STAKE_DEBIT, idemKey) exists -> no-op
     * - Else check balance and insert a negative amount row
     */
    @Transactional
    public void debitStakeOnce(
            UserEntity user,
            DomainEntity domain,
            EventEntity event,
            WagerEntity wager,
            int stakeTokens,
            String idemKey
    ) {

        log.info("token.debit.start userId={} txnType={} stakeTokens={} idemKeyPresent={} wagerId={}",
                user.getId(), TokenTxnType.WAGER_STAKE_DEBIT, stakeTokens,
                (idemKey != null && !idemKey.isBlank()),
                (wager != null ? wager.getId() : null));

        if (stakeTokens <= 0) {
            log.info("token.debit.skipZero userId={} wagerId={}", user.getId(), (wager != null ? wager.getId() : null));
            return;
        }

        if (idemKey != null && !idemKey.isBlank()) {
            var existing = tokenLedgerRepo.findByUser_IdAndTxnTypeAndIdempotencyKey(
                    user.getId(), TokenTxnType.WAGER_STAKE_DEBIT, idemKey
            );
            if (existing.isPresent()) {
                log.info("token.debit.idempotentHit userId={} idemKey={} ledgerId={}",
                        user.getId(), idemKey, existing.get().getId());
                return;
            }
        }

        long bal = tokenLedgerRepo.getBalance(user.getId());
        log.info("token.debit.balanceCheck userId={} balance={} required={}", user.getId(), bal, stakeTokens);
        if (bal < stakeTokens) {
            log.warn("token.debit.insufficient userId={} balance={} required={} wagerId={}",
                    user.getId(), bal, stakeTokens, (wager != null ? wager.getId() : null));
            throw new IllegalStateException("Insufficient tokens. balance=" + bal + ", stake=" + stakeTokens);
        }

        TokenLedgerEntity row = new TokenLedgerEntity();
        row.setUser(user);
        row.setDomain(domain);
        row.setEvent(event);
        row.setWager(wager);
        row.setTxnType(TokenTxnType.WAGER_STAKE_DEBIT);
        row.setAmount(-stakeTokens);
        row.setReason("Wager stake debit");
        row.setIdempotencyKey(idemKey);
        row.setMetadata(Map.of(
                "wagerEventId", wager.getEventId().toString(),
                "wagerId", wager.getId().toString()
        ));

        tokenLedgerRepo.save(row);
        log.info("token.debit.saved userId={} amount={} txnType={} ledgerId={} wagerId={}",
                user.getId(), -stakeTokens, TokenTxnType.WAGER_STAKE_DEBIT, row.getId(), (wager != null ? wager.getId() : null));
    }
}