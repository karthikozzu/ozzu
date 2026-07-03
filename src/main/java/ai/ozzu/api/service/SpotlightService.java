package ai.ozzu.api.service;

import ai.ozzu.api.generated.model.InningsScore;
import ai.ozzu.api.generated.model.Pagination;
import ai.ozzu.api.generated.model.ScoreSummary;
import ai.ozzu.api.generated.model.SpotlightEvent;
import ai.ozzu.api.generated.model.SpotlightResponse;
import ai.ozzu.api.generated.model.SpotlightTeam;
import ai.ozzu.api.generated.model.SpotlightUserWager;
import ai.ozzu.api.persistence.entity.EventEntity;
import ai.ozzu.api.persistence.entity.TeamEntity;
import ai.ozzu.api.persistence.entity.WagerEntity;
import ai.ozzu.api.persistence.repo.EventRepository;
import ai.ozzu.api.persistence.repo.WagerRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class SpotlightService {

    private final EventRepository eventRepo;
    private final WagerRepository wagerRepo;
    private final TokenLedgerService tokenLedgerService;

    public SpotlightService(
            EventRepository eventRepo,
            WagerRepository wagerRepo,
            TokenLedgerService tokenLedgerService
    ) {
        this.eventRepo = eventRepo;
        this.wagerRepo = wagerRepo;
        this.tokenLedgerService = tokenLedgerService;
    }

    public SpotlightResponse getSpotlight(UUID domainId, UUID userId, int limit, int page) {
        int safeLimit = limit <= 0 ? 10 : Math.min(limit, 50);
        int safePage = Math.max(page, 0);

        Pageable spotlightPage = PageRequest.of(0, safeLimit);
        Pageable otherPage = PageRequest.of(safePage, safeLimit);

        List<EventEntity> spotlightEvents =
                eventRepo.findSpotlightEvents(domainId, spotlightPage);

        List<EventEntity> otherEvents =
                eventRepo.findNonSpotlightEvents(domainId, otherPage);

        List<EventEntity> allEvents = new ArrayList<>();
        allEvents.addAll(spotlightEvents);
        allEvents.addAll(otherEvents);

        List<UUID> eventIds = allEvents.stream()
                .map(EventEntity::getId)
                .toList();

        Map<UUID, Long> userCounts = countUsersBulk(eventIds);
        Map<UUID, Long> potMap = sumPotBulk(eventIds);
        Map<UUID, WagerEntity> userWagers = findUserWagersBulk(userId, eventIds);

        long tokens = userId != null ? tokenLedgerService.balance(userId) : 0L;

        List<SpotlightEvent> events = allEvents.stream()
                .map(event -> mapEvent(event, userCounts, potMap, userWagers))
                .toList();

        Pagination pagination = new Pagination();
        pagination.setTotalItems(otherEvents.size());
        pagination.setNextPage(otherEvents.size() == safeLimit ? safePage + 1 : null);
        pagination.setPrevPage(safePage > 0 ? safePage - 1 : null);

        SpotlightResponse response = new SpotlightResponse();
        response.setTokens(tokens);
        response.setEvents(events);
        response.setPagination(pagination);

        return response;
    }

    private Map<UUID, Long> countUsersBulk(List<UUID> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return wagerRepo.countUsersBulkRows(eventIds)
                .stream()
                .collect(Collectors.toMap(
                        WagerRepository.EventUserCountRow::getEventId,
                        row -> row.getUsersCount() != null ? row.getUsersCount() : 0L
                ));
    }

    private Map<UUID, Long> sumPotBulk(List<UUID> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return wagerRepo.sumPotBulkRows(eventIds)
                .stream()
                .collect(Collectors.toMap(
                        WagerRepository.EventPotRow::getEventId,
                        row -> row.getPotAmount() != null ? row.getPotAmount() : 0
                ));
    }

    public Map<UUID, WagerEntity> findUserWagersBulk(UUID userId, List<UUID> eventIds) {
        if (userId == null || eventIds == null || eventIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<WagerEntity> wagers =
                wagerRepo.findByUserIdAndEventIdIn(userId, eventIds);

        return wagers.stream()
                .collect(Collectors.toMap(
                        WagerEntity::getEventId,
                        w -> w,
                        (w1, w2) -> w1.getCreatedAt().isAfter(w2.getCreatedAt()) ? w1 : w2
                ));
    }

    private SpotlightEvent mapEvent(
            EventEntity event,
            Map<UUID, Long> userCounts,
            Map<UUID, Long> potMap,
            Map<UUID, WagerEntity> userWagers
    ) {
        SpotlightEvent dto = new SpotlightEvent();

        dto.setId(event.getId());

        if (event.getDomain() != null) {
            dto.setDomainId(event.getDomain().getId());
        }

        if (event.getSeries() != null) {
            dto.setSeriesId(event.getSeries().getId());
            dto.setSeriesName(event.getSeries().getName());
        }

        dto.setName(event.getName());
        dto.setDescription(event.getDescription());
        dto.setEventImageUrl(event.getEventImageUrl());
        dto.setEventLocation(event.getLocation());
        dto.setVenue(event.getVenue());

        dto.setTimeCreated(event.getCreatedAt());
        dto.setTimeUpdated(event.getUpdatedAt());
        dto.setTimeEventStart(event.getTimeEventStart());
        dto.setTimeEventEnd(event.getTimeEventEnd());

        dto.setIsSpotlight(event.isSpotlight());
        dto.setInternalProperties(event.getInternalProperties());

        Long usersCount = userCounts != null ? userCounts.get(event.getId()) : null;
        Long potAmount = potMap != null ? potMap.get(event.getId()) : null;

        dto.setUsersCount(usersCount != null ? usersCount : 0L);
        dto.setPotAmount(potAmount != null ? potAmount.intValue() : 0);

        dto.setTeams(mapTeams(event));
        dto.setScoreSummary(mapScoreSummary(event.getInternalProperties()));

        WagerEntity wager = userWagers != null ? userWagers.get(event.getId()) : null;

        if (wager != null) {
            SpotlightUserWager userWager = new SpotlightUserWager();
            userWager.setAmount(wager.getStakeTokens());
            userWager.setOutcome(wager.getOutcome() != null ? wager.getOutcome().name() : null);
            dto.setUserWager(userWager);
        }

        dto.setIsEventCanceled(isEventCanceled(event));
        dto.setIsEventCompleted(isEventCompleted(event));

        return dto;
    }

    private List<SpotlightTeam> mapTeams(EventEntity event) {
        List<SpotlightTeam> teams = new ArrayList<>();

        if (event.getTeamA() != null) {
            teams.add(mapTeam(event.getTeamA(), event, "teamA"));
        }

        if (event.getTeamB() != null) {
            teams.add(mapTeam(event.getTeamB(), event, "teamB"));
        }

        return teams;
    }

    private SpotlightTeam mapTeam(TeamEntity team, EventEntity event, String teamKey) {
        SpotlightTeam dto = new SpotlightTeam();

        dto.setId(team.getId());

        if (team.getDomain() != null) {
            dto.setDomainId(team.getDomain().getId());
        }

        if (team.getSeries() != null) {
            dto.setSeriesId(team.getSeries().getId());
        }

        dto.setName(team.getName());

        String imageUrl = null;

        if (team.getTeamPhotoUrl() != null) {
            imageUrl = team.getTeamPhotoUrl();
        } else if (team.getInternalProperties() != null) {
            Object logoUrl = team.getInternalProperties().get("logoUrl");
            imageUrl = logoUrl != null ? String.valueOf(logoUrl) : null;
        }

        dto.setTeamImageUrl(imageUrl);
        dto.setScore(getScoreDisplay(event.getInternalProperties(), teamKey));

        return dto;
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

    private boolean isEventCanceled(EventEntity event) {
        return event.getStatus() != null
                && "CANCELED".equalsIgnoreCase(String.valueOf(event.getStatus()));
    }

    private boolean isEventCompleted(EventEntity event) {
        return event.getStatus() != null
                && "COMPLETED".equalsIgnoreCase(String.valueOf(event.getStatus()));
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

        try {
            return Integer.valueOf(String.valueOf(value));
        } catch (Exception ex) {
            return null;
        }
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

        try {
            return Double.valueOf(String.valueOf(value));
        } catch (Exception ex) {
            return null;
        }
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