package ai.ozzu.api.service;

import ai.ozzu.api.exceptions.EntityNotFoundException;
import ai.ozzu.api.generated.model.Event;
import ai.ozzu.api.generated.model.EventCreateRequest;
import ai.ozzu.api.generated.model.EventListResponse;
import ai.ozzu.api.generated.model.EventPageResponse;
import ai.ozzu.api.generated.model.LineupEntry;
import ai.ozzu.api.persistence.entity.DomainEntity;
import ai.ozzu.api.persistence.entity.EventEntity;
import ai.ozzu.api.persistence.entity.EventParticipantEntity;
import ai.ozzu.api.persistence.entity.SeriesEntity;
import ai.ozzu.api.persistence.enums.EventStatus;
import ai.ozzu.api.persistence.repo.DomainRepository;
import ai.ozzu.api.persistence.repo.EventParticipantRepository;
import ai.ozzu.api.persistence.repo.EventRepository;
import ai.ozzu.api.persistence.repo.SeriesRepository;
import ai.ozzu.api.persistence.repo.WagerRepository;
import org.openapitools.jackson.nullable.JsonNullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

@Service
public class EventsService {

    private static final Logger log = LoggerFactory.getLogger(EventsService.class);

    private final EventRepository eventRepository;
    private final DomainRepository domainRepository;
    private final SeriesRepository seriesRepository;
    private final EventParticipantRepository eventParticipantRepository;
    private final WagerRepository wagerRepository;

    public EventsService(EventRepository eventRepository,
                         DomainRepository domainRepository,
                         SeriesRepository seriesRepository,
                         EventParticipantRepository eventParticipantRepository,
                         WagerRepository wagerRepository) {
        this.eventRepository = eventRepository;
        this.domainRepository = domainRepository;
        this.seriesRepository = seriesRepository;
        this.eventParticipantRepository = eventParticipantRepository;
        this.wagerRepository = wagerRepository;
    }

    @Transactional
    public Event createEvent(UUID domainId, EventCreateRequest req) {
        log.info("createEvent: domainId={}, name={}", domainId, req != null ? req.getName() : null);

        DomainEntity domain = domainRepository.findById(domainId)
                .orElseThrow(() -> {
                    log.error("createEvent: Domain not found: {}", domainId);
                    return new EntityNotFoundException("Domain not found: " + domainId);
                });

        if (req == null || req.getName() == null || req.getName().isBlank()) {
            log.warn("createEvent: missing event name for domain {}", domainId);
            throw new IllegalArgumentException("Event name is required");
        }

        SeriesEntity series = null;
        if (req.getSeriesId() != null) {
            series = seriesRepository.findByIdAndDomain_Id(req.getSeriesId(), domainId)
                    .orElseThrow(() -> {
                        log.error("createEvent: Series not found in domain: {} {}", domainId, req.getSeriesId());
                        return new EntityNotFoundException("Series not found in domain: " + req.getSeriesId());
                    });
        }

        EventEntity e = new EventEntity();
        e.setDomain(domain);
        e.setSeries(series);
        e.setName(req.getName().trim());
        e.setDescription(req.getDescription());
        e.setTimeEventStart(req.getTimeEventStart());
        e.setTimeEventEnd(req.getTimeEventEnd());
        e.setInternalProperties(req.getInternalProperties() != null ? req.getInternalProperties() : Map.of());

        if (req.getInternalProperties() != null && req.getInternalProperties().containsKey("status")) {
            Object rawStatus = req.getInternalProperties().get("status");
            if (rawStatus instanceof String statusStr && !statusStr.isBlank()) {
                try {
                    EventStatus st = EventStatus.valueOf(statusStr.toUpperCase(Locale.ROOT));
                    e.setStatus(st);
                    log.debug("createEvent: set custom status={}", st);
                } catch (IllegalArgumentException ex) {
                    log.error("createEvent: Invalid status provided: {}", rawStatus);
                    throw new IllegalArgumentException("Invalid event status: " + statusStr);
                }
            }
        }

        EventEntity saved = eventRepository.save(e);
        log.info("createEvent: created eventId={} name={}", saved.getId(), saved.getName());
        return toApi(saved);
    }

    @Transactional(readOnly = true)
    public EventListResponse listEvents(UUID domainId,
                                        UUID seriesId,
                                        UUID teamId,
                                        LocalDate fromDate,
                                        LocalDate toDate,
                                        String statusStr,
                                        Integer limit,
                                        String cursor) {
        log.info("listEvents: domainId={}, seriesId={}, teamId={}, fromDate={}, toDate={}, status={}, limit={}, cursor={}",
                domainId, seriesId, teamId, fromDate, toDate, statusStr, limit, cursor);

        if (!domainRepository.existsById(domainId)) {
            log.error("listEvents: Domain not found: {}", domainId);
            throw new EntityNotFoundException("Domain not found: " + domainId);
        }

        int pageSize = (limit == null) ? 20 : Math.max(1, Math.min(100, limit));

        EventStatus status = null;
        if (statusStr != null && !statusStr.isBlank()) {
            try {
                status = EventStatus.valueOf(statusStr.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                log.warn("listEvents: Invalid status filter ignored: {}", statusStr);
            }
        }

        OffsetDateTime fromTs = (fromDate == null) ? null : fromDate.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime toTs = (toDate == null) ? null : toDate.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);

        CursorParts cp = decodeCursor(cursor);

        List<EventEntity> rows = eventRepository.searchSchedule(
                domainId,
                seriesId,
                status,
                fromTs,
                toTs,
                cp.cursorTime,
                cp.cursorId,
                PageRequest.of(0, pageSize + 1)
        );

        boolean hasMore = rows.size() > pageSize;
        if (hasMore) rows = rows.subList(0, pageSize);

        List<Event> items = rows.stream().map(this::toApi).toList();
        String nextCursor = null;
        if (hasMore && !rows.isEmpty()) {
            EventEntity last = rows.get(rows.size() - 1);
            nextCursor = encodeCursor(last.getTimeEventStart(), last.getId());
        }

        EventListResponse resp = new EventListResponse();
        resp.setItems(items);
        resp.setHasMore(hasMore);
        resp.setNextCursor(JsonNullable.of(nextCursor));

        log.info("listEvents: returned {} items, hasMore={}", items.size(), hasMore);
        return resp;
    }

