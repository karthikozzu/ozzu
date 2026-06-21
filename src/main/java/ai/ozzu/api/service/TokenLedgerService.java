package ai.ozzu.api.service;

import ai.ozzu.api.persistence.entity.DomainEntity;
import ai.ozzu.api.persistence.entity.EventEntity;
import ai.ozzu.api.persistence.entity.TokenLedgerEntity;
import ai.ozzu.api.persistence.entity.UserEntity;
import ai.ozzu.api.persistence.entity.WagerEntity;
import ai.ozzu.api.persistence.enums.TokenTxnType;
import ai.ozzu.api.persistence.repo.DomainRepository;
import ai.ozzu.api.persistence.repo.EventRepository;
import ai.ozzu.api.persistence.repo.TokenLedgerRepository;
import ai.ozzu.api.persistence.repo.UserRepository;
import ai.ozzu.api.persistence.repo.WagerRepository;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
public class TokenLedgerService {

    private static final Logger log = LoggerFactory.getLogger(TokenLedgerService.class);

    private final TokenLedgerRepository tokenLedgerRepo;
    private final UserRepository userRepo;
    private final DomainRepository domainRepo;
    private final EventRepository eventRepo;
    private final WagerRepository wagerRepo;

    public TokenLedgerService(
            TokenLedgerRepository tokenLedgerRepo,
            UserRepository userRepo,
            DomainRepository domainRepo,
            EventRepository eventRepo,
            WagerRepository wagerRepo
    ) {
        this.tokenLedgerRepo = tokenLedgerRepo;
        this.userRepo = userRepo;
        this.domainRepo = domainRepo;
        this.eventRepo = eventRepo;
        this.wagerRepo = wagerRepo;
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "balance", key = "#userId")
    public long balance(UUID userId) {
        return tokenLedgerRepo.getBalance(userId);
    }

    /**
     * Idempotent debit:
     * - If (userId, WAGER_STAKE_DEBIT, idemKey) exists -> no-op
     * - Else check ledger balance and insert negative amount row.
     */
    @Transactional
    @CacheEvict(value = "balance", key = "#user.id")
    public void debitStakeOnce(
            UserEntity user,
            DomainEntity domain,
            EventEntity event,
            WagerEntity wager,
            int stakeTokens,
            String idemKey
    ) {
        UUID wagerId = wager != null ? wager.getId() : null;

        log.info(
                "token.debit.start userId={} txnType={} stakeTokens={} idemKeyPresent={} wagerId={}",
                user.getId(),
                TokenTxnType.WAGER_STAKE_DEBIT,
                stakeTokens,
                idemKey != null && !idemKey.isBlank(),
                wagerId
        );

        if (stakeTokens <= 0) {
            log.info("token.debit.skipZero userId={} wagerId={}", user.getId(), wagerId);
            return;
        }

        if (idemKey != null && !idemKey.isBlank()) {
            var existing = tokenLedgerRepo.findByUser_IdAndTxnTypeAndIdempotencyKey(
                    user.getId(),
                    TokenTxnType.WAGER_STAKE_DEBIT,
                    idemKey
            );

            if (existing.isPresent()) {
                log.info(
                        "token.debit.idempotentHit userId={} idemKey={} ledgerId={}",
                        user.getId(),
                        idemKey,
                        existing.get().getId()
                );
                return;
            }
        }

        long currentBalance = tokenLedgerRepo.getBalance(user.getId());

        log.info(
                "token.debit.balanceCheck userId={} balance={} required={}",
                user.getId(),
                currentBalance,
                stakeTokens
        );

        if (currentBalance < stakeTokens) {
            log.warn(
                    "token.debit.insufficient userId={} balance={} required={} wagerId={}",
                    user.getId(),
                    currentBalance,
                    stakeTokens,
                    wagerId
            );

            throw new IllegalStateException(
                    "Insufficient tokens. balance=" + currentBalance + ", stake=" + stakeTokens
            );
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

        Map<String, Object> metadata = wager != null
                ? Map.of(
                "wagerEventId", wager.getEventId().toString(),
                "wagerId", wager.getId().toString(),
                "source", "wager_create"
        )
                : Map.of("source", "wager_create");

        row.setMetadata(metadata);

        tokenLedgerRepo.save(row);

        log.info(
                "token.debit.saved userId={} amount={} txnType={} ledgerId={} wagerId={}",
                user.getId(),
                -stakeTokens,
                TokenTxnType.WAGER_STAKE_DEBIT,
                row.getId(),
                wagerId
        );
    }

    /**
     * Credit payout once after wager WON.
     */
    @Transactional
    @CacheEvict(value = "balance", key = "#userId")
    public void creditPayoutOnce(
            UUID userId,
            UUID domainId,
            UUID eventId,
            UUID wagerId,
            int amount,
            String idempotencyKey
    ) {
        creditOnce(
                userId,
                domainId,
                eventId,
                wagerId,
                amount,
                TokenTxnType.WAGER_PAYOUT_CREDIT,
                "Wager payout credit",
                idempotencyKey
        );
    }

    /**
     * Credit refund once after wager VOID / event canceled.
     */
    @Transactional
    @CacheEvict(value = "balance", key = "#userId")
    public void creditRefundOnce(
            UUID userId,
            UUID domainId,
            UUID eventId,
            UUID wagerId,
            int amount,
            String idempotencyKey
    ) {
        creditOnce(
                userId,
                domainId,
                eventId,
                wagerId,
                amount,
                TokenTxnType.WAGER_REFUND_CREDIT,
                "Wager refund credit",
                idempotencyKey
        );
    }

    private void creditOnce(
            UUID userId,
            UUID domainId,
            UUID eventId,
            UUID wagerId,
            int amount,
            TokenTxnType txnType,
            String reason,
            String idempotencyKey
    ) {
        if (amount <= 0) {
            log.info(
                    "token.credit.skipZero userId={} txnType={} amount={} wagerId={}",
                    userId,
                    txnType,
                    amount,
                    wagerId
            );
            return;
        }

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var existing = tokenLedgerRepo.findByUser_IdAndTxnTypeAndIdempotencyKey(
                    userId,
                    txnType,
                    idempotencyKey
            );

            if (existing.isPresent()) {
                log.info(
                        "token.credit.idempotentHit userId={} txnType={} idemKey={} ledgerId={}",
                        userId,
                        txnType,
                        idempotencyKey,
                        existing.get().getId()
                );
                return;
            }
        }

        UserEntity user = userRepo.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));

        DomainEntity domain = domainRepo.findById(domainId)
                .orElseThrow(() -> new EntityNotFoundException("Domain not found: " + domainId));

        EventEntity event = eventRepo.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Event not found: " + eventId));

        WagerEntity wager = wagerRepo.findByEventIdAndId(eventId, wagerId)
                .orElseThrow(() -> new EntityNotFoundException("Wager not found: " + wagerId));

        TokenLedgerEntity row = new TokenLedgerEntity();
        row.setUser(user);
        row.setDomain(domain);
        row.setEvent(event);
        row.setWager(wager);
        row.setTxnType(txnType);
        row.setAmount(amount);
        row.setReason(reason);
        row.setIdempotencyKey(idempotencyKey);
        row.setMetadata(
                Map.of(
                        "wagerEventId", eventId.toString(),
                        "wagerId", wagerId.toString(),
                        "source", "settlement_service"
                )
        );

        tokenLedgerRepo.save(row);

        log.info(
                "token.credit.saved userId={} amount={} txnType={} ledgerId={} wagerId={}",
                userId,
                amount,
                txnType,
                row.getId(),
                wagerId
        );
    }
}