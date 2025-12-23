package ai.ozzu.api.service;

import ai.ozzu.api.persistence.entity.WagerEntity;
import ai.ozzu.api.persistence.enums.WagerStateEventEntity;
import ai.ozzu.api.persistence.enums.WagerStatus;
import ai.ozzu.api.persistence.repo.WagerRepository;
import ai.ozzu.api.persistence.repo.WagerStateEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Service
public class WagerStatusService {

    private final WagerRepository wagerRepo;
    private final WagerStateEventRepository wagerStateEventRepo;

    public WagerStatusService(WagerRepository wagerRepo, WagerStateEventRepository stateRepo) {
        this.wagerRepo = wagerRepo;
        this.wagerStateEventRepo = stateRepo;
    }

    @Transactional
    public void changeWagerStatus(
            UUID eventId,
            UUID wagerId,
            WagerStatus newStatus,
            UUID actorUserId,
            String reason,
            Map<String, Object> metadata
    ) {
        WagerEntity w = wagerRepo
                .lockByEventIdAndId(eventId, wagerId)
                .orElseThrow(() -> new IllegalArgumentException("Wager not found"));

        WagerStatus oldStatus = w.getStatus();

        if (oldStatus == newStatus) {
            return; // idempotent
        }

        if (!WagerStatus.canTransition(oldStatus, newStatus)) {
            throw new IllegalStateException(
                    "Invalid transition " + oldStatus + " → " + newStatus
            );
        }

        w.setStatus(newStatus);
        w.setUpdatedAt(OffsetDateTime.now());

        wagerRepo.save(w);

        wagerStateEventRepo.save(
                WagerStateEventEntity.of(
                        w, oldStatus, newStatus, actorUserId, reason, metadata
                )
        );
    }
}
