package ai.ozzu.api.service;

import ai.ozzu.api.persistence.entity.WagerEntity;
import ai.ozzu.api.persistence.enums.WagerStateEventEntity;
import ai.ozzu.api.persistence.enums.WagerStatus;
import ai.ozzu.api.persistence.repo.WagerRepository;
import ai.ozzu.api.persistence.repo.WagerStateEventRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Service
public class WagerStatusService {

    private final WagerRepository wagerRepo;
    private final WagerStateEventRepository wagerStateEventRepo;

    public WagerStatusService(
            WagerRepository wagerRepo,
            WagerStateEventRepository wagerStateEventRepo
    ) {
        this.wagerRepo = wagerRepo;
        this.wagerStateEventRepo = wagerStateEventRepo;
    }

    @Transactional
    public WagerEntity changeWagerStatus(
            UUID eventId,
            UUID wagerId,
            WagerStatus newStatus,
            UUID actorUserId,
            String reason,
            Map<String, Object> metadata
    ) {
        WagerEntity wager = wagerRepo.lockByEventIdAndId(eventId, wagerId)
                .orElseThrow(() -> new EntityNotFoundException("Wager not found"));

        WagerStatus oldStatus = wager.getStatus();

        if (oldStatus == newStatus) {
            return wager;
        }

        if (!WagerStatus.canTransition(oldStatus, newStatus)) {
            throw new IllegalStateException(
                    "Invalid wager transition " + oldStatus + " → " + newStatus
            );
        }

        wager.setStatus(newStatus);
        wager.setUpdatedAt(OffsetDateTime.now());

        wagerStateEventRepo.save(
                WagerStateEventEntity.of(
                        wager,
                        oldStatus,
                        newStatus,
                        actorUserId,
                        reason,
                        metadata == null ? Map.of() : metadata
                )
        );

        return wager;
    }
}