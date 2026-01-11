package ai.ozzu.api.service;

import ai.ozzu.api.exceptions.EntityNotFoundException;
import ai.ozzu.api.generated.model.LoungeEntry;
import ai.ozzu.api.generated.model.LoungeEntryCreateRequest;
import ai.ozzu.api.persistence.entity.EventLoungeEntity;
import ai.ozzu.api.persistence.entity.LoungeEntryEntity;
import ai.ozzu.api.persistence.entity.UserEntity;
import ai.ozzu.api.persistence.repo.EventLoungeRepository;
import ai.ozzu.api.persistence.repo.LoungeEntryRepository;
import ai.ozzu.api.persistence.repo.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class LoungeEntryService {

    private static final Logger log = LoggerFactory.getLogger(LoungeEntryService.class);

    private final EventLoungeRepository eventLoungeRepository;
    private final LoungeEntryRepository loungeEntryRepository;
    private final UserRepository userRepository;

    public LoungeEntryService(
            EventLoungeRepository eventLoungeRepository,
            LoungeEntryRepository loungeEntryRepository,
            UserRepository userRepository
    ) {
        this.eventLoungeRepository = eventLoungeRepository;
        this.loungeEntryRepository = loungeEntryRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public LoungeEntry createEntry(UUID domainId, UUID eventId, UUID eventLoungeId, UUID userId, LoungeEntryCreateRequest req) {
        log.info("loungeEntry.create.start domainId={} eventId={} eventLoungeId={} userId={} wagerIdPresent={}",
                domainId, eventId, eventLoungeId, userId, (req != null && req.getWagerId() != null));

        // Ensure user exists
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));

        // Ensure event lounge exists and is in (domain,event)
        EventLoungeEntity eventLounge = eventLoungeRepository
                .findByIdAndDomain_IdAndEvent_Id(eventLoungeId, domainId, eventId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "EventLounge not found for domain/event: " + eventLoungeId
                ));

        if (!eventLounge.isActive()) {
            throw new IllegalStateException("Event lounge is not active: " + eventLoungeId);
        }

        // Idempotent behavior: if already entered, return existing
        return loungeEntryRepository.findByEventLounge_IdAndUser_Id(eventLoungeId, userId)
                .map(existing -> {
                    log.info("loungeEntry.create.idempotentHit eventLoungeId={} userId={} loungeEntryId={}",
                            eventLoungeId, userId, existing.getId());
                    return toApi(existing, req);
                })
                .orElseGet(() -> {
                    LoungeEntryEntity e = new LoungeEntryEntity();
                    e.setEventLounge(eventLounge);
                    e.setUser(user);
                    e.setJoinedAt(OffsetDateTime.now());

                    try {
                        LoungeEntryEntity saved = loungeEntryRepository.save(e);
                        log.info("loungeEntry.create.success domainId={} eventId={} eventLoungeId={} userId={} loungeEntryId={}",
                                domainId, eventId, eventLoungeId, userId, saved.getId());
                        return toApi(saved, req);
                    } catch (DataIntegrityViolationException dup) {
                        // Race-condition safe: unique constraint (event_lounge_id, user_id)
                        LoungeEntryEntity existing = loungeEntryRepository
                                .findByEventLounge_IdAndUser_Id(eventLoungeId, userId)
                                .orElseThrow(() -> dup);
                        log.info("loungeEntry.create.raceDuplicateResolved eventLoungeId={} userId={} loungeEntryId={}",
                                eventLoungeId, userId, existing.getId());
                        return toApi(existing, req);
                    }
                });
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
}