    @Transactional(readOnly = true)
    public EventPageResponse getEventPage(UUID domainId, UUID eventId) {
        log.info("getEventPage: domainId={}, eventId={}", domainId, eventId);

        EventEntity e = eventRepository.findByIdAndDomain_Id(eventId, domainId)
                .orElseThrow(() -> {
                    log.error("getEventPage: Event not found in domain: {} {}", domainId, eventId);
                    return new EntityNotFoundException("Event not found in domain: " + eventId);
                });

        EventPageResponse page = new EventPageResponse();
        copyEventFields(page, e);

        Map<String,Object> scorecardObj = (Map<String,Object>) e.getInternalProperties().get("scorecard");
        if (scorecardObj != null) {
            page.setScorecard(scorecardObj);
        } else {
            page.setScorecard(new HashMap<>());
        }

        List<EventParticipantEntity> participants =
                eventParticipantRepository.findByEvent_IdOrderByCreatedAtAsc(eventId);

        List<ai.ozzu.api.generated.model.LineupEntry> lineup = participants.stream()
                .map(ep -> {
                    ai.ozzu.api.generated.model.LineupEntry li = new ai.ozzu.api.generated.model.LineupEntry();
                    LineupEntry.ParticipantTypeEnum type = (ep.getPlayer() != null)
                            ? LineupEntry.ParticipantTypeEnum.PLAYER
                            : LineupEntry.ParticipantTypeEnum.TEAM;
                    li.setParticipantType(type);
                    UUID pid = (ep.getPlayer() != null)
                            ? ep.getPlayer().getId()
                            : ep.getTeam().getId();
                    li.setParticipantId(pid);
                    li.setRole(ep.getRole());
                    li.setInternalProperties(ep.getInternalProperties());
                    return li;
                })
                .toList();

        page.setLineup(lineup);

        Map<String, Object> mySummary = wagerRepository.computeSummaryForEvent(eventId);
        if (mySummary != null) {
            page.setMyWagerSummary(mySummary);
        }

        log.info("getEventPage: built page for eventId={} with {} lineup entries",
                eventId, lineup.size());
        return page;
    }

    private Event toApi(EventEntity e) {
        Event api = new Event();
        copyEventFields(api, e);
        return api;
    }

    private void copyEventFields(Object apiObj, EventEntity e) {
        if (apiObj instanceof Event api) {
            fill(api, e);
        } else if (apiObj instanceof EventPageResponse page) {
            fill(page, e);
        } else {
            throw new IllegalArgumentException("Unsupported api object: " + apiObj.getClass());
        }
    }

    private void fill(Event api, EventEntity e) {
        api.setId(e.getId());
        api.setDomainId(e.getDomain() != null ? e.getDomain().getId() : null);
        api.setSeriesId(e.getSeries() != null ? e.getSeries().getId() : null);
        api.setName(e.getName());
        api.setDescription(e.getDescription());
        api.setTimeEventStart(e.getTimeEventStart());
        api.setTimeEventEnd(e.getTimeEventEnd());
        api.setIsEventCanceled(e.isCanceled());
        api.setIsEventCompleted(e.isCompleted());
        api.setObjectStatus(e.getStatus() != null ? e.getStatus().name() : null);
        api.setTimeCreated(e.getCreatedAt());
        api.setTimeUpdated(e.getUpdatedAt());
        api.setInternalProperties(e.getInternalProperties());
    }

    private void fill(EventPageResponse api, EventEntity e) {
        api.setId(e.getId());
        api.setDomainId(e.getDomain() != null ? e.getDomain().getId() : null);
        api.setSeriesId(e.getSeries() != null ? e.getSeries().getId() : null);
        api.setName(e.getName());
        api.setDescription(e.getDescription());
        api.setTimeEventStart(e.getTimeEventStart());
        api.setTimeEventEnd(e.getTimeEventEnd());
        api.setIsEventCanceled(e.isCanceled());
        api.setIsEventCompleted(e.isCompleted());
        api.setObjectStatus(e.getStatus() != null ? e.getStatus().name() : null);
        api.setTimeCreated(e.getCreatedAt());
        api.setTimeUpdated(e.getUpdatedAt());
        api.setInternalProperties(e.getInternalProperties());
    }

    private record CursorParts(OffsetDateTime cursorTime, UUID cursorId) {}

    private String encodeCursor(OffsetDateTime time, UUID id) {
        if (time == null || id == null) return null;
        String raw = time.toString() + "|" + id;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private CursorParts decodeCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) return new CursorParts(null, null);
        try {
            String raw = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            String[] parts = raw.split("\\|");
            if (parts.length != 2) return new CursorParts(null, null);
            OffsetDateTime t = OffsetDateTime.parse(parts[0]);
            UUID id = UUID.fromString(parts[1]);
            return new CursorParts(t, id);
        } catch (Exception ex) {
            log.warn("decodeCursor: invalid cursor '{}', treating as empty", cursor);
            return new CursorParts(null, null);
        }
    }
}