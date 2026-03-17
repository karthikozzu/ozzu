package ai.ozzu.api.service;

import ai.ozzu.api.generated.model.Pagination;
import ai.ozzu.api.generated.model.SpotlightItem;
import ai.ozzu.api.generated.model.SpotlightItemType;
import ai.ozzu.api.generated.model.SpotlightResponse;
import ai.ozzu.api.persistence.entity.EventEntity;
import ai.ozzu.api.persistence.entity.TeamEntity;
import ai.ozzu.api.persistence.entity.WagerEntity;
import ai.ozzu.api.persistence.repo.EventRepository;
import ai.ozzu.api.persistence.repo.WagerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SpotlightService {

    @Autowired
    private EventRepository eventRepo;

    @Autowired
    private WagerRepository wagerRepo;

    @Autowired
    private TokenLedgerService tokenLedgerService;

    /**
     * Main API
     */
    public SpotlightResponse getSpotlight(UUID domainId, UUID userId, int limit, int page) {

        Pageable spotlightPage = PageRequest.of(0, limit);   // always first page
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

        SpotlightResponse response = new SpotlightResponse();

        // 🔹 1. Add token info
        long tokens = tokenLedgerService.balance(userId);

        response.addItemsItem(
                new SpotlightItem()
                        .responseType(SpotlightItemType.INFO_BLOCK)
                        .putContentItem("tokens", tokens)
        );

        // 🔹 2. Add events
        for (EventEntity event : allEvents) {

            boolean isSpotlight = event.isIs_spotlight();

            SpotlightItem item = mapEvent(
                    event,
                    isSpotlight,
                    userCounts,
                    potMap,
                    userWagers
            );

            response.addItemsItem(item);
        }

        Pagination pagination = new Pagination();
        pagination.setTotalItems(limit);
        pagination.setTotalItems(otherEvents.size());
        pagination.setNextPage(page + 1);
        pagination.setPrevPage(page > 0 ? page - 1 : null);
        response.setPagination(pagination);
        return response;
    }

    /**
     * Bulk fetch user wagers
     */
    public Map<UUID, WagerEntity> findUserWagersBulk(UUID userId, List<UUID> eventIds) {

        if (eventIds == null || eventIds.isEmpty()) return Map.of();

        List<WagerEntity> wagers =
                wagerRepo.findByUserIdAndEventIdIn(userId, eventIds);

        return wagers.stream()
                .collect(Collectors.toMap(
                        WagerEntity::getEventId,
                        w -> w,
                        (w1, w2) -> w1.getCreatedAt().isAfter(w2.getCreatedAt()) ? w1 : w2
                ));
    }

    /**
     * Map Event -> SpotlightItem
     */
    private SpotlightItem mapEvent(
            EventEntity event,
            boolean isSpotlight,
            Map<UUID, Long> userCounts,
            Map<UUID, Integer> potMap,
            Map<UUID, WagerEntity> userWagers) {

        Map<String, Object> content = new HashMap<>();

        content.put("eventId", event.getId());
        content.put("isSpotlight", isSpotlight);
        content.put("status", event.getStatus());

        content.put("series", event.getSeries() != null ? event.getSeries().getName() : null);
        content.put("venue", event.getVenue());
        content.put("location", event.getLocation());
        content.put("eventImageUrl", event.getEventImageUrl());

        content.put("teamA", mapTeam(event.getTeamA(), event, "teamA"));
        content.put("teamB", mapTeam(event.getTeamB(), event, "teamB"));

        content.put("usersCount", userCounts.getOrDefault(event.getId(), 0L));
        content.put("potAmount", potMap.getOrDefault(event.getId(), 0));

        WagerEntity wager = userWagers.get(event.getId());
        if (wager != null) {
            content.put("userWager", Map.of(
                    "amount", wager.getStakeTokens(),
                    "outcome", wager.getOutcome()
            ));
        }

        return new SpotlightItem()
                .id(event.getId().toString())
                .responseType(SpotlightItemType.SPOTLIGHT_GAME)
                .content(content)
                .timeBegin(event.getTimeEventStart())
                .timeEnd(event.getTimeEventEnd());
    }

    /**
     * Map Team
     */
    private Map<String, Object> mapTeam(TeamEntity team, EventEntity event, String key) {

        if (team == null) return Map.of();

        Map<String, Object> props = event.getInternalProperties();
        String score = getScoreDisplay(props, key);

        return Map.of(
                "id", team.getId(),
                "name", team.getName(),
                "logoUrl", team.getInternalProperties().get("logoUrl"),
                "score", score
        );
    }

    /**
     * Extract score from JSON
     */
    private String getScoreDisplay(Map<String, Object> props, String teamKey) {

        if (props == null) return null;

        Object scoresObj = props.get("scores");
        if (!(scoresObj instanceof Map)) return null;

        Map<String, Object> scores = (Map<String, Object>) scoresObj;

        Object teamObj = scores.get(teamKey);
        if (!(teamObj instanceof Map)) return null;

        Map<String, Object> team = (Map<String, Object>) teamObj;

        return (String) team.get("display");
    }
}