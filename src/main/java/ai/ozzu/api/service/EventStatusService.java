package ai.ozzu.api.service;

import ai.ozzu.api.generated.model.EventStatusUpdateResponse;
import ai.ozzu.api.persistence.entity.EventEntity;
import ai.ozzu.api.persistence.entity.WagerEntity;
import ai.ozzu.api.persistence.enums.EventStatus;
import ai.ozzu.api.persistence.enums.WagerOutcome;
import ai.ozzu.api.persistence.enums.WagerStateEventEntity;
import ai.ozzu.api.persistence.enums.WagerStatus;
import ai.ozzu.api.persistence.repo.EventRepository;
import ai.ozzu.api.persistence.repo.WagerRepository;
import ai.ozzu.api.persistence.repo.WagerStateEventRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class EventStatusService {

    private final EventRepository eventRepo;
    private final WagerRepository wagerRepo;
    private final WagerStateEventRepository wagerStateEventRepo;
    private final WagerSettlementService wagerSettlementService;
    private final TokenLedgerService tokenLedgerService;

    public EventStatusService(
            EventRepository eventRepo,
            WagerRepository wagerRepo,
            WagerStateEventRepository wagerStateEventRepo,
            WagerSettlementService wagerSettlementService,
            TokenLedgerService tokenLedgerService
    ) {
        this.eventRepo = eventRepo;
        this.wagerRepo = wagerRepo;
        this.wagerStateEventRepo = wagerStateEventRepo;
        this.wagerSettlementService = wagerSettlementService;
        this.tokenLedgerService = tokenLedgerService;
    }

    @Transactional
    public EventStatusUpdateResponse changeEventStatus(
            UUID domainId,
            UUID eventId,
            EventStatus newStatus,
            UUID actorUserId
    ) {
        EventEntity event = eventRepo.findByIdAndDomain_Id(eventId, domainId)
                .orElseThrow(() -> new EntityNotFoundException("Event not found"));

        EventStatus oldStatus = event.getStatus();

        if (oldStatus == newStatus) {
            return buildResponse(event, oldStatus, 0, 0, 0);
        }

        if (!EventStatus.canTransition(oldStatus, newStatus)) {
            throw new IllegalStateException(
                    "Invalid event transition " + oldStatus + " → " + newStatus
            );
        }

        int lockedWagersCount = 0;
        int settledWagersCount = 0;
        int canceledWagersCount = 0;

        event.setStatus(newStatus);

        if (newStatus == EventStatus.LIVE) {
            event.setCanceled(false);
            event.setCompleted(false);

            lockedWagersCount = lockPlacedWagers(eventId, actorUserId);
        }

        if (newStatus == EventStatus.COMPLETED) {
            event.setCanceled(false);
            event.setCompleted(true);

            settledWagersCount = wagerSettlementService.settleEvent(eventId, actorUserId);
        }

        if (newStatus == EventStatus.CANCELED) {
            event.setCanceled(true);
            event.setCompleted(false);

            canceledWagersCount = voidOpenWagers(eventId, actorUserId);
        }

        EventEntity saved = eventRepo.saveAndFlush(event);

        return buildResponse(
                saved,
                oldStatus,
                lockedWagersCount,
                settledWagersCount,
                canceledWagersCount
        );
    }

    private int lockPlacedWagers(UUID eventId, UUID actorUserId) {
        List<WagerEntity> wagers =
                wagerRepo.lockByEventIdAndStatus(eventId, WagerStatus.PLACED);

        int count = 0;

        for (WagerEntity wager : wagers) {
            WagerStatus oldStatus = wager.getStatus();

            if (!WagerStatus.canTransition(oldStatus, WagerStatus.LOCKED)) {
                continue;
            }

            wager.setStatus(WagerStatus.LOCKED);
            wager.setUpdatedAt(OffsetDateTime.now());

            wagerStateEventRepo.save(
                    WagerStateEventEntity.of(
                            wager,
                            oldStatus,
                            WagerStatus.LOCKED,
                            actorUserId,
                            "event_started",
                            Map.of(
                                    "eventId", eventId.toString(),
                                    "source", "event_status_service"
                            )
                    )
            );

            count++;
        }

        return count;
    }

    private int voidOpenWagers(UUID eventId, UUID actorUserId) {
        int count = 0;

        count += cancelWagersByStatus(eventId, WagerStatus.CREATED, actorUserId);
        count += cancelWagersByStatus(eventId, WagerStatus.PLACED, actorUserId);
        count += cancelWagersByStatus(eventId, WagerStatus.LOCKED, actorUserId);

        return count;
    }

    private int cancelWagersByStatus(
            UUID eventId,
            WagerStatus currentStatus,
            UUID actorUserId
    ) {
        List<WagerEntity> wagers =
                wagerRepo.lockByEventIdAndStatus(eventId, currentStatus);

        int count = 0;

        for (WagerEntity wager : wagers) {
            WagerStatus oldStatus = wager.getStatus();

            if (!WagerStatus.canTransition(oldStatus, WagerStatus.CANCELED)) {
                continue;
            }

            wager.setStatus(WagerStatus.CANCELED);
            wager.setOutcome(WagerOutcome.VOID);
            wager.setPayoutTokens(0);
            wager.setUpdatedAt(OffsetDateTime.now());

            wagerStateEventRepo.save(
                    WagerStateEventEntity.of(
                            wager,
                            oldStatus,
                            WagerStatus.CANCELED,
                            actorUserId,
                            "event_canceled",
                            Map.of(
                                    "eventId", eventId.toString(),
                                    "outcome", WagerOutcome.VOID.name(),
                                    "source", "event_status_service"
                            )
                    )
            );

            if (wager.getStakeTokens() > 0) {
                tokenLedgerService.creditRefundOnce(
                        wager.getUserId(),
                        wager.getDomainId(),
                        wager.getEventId(),
                        wager.getId(),
                        wager.getStakeTokens(),
                        "wager_refund_" + wager.getEventId() + "_" + wager.getId()
                );
            }

            count++;
        }

        return count;
    }

    private EventStatusUpdateResponse buildResponse(
            EventEntity event,
            EventStatus previousStatus,
            int lockedWagersCount,
            int settledWagersCount,
            int canceledWagersCount
    ) {
        EventStatusUpdateResponse response = new EventStatusUpdateResponse();

        response.setId(event.getId());
        response.setDomainId(event.getDomain().getId());
        response.setStatus(
                ai.ozzu.api.generated.model.EventStatus.valueOf(event.getStatus().name())
        );
        response.setPreviousStatus(
                ai.ozzu.api.generated.model.EventStatus.valueOf(previousStatus.name())
        );
        response.setLockedWagersCount(lockedWagersCount);
        response.setSettledWagersCount(settledWagersCount);
        response.setCanceledWagersCount(canceledWagersCount);
        response.setTimeUpdated(event.getUpdatedAt());

        return response;
    }
}