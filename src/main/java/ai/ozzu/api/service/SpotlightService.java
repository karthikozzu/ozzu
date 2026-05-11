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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class SpotlightService {

    @Autowired
    private EventRepository eventRepo;

    @Autowired
    private WagerRepository wagerRepo;

    @Autowired
    private TokenLedgerService tokenLedgerService;

    public SpotlightResponse getSpotlight(UUID domainId, UUID userId, int limit, int page) {

        Pageable spotlightPage = PageRequest.of(0, limit);
        Pageable otherPage = PageRequest.of(page, limit);

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

        Map<UUID, Long> userCounts = wagerRepo.countUsersBulk(eventIds);
        Map<UUID, Integer> potMap = wagerRepo.sumPotBulk(eventIds);
        Map<UUID, WagerEntity> userWagers = findUserWagersBulk(userId, eventIds);

        long tokens = tokenLedgerService.balance(userId);

        List<SpotlightEvent> events = allEvents.stream()
                .map(event -> mapEvent(event, userCounts, potMap, userWagers))
                .toList();

        Pagination pagination = new Pagination();
        pagination.setTotalItems(otherEvents.size());
        pagination.setNextPage(otherEvents.size() == limit ? page + 1 : null);
        pagination.setPrevPage(page > 0 ? page - 1 : null);

        SpotlightResponse response = new SpotlightResponse();
        response.setTokens(tokens);
        response.setEvents(events);
        response.setPagination(pagination);

        return response;
    }

    public Map<UUID, WagerEntity> findUserWagersBulk(UUID userId, List<UUID> eventIds) {

        if (eventIds == null || eventIds.isEmpty()) {
            return Map.of();
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
            Map<UUID, Integer> potMap,
            Map<UUID, WagerEntity> userWagers
    ) {

        SpotlightEvent dto = new SpotlightEvent();

        dto.setId(event.getId());
        dto.setDomainId(event.getDomain().getId());

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

        dto.setUsersCount(userCounts.getOrDefault(event.getId(), 0L));
        dto.setPotAmount(potMap.getOrDefault(event.getId(), 0));

        dto.setTeams(mapTeams(event));
        dto.setScoreSummary(mapScoreSummary(event.getInternalProperties()));

        WagerEntity wager = userWagers.get(event.getId());
        if (wager != null) {
            SpotlightUserWager userWager = new SpotlightUserWager();
            userWager.setAmount(wager.getStakeTokens());
            userWager.setOutcome(String.valueOf(wager.getOutcome()));
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
        dto.setDomainId(team.getDomain().getId());

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