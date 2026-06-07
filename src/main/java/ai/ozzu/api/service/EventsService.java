package ai.ozzu.api.service;

import ai.ozzu.api.exceptions.BadRequestException;
import ai.ozzu.api.exceptions.EntityNotFoundException;
import ai.ozzu.api.generated.model.Event;
import ai.ozzu.api.generated.model.EventListResponse;
import ai.ozzu.api.generated.model.EventPageResponse;
import ai.ozzu.api.generated.model.EventTeam;
import ai.ozzu.api.generated.model.InningsScore;
import ai.ozzu.api.generated.model.LineupEntry;
import ai.ozzu.api.generated.model.ScoreSummary;
import ai.ozzu.api.persistence.entity.DomainEntity;
import ai.ozzu.api.persistence.entity.EventEntity;
import ai.ozzu.api.persistence.entity.EventParticipantEntity;
import ai.ozzu.api.persistence.entity.SeriesEntity;
import ai.ozzu.api.persistence.entity.TeamEntity;
import ai.ozzu.api.persistence.enums.EventStatus;
import ai.ozzu.api.persistence.enums.WagerStatus;
import ai.ozzu.api.persistence.models.EventCreateRequest;
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

import java.io.IOException;
import java.net.URI;
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

    private final MediaService mediaService;

    public EventsService(EventRepository eventRepository,
                         DomainRepository domainRepository,
                         SeriesRepository seriesRepository,
                         EventParticipantRepository eventParticipantRepository,
                         WagerRepository wagerRepository,
                         MediaService mediaService) {
        this.eventRepository = eventRepository;
        this.domainRepository = domainRepository;
        this.seriesRepository = seriesRepository;
        this.eventParticipantRepository = eventParticipantRepository;
        this.wagerRepository = wagerRepository;
        this.mediaService = mediaService;
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
        if(req.getImage() != null) {
            String url;
            try {
                url = mediaService.uploadImage(
                        req.getImage().getContentAsByteArray(),
                        e.getName()+".jpg",
                        "image/jpeg",
                        "EVENT_PHOTO",
                        e.getId()
                );
            } catch (IOException ex) {
                throw new BadRequestException(ex.getMessage());
            }
            e.setEventImageUrl(url);
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

        OffsetDateTime fromTs = (fromDate == null)
                ? null
                : fromDate.atStartOfDay().atOffset(ZoneOffset.UTC);

        OffsetDateTime toTs = (toDate == null)
                ? null
                : toDate.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);

        CursorParts cp = decodeCursor(cursor);

        var spec = EventSpecs.domainId(domainId);

        if (seriesId != null) {
            spec = spec.and(EventSpecs.seriesId(seriesId));
        }

        if (teamId != null) {
            spec = spec.and(EventSpecs.teamId(teamId));
        }

        if (status != null) {
            spec = spec.and(EventSpecs.status(status));
        }

        if (fromTs != null) {
            spec = spec.and(EventSpecs.startGte(fromTs));
        }

        if (toTs != null) {
            spec = spec.and(EventSpecs.startLt(toTs));
        }

        if (cp.cursorTime != null && cp.cursorId != null) {
            spec = spec.and(EventSpecs.afterCursor(cp.cursorTime, cp.cursorId));
        }

        var sort = org.springframework.data.domain.Sort
                .by(org.springframework.data.domain.Sort.Direction.ASC, "timeEventStart")
                .and(org.springframework.data.domain.Sort.by("id"));

        var pageable = PageRequest.of(0, pageSize + 1, sort);

        var page = eventRepository.findAll(spec, pageable);
        List<EventEntity> rows = page.getContent();

        boolean hasMore = rows.size() > pageSize;

        if (hasMore) {
            rows = rows.subList(0, pageSize);
        }

        List<Event> events = rows.stream()
                .map(this::toApi)
                .toList();

        String nextCursor = null;

        if (hasMore && !rows.isEmpty()) {
            EventEntity last = rows.get(rows.size() - 1);
            nextCursor = encodeCursor(last.getTimeEventStart(), last.getId());
        }

        EventListResponse resp = new EventListResponse();
        resp.setEvents(events);
        resp.setHasMore(hasMore);
        resp.setNextCursor(JsonNullable.of(nextCursor));

        log.info("listEvents: returned {} events, hasMore={}", events.size(), hasMore);

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

        fill(page, e);

        Map<String, Object> eventScores = extractEventScores(e);

        if (eventScores != null && !eventScores.isEmpty()) {
            page.setEventScores(eventScores);
        }

        List<EventParticipantEntity> participants =
                eventParticipantRepository.findByEvent_IdOrderByCreatedAtAsc(eventId);

        List<LineupEntry> lineup = participants.stream()
                .map(ep -> {
                    LineupEntry li = new LineupEntry();

                    if (ep.getPlayer() != null) {
                        li.setParticipantType(LineupEntry.ParticipantTypeEnum.PLAYER);
                        li.setParticipantId(ep.getPlayer().getId());
                    } else if (ep.getTeam() != null) {
                        li.setParticipantType(LineupEntry.ParticipantTypeEnum.TEAM);
                        li.setParticipantId(ep.getTeam().getId());
                    } else {
                        log.warn("getEventPage: participant {} has neither player nor team", ep.getId());
                        return null;
                    }

                    li.setRole(ep.getRole());
                    li.setInternalProperties(ep.getInternalProperties());

                    return li;
                })
                .filter(Objects::nonNull)
                .toList();

        page.setLineup(lineup);

        Map<String, Object> mySummary =
                wagerRepository.computeSummaryForEvent(eventId, WagerStatus.PLACED);

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
        api.setSeriesName(e.getSeries() != null ? e.getSeries().getName() : null);

        api.setName(e.getName());
        api.setDescription(e.getDescription());

        api.setEventImageUrl(e.getEventImageUrl() != null ? URI.create(e.getEventImageUrl()) : null);
        api.setEventLocation(e.getLocation());
        api.setVenue(e.getVenue());

        api.setTimeEventStart(e.getTimeEventStart());
        api.setTimeEventEnd(e.getTimeEventEnd());

        api.setIsEventCanceled(e.isCanceled());
        api.setIsEventCompleted(e.isCompleted());
        api.setIsSpotlight(e.isSpotlight());

        api.setObjectStatus(e.getStatus() != null ? e.getStatus().name() : null);

        api.setTimeCreated(e.getCreatedAt());
        api.setTimeUpdated(e.getUpdatedAt());
        api.setInternalProperties(e.getInternalProperties());

        api.setTeams(mapTeams(e));
        api.setScoreSummary(mapScoreSummary(e.getInternalProperties()));
    }

    private void fill(EventPageResponse api, EventEntity e) {
        api.setId(e.getId());
        api.setDomainId(e.getDomain() != null ? e.getDomain().getId() : null);

        api.setSeriesId(e.getSeries() != null ? e.getSeries().getId() : null);
        api.setSeriesName(e.getSeries() != null ? e.getSeries().getName() : null);

        api.setName(e.getName());
        api.setDescription(e.getDescription());

        api.setEventImageUrl(e.getEventImageUrl() != null ? URI.create(e.getEventImageUrl()) : null);
        api.setEventLocation(e.getLocation());
        api.setVenue(e.getVenue());

        api.setTimeEventStart(e.getTimeEventStart());
        api.setTimeEventEnd(e.getTimeEventEnd());
        api.setIsEventCanceled(e.isCanceled());
        api.setIsEventCompleted(e.isCompleted());
        api.setIsSpotlight(e.isSpotlight());

        api.setObjectStatus(e.getStatus() != null ? e.getStatus().name() : null);

        api.setTimeCreated(e.getCreatedAt());
        api.setTimeUpdated(e.getUpdatedAt());
        api.setInternalProperties(e.getInternalProperties());

        api.setTeams(mapTeams(e));
        api.setScoreSummary(mapScoreSummary(e.getInternalProperties()));
    }

    private List<EventTeam> mapTeams(EventEntity event) {

        List<EventTeam> teams = new ArrayList<>();

        if (event.getTeamA() != null) {
            teams.add(mapTeam(event.getTeamA(), event, "teamA"));
        }

        if (event.getTeamB() != null) {
            teams.add(mapTeam(event.getTeamB(), event, "teamB"));
        }

        return teams;
    }

    private EventTeam mapTeam(TeamEntity team, EventEntity event, String teamKey) {

        EventTeam dto = new EventTeam();

        dto.setId(team.getId());
        dto.setDomainId(team.getDomain() != null ? team.getDomain().getId() : null);
        dto.setSeriesId(team.getSeries() != null ? team.getSeries().getId() : null);
        dto.setName(team.getName());

        if (team.getTeamPhotoUrl() != null) {
            dto.setTeamImageUrl(URI.create(team.getTeamPhotoUrl()));
        } else if (team.getInternalProperties() != null) {
            Object logoUrl = team.getInternalProperties().get("logoUrl");
            dto.setTeamImageUrl(logoUrl != null ? URI.create(String.valueOf(logoUrl)) : null);
        }

        dto.setScore(getScoreDisplay(event.getInternalProperties(), teamKey));

        return dto;
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

    private ScoreSummary mapScoreSummary(Map<String, Object> props) {

        if (props == null) {
            return null;
        }

        Object scoreSummaryObj = props.get("scoreSummary");

        if (!(scoreSummaryObj instanceof Map<?, ?> scoreSummaryMap)) {
            return null;
        }

        Object inningsObj = scoreSummaryMap.get("innings");

        if (!(inningsObj instanceof List<?> inningsList)) {
            return null;
        }

        List<InningsScore> innings = new ArrayList<>();

        for (Object item : inningsList) {
            if (!(item instanceof Map<?, ?> inningMap)) {
                continue;
            }

            InningsScore score = new InningsScore();

            score.setTeam(asString(inningMap.get("team")));
            score.setInningNumber(asInteger(inningMap.get("inningNumber")));
            score.setRuns(asInteger(inningMap.get("runs")));
            score.setWickets(asInteger(inningMap.get("wickets")));
            score.setOvers(asDouble(inningMap.get("overs")));
            score.setRunRate(asDouble(inningMap.get("runRate")));
            score.setIsCompleted(asBoolean(inningMap.get("isCompleted")));

            innings.add(score);
        }

        ScoreSummary scoreSummary = new ScoreSummary();
        scoreSummary.setInnings(innings);

        return scoreSummary;
    }

    private String getScoreDisplay(Map<String, Object> props, String teamKey) {

        if (props == null) {
            return null;
        }

        Object scoresObj = props.get("scores");

        if (!(scoresObj instanceof Map<?, ?> scores)) {
            return null;
        }

        Object teamObj = scores.get(teamKey);

        if (!(teamObj instanceof Map<?, ?> team)) {
            return null;
        }

        Object display = team.get("display");

        return display != null ? String.valueOf(display) : null;
    }

    private Map<String, Object> extractEventScores(EventEntity e) {

        if (e == null || e.getInternalProperties() == null) {
            return null;
        }

        Object eventScoresObj = e.getInternalProperties().get("eventScores");

        if (eventScoresObj instanceof Map<?, ?> map) {
            Map<String, Object> result = new HashMap<>();

            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() != null) {
                    result.put(String.valueOf(entry.getKey()), entry.getValue());
                }
            }

            return result;
        }

        Object oldScorecardObj = e.getInternalProperties().get("scorecard");

        if (oldScorecardObj instanceof Map<?, ?> map) {
            Map<String, Object> result = new HashMap<>();

            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() != null) {
                    result.put(String.valueOf(entry.getKey()), entry.getValue());
                }
            }

            return result;
        }

        return null;
    }

    private String asString(Object value) {
        return value != null ? String.valueOf(value) : null;
    }

    private Integer asInteger(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Integer i) {
            return i;
        }

        if (value instanceof Number n) {
            return n.intValue();
        }

        return Integer.valueOf(String.valueOf(value));
    }

    private Double asDouble(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Double d) {
            return d;
        }

        if (value instanceof Number n) {
            return n.doubleValue();
        }

        return Double.valueOf(String.valueOf(value));
    }

    private Boolean asBoolean(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Boolean b) {
            return b;
        }

        return Boolean.valueOf(String.valueOf(value));
    }
}