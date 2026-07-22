package ai.ozzu.api.service;

import ai.ozzu.api.exceptions.BadRequestException;
import ai.ozzu.api.exceptions.EntityNotFoundException;
import ai.ozzu.api.generated.model.LoungeEntry;
import ai.ozzu.api.generated.model.LoungeEntryCreateRequest;
import ai.ozzu.api.persistence.entity.EventLoungeEntity;
import ai.ozzu.api.persistence.entity.LoungeEntryEntity;
import ai.ozzu.api.persistence.entity.UserEntity;
import ai.ozzu.api.persistence.entity.WagerEntity;
import ai.ozzu.api.persistence.entity.WagerInLoungeEntity;
import ai.ozzu.api.persistence.repo.EventLoungeRepository;
import ai.ozzu.api.persistence.repo.LoungeEntryRepository;
import ai.ozzu.api.persistence.repo.UserRepository;
import ai.ozzu.api.persistence.repo.WagerInLoungeRepository;
import ai.ozzu.api.persistence.repo.WagerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
public class LoungeEntryService {

    private static final Logger log = LoggerFactory.getLogger(LoungeEntryService.class);

    private final EventLoungeRepository eventLoungeRepository;
    private final LoungeEntryRepository loungeEntryRepository;

    private final UserRepository userRepository;
    private final WagerRepository wagerRepository;
    private final WagerInLoungeRepository wagerInLoungeRepository;

    public LoungeEntryService(
            EventLoungeRepository eventLoungeRepository,
            LoungeEntryRepository loungeEntryRepository,
            UserRepository userRepository,
            WagerRepository wagerRepository,
            WagerInLoungeRepository wagerInLoungeRepository
    ) {
        this.eventLoungeRepository = eventLoungeRepository;
        this.loungeEntryRepository = loungeEntryRepository;
        this.userRepository = userRepository;
        this.wagerRepository = wagerRepository;
        this.wagerInLoungeRepository = wagerInLoungeRepository;
    }

    @Transactional
    public LoungeEntry createEntry(
            UUID domainId,
            UUID eventId,
            UUID eventLoungeId,
            UUID userId,
            LoungeEntryCreateRequest req
    ) {
        log.info("loungeEntry.create.start domainId={} eventId={} eventLoungeId={} userId={} wagerIdPresent={}",
                domainId, eventId, eventLoungeId, userId, (req != null && req.getWagerId() != null));

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));

        EventLoungeEntity eventLounge = eventLoungeRepository
                .findByIdAndDomain_IdAndEvent_Id(eventLoungeId, domainId, eventId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "EventLounge not found for domain/event: " + eventLoungeId
                ));

        if (!eventLounge.isActive()) {
            throw new IllegalStateException("Event lounge is not active: " + eventLoungeId);
        }

        WagerEntity wager = null;

        if (req != null && req.getWagerId() != null) {
            wager = wagerRepository.findByEventIdAndId(eventId, req.getWagerId())
                    .orElseThrow(() -> new EntityNotFoundException("Wager not found: " + req.getWagerId()));

            if (wager.getDomainId() == null || !domainId.equals(wager.getDomainId())) {
                throw new BadRequestException("Wager does not belong to domain: " + domainId);
            }

            if (wager.getUserId() == null || !userId.equals(wager.getUserId())) {
                throw new BadRequestException("Wager does not belong to current user");
            }
        }

        LoungeEntryEntity loungeEntry = loungeEntryRepository
                .findByEventLounge_IdAndUser_Id(eventLoungeId, userId)
                .orElseGet(() -> {
                    LoungeEntryEntity e = new LoungeEntryEntity();
                    e.setEventLounge(eventLounge);
                    e.setUser(user);
                    e.setJoinedAt(OffsetDateTime.now());

                    try {
                        LoungeEntryEntity saved = loungeEntryRepository.save(e);
                        log.info("loungeEntry.create.success domainId={} eventId={} eventLoungeId={} userId={} loungeEntryId={}",
                                domainId, eventId, eventLoungeId, userId, saved.getId());
                        return saved;
                    } catch (DataIntegrityViolationException dup) {
                        return loungeEntryRepository
                                .findByEventLounge_IdAndUser_Id(eventLoungeId, userId)
                                .orElseThrow(() -> dup);
                    }
                });

        if (wager != null) {
            createWagerInLoungeIfMissing(eventLounge, wager);
        }

        return toApi(loungeEntry, wager != null ? wager.getId() : null);
    }

    private void createWagerInLoungeIfMissing(
            EventLoungeEntity eventLounge,
            WagerEntity wager
    ) {
        wagerInLoungeRepository
                .findByEventLounge_IdAndWager_Id(eventLounge.getId(), wager.getId())
                .ifPresentOrElse(
                        existing -> log.info(
                                "wagerInLounge.create.idempotentHit eventLoungeId={} wagerId={} wagerInLoungeId={}",
                                eventLounge.getId(), wager.getId(), existing.getId()
                        ),
                        () -> {
                            WagerInLoungeEntity wil = new WagerInLoungeEntity();
                            wil.setEventLounge(eventLounge);
                            wil.setWager(wager);
                            wil.setCreatedAt(OffsetDateTime.now());
                            WagerInLoungeEntity saved = wagerInLoungeRepository.save(wil);

                            log.info(
                                    "wagerInLounge.create.success eventLoungeId={} wagerId={} wagerInLoungeId={}",
                                    eventLounge.getId(), wager.getId(), saved.getId()
                            );
                        }
                );
    }

    private LoungeEntry toApi(LoungeEntryEntity e, UUID wagerId) {
        LoungeEntry api = new LoungeEntry();

        api.setId(e.getId());
        api.setEventLoungeId(e.getEventLounge() != null ? e.getEventLounge().getId() : null);
        api.setUserId(e.getUser() != null ? e.getUser().getId() : null);
        api.setWagerId(wagerId);
        api.setTimeCreated(e.getJoinedAt());

        Map<String, Object> ip = new LinkedHashMap<>();
        if (wagerId != null) {
            ip.put("wagerId", wagerId.toString());
        }
        api.setInternalProperties(ip);

        return api;
    }

    private LoungeEntry toApi(LoungeEntryEntity e, LoungeEntryCreateRequest req) {
        LoungeEntry api = new LoungeEntry();
        api.setId(e.getId());
        api.setEventLoungeId(e.getEventLounge() != null ? e.getEventLounge().getId() : null);
        api.setUserId(e.getUser() != null ? e.getUser().getId() : null);

        // Put it into internalProperties for now (keeps schema compatibility for clients).
        UUID wagerId = (req != null ? req.getWagerId() : null);
        api.setWagerId(wagerId);

        api.setTimeCreated(e.getJoinedAt()); // spec uses timeCreated; joinedAt is the closest match

        Map<String, Object> ip = new LinkedHashMap<>();
        if (req != null && req.getInternalProperties() != null) {
            ip.putAll(req.getInternalProperties());
        }
        if (wagerId != null) {
            ip.put("wagerId", wagerId.toString());
        }
        api.setInternalProperties(ip);

        return api;
    }

    @Transactional(readOnly = true)
    public List<LoungeEntry> getLoungeEntries(
            UUID domainId,
            UUID eventId,
            UUID eventLoungeId,
            UUID userId
    ) {
        log.info("loungeEntry.get.start userId={} eventLoungeId={} eventId={} domainId={}",
                userId, eventLoungeId, eventId, domainId);

        EventLoungeEntity eventLounge = eventLoungeRepository
                .findByIdAndDomain_IdAndEvent_Id(eventLoungeId, domainId, eventId)
                .orElseThrow(() -> new EntityNotFoundException("Event Lounge Not Found: " + eventLoungeId));

        List<LoungeEntryEntity> loungeEntries;
        List<WagerInLoungeEntity> wagerInLounges;

        if (userId != null) {
            loungeEntries = loungeEntryRepository
                    .findAllByEventLounge_IdAndUser_Id(eventLounge.getId(), userId);

            wagerInLounges = wagerInLoungeRepository
                    .findByEventLounge_IdAndWager_UserId(eventLounge.getId(), userId);
        } else {
            loungeEntries = loungeEntryRepository
                    .findAllByEventLounge_Id(eventLounge.getId());

            wagerInLounges = wagerInLoungeRepository
                    .findByEventLounge_Id(eventLounge.getId());
        }

        Map<UUID, UUID> wagerIdByUserId = new LinkedHashMap<>();

        for (WagerInLoungeEntity wil : wagerInLounges) {
            if (wil.getWager() == null) {
                continue;
            }

            UUID wagerUserId = wil.getWager().getUserId();
            UUID wagerId = wil.getWager().getId();

            if (wagerUserId != null && wagerId != null) {
                wagerIdByUserId.putIfAbsent(wagerUserId, wagerId);
            }
        }

        List<LoungeEntry> response = new ArrayList<>();

        for (LoungeEntryEntity loungeEntryEntity : loungeEntries) {
            UUID entryUserId = loungeEntryEntity.getUser() != null
                    ? loungeEntryEntity.getUser().getId()
                    : null;

            UUID wagerId = entryUserId != null
                    ? wagerIdByUserId.get(entryUserId)
                    : null;

            response.add(toApi(loungeEntryEntity, wagerId));
        }

        return response;
    }
}
