package ai.ozzu.api.service;

import ai.ozzu.api.persistence.entity.EventScoreEntity;
import ai.ozzu.api.persistence.entity.WagerCardBindingEntity;
import ai.ozzu.api.persistence.entity.WagerCardEntity;
import ai.ozzu.api.persistence.entity.WagerEntity;
import ai.ozzu.api.persistence.enums.WagerOutcome;
import ai.ozzu.api.persistence.enums.WagerStateEventEntity;
import ai.ozzu.api.persistence.enums.WagerStatus;
import ai.ozzu.api.persistence.repo.EventScoreRepository;
import ai.ozzu.api.persistence.repo.WagerCardBindingRepository;
import ai.ozzu.api.persistence.repo.WagerCardRepository;
import ai.ozzu.api.persistence.repo.WagerRepository;
import ai.ozzu.api.persistence.repo.WagerStateEventRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class WagerSettlementService {

    private final WagerRepository wagerRepo;
    private final WagerCardRepository wagerCardRepo;
    private final WagerCardBindingRepository wagerCardBindingRepo;
    private final WagerStateEventRepository wagerStateEventRepo;
    private final EventScoreRepository eventScoreRepo;
    private final ObjectMapper objectMapper;
    private final TokenLedgerService tokenLedgerService;

    public WagerSettlementService(
            WagerRepository wagerRepo,
            WagerCardRepository wagerCardRepo,
            WagerCardBindingRepository wagerCardBindingRepo,
            WagerStateEventRepository wagerStateEventRepo,
            EventScoreRepository eventScoreRepo,
            ObjectMapper objectMapper,
            TokenLedgerService tokenLedgerService
    ) {
        this.wagerRepo = wagerRepo;
        this.wagerCardRepo = wagerCardRepo;
        this.wagerCardBindingRepo = wagerCardBindingRepo;
        this.wagerStateEventRepo = wagerStateEventRepo;
        this.eventScoreRepo = eventScoreRepo;
        this.objectMapper = objectMapper;
        this.tokenLedgerService = tokenLedgerService;
    }

    /**
     * Main settlement method.
     *
     * Called from EventStatusService when event status becomes COMPLETED.
     */
    @Transactional
    public int settleEvent(UUID eventId, UUID actorUserId) {
        EventScoreEntity finalScore = eventScoreRepo
                .findTopByEvent_IdOrderByCreatedAtDesc(eventId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Final score not found for eventId " + eventId
                ));

        JsonNode scoreJson = objectMapper.valueToTree(finalScore.getScoreJson());

        List<WagerEntity> lockedWagers =
                wagerRepo.lockByEventIdAndStatus(eventId, WagerStatus.LOCKED);

        int settledCount = 0;

        for (WagerEntity wager : lockedWagers) {
            SettlementDecision decision = evaluateWager(wager, scoreJson);

            boolean settled = settleSingleWager(wager, decision, actorUserId);

            if (settled) {
                settledCount++;
            }
        }

        return settledCount;
    }

    private SettlementDecision evaluateWager(
            WagerEntity wager,
            JsonNode scoreJson
    ) {
        List<WagerCardBindingEntity> bindings =
                wagerCardBindingRepo.findByWagerEventIdAndWagerId(
                        wager.getEventId(),
                        wager.getId()
                );

        if (bindings.isEmpty()) {
            return SettlementDecision.voided("No wager bindings found");
        }

        for (WagerCardBindingEntity binding : bindings) {
            BindingDecision bindingDecision = evaluateBinding(binding, scoreJson);

            if (bindingDecision == BindingDecision.VOID) {
                return SettlementDecision.voided("One or more picks could not be evaluated");
            }

            if (bindingDecision == BindingDecision.LOSE) {
                return SettlementDecision.lost("One or more picks lost");
            }
        }

        return SettlementDecision.won("All picks won");
    }

    private BindingDecision evaluateBinding(
            WagerCardBindingEntity binding,
            JsonNode scoreJson
    ) {
        String conceptName = conceptName(binding);

        if (conceptName == null || conceptName.isBlank()) {
            return BindingDecision.VOID;
        }

        return switch (normalize(conceptName)) {
            case "runs" -> evaluatePlayerRunsRange(binding, scoreJson);
            case "wickets" -> evaluatePlayerWicketsRange(binding, scoreJson);
            case "typeofdismissal" -> evaluatePlayerDismissalType(binding, scoreJson);
            case "team" -> evaluateTeamResult(binding, scoreJson);
            case "batsmen", "batsman", "bowler", "allrounder" -> evaluatePlayerRole(binding, scoreJson);
            default -> BindingDecision.VOID;
        };
    }

    private String conceptName(WagerCardBindingEntity binding) {
        if (binding == null) {
            return null;
        }

        if (binding.getWagerCardTypeBinding() == null) {
            return null;
        }

        if (binding.getWagerCardTypeBinding().getConceptTerm() == null) {
            return null;
        }

        return binding.getWagerCardTypeBinding().getConceptTerm().getName();
    }

    private BindingDecision evaluatePlayerRunsRange(
            WagerCardBindingEntity binding,
            JsonNode scoreJson
    ) {
        if (binding.getPlayer() == null) {
            return BindingDecision.VOID;
        }

        String playerId = binding.getPlayer().getId().toString();
        JsonNode runsNode = scoreJson.at("/players/" + playerId + "/runs");

        if (runsNode.isMissingNode() || runsNode.isNull() || !runsNode.canConvertToInt()) {
            return BindingDecision.VOID;
        }

        String selectedValue = selectedValue(binding);

        if (selectedValue == null || selectedValue.isBlank()) {
            return BindingDecision.VOID;
        }

        return numberMatchesRange(runsNode.asInt(), selectedValue)
                ? BindingDecision.WIN
                : BindingDecision.LOSE;
    }

    private BindingDecision evaluatePlayerWicketsRange(
            WagerCardBindingEntity binding,
            JsonNode scoreJson
    ) {
        if (binding.getPlayer() == null) {
            return BindingDecision.VOID;
        }

        String playerId = binding.getPlayer().getId().toString();
        JsonNode wicketsNode = scoreJson.at("/players/" + playerId + "/wickets");

        if (wicketsNode.isMissingNode() || wicketsNode.isNull() || !wicketsNode.canConvertToInt()) {
            return BindingDecision.VOID;
        }

        String selectedValue = selectedValue(binding);

        if (selectedValue == null || selectedValue.isBlank()) {
            return BindingDecision.VOID;
        }

        return numberMatchesRange(wicketsNode.asInt(), selectedValue)
                ? BindingDecision.WIN
                : BindingDecision.LOSE;
    }

    private BindingDecision evaluatePlayerDismissalType(
            WagerCardBindingEntity binding,
            JsonNode scoreJson
    ) {
        if (binding.getPlayer() == null) {
            return BindingDecision.VOID;
        }

        String playerId = binding.getPlayer().getId().toString();

        String actualDismissalType =
                textAt(scoreJson, "/players/" + playerId + "/dismissalType");

        String selectedValue = selectedValue(binding);

        if (actualDismissalType == null || selectedValue == null) {
            return BindingDecision.VOID;
        }

        return normalize(actualDismissalType).equals(normalize(selectedValue))
                ? BindingDecision.WIN
                : BindingDecision.LOSE;
    }

    private BindingDecision evaluatePlayerRole(
            WagerCardBindingEntity binding,
            JsonNode scoreJson
    ) {
        if (binding.getPlayer() == null) {
            return BindingDecision.VOID;
        }

        String playerId = binding.getPlayer().getId().toString();

        String actualRole = textAt(scoreJson, "/players/" + playerId + "/role");
        String expectedRole = conceptName(binding);

        if (actualRole == null || expectedRole == null) {
            return BindingDecision.VOID;
        }

        return normalize(actualRole).equals(normalize(expectedRole))
                ? BindingDecision.WIN
                : BindingDecision.LOSE;
    }

    private BindingDecision evaluateTeamResult(
            WagerCardBindingEntity binding,
            JsonNode scoreJson
    ) {
        String winningTeamId = textAt(scoreJson, "/result/winningTeamId");

        if (winningTeamId == null || winningTeamId.isBlank()) {
            return BindingDecision.VOID;
        }

        if (binding.getTeam() == null) {
            return BindingDecision.VOID;
        }

        return binding.getTeam().getId().toString().equals(winningTeamId)
                ? BindingDecision.WIN
                : BindingDecision.LOSE;
    }

    private boolean settleSingleWager(
            WagerEntity wager,
            SettlementDecision decision,
            UUID actorUserId
    ) {
        WagerStatus oldStatus = wager.getStatus();

        if (!WagerStatus.canTransition(oldStatus, WagerStatus.SETTLED)) {
            return false;
        }

        int payoutTokens = calculatePayoutTokens(wager, decision);

        wager.setStatus(WagerStatus.SETTLED);
        wager.setOutcome(decision.outcome());
        wager.setPayoutTokens(payoutTokens);
        wager.setUpdatedAt(OffsetDateTime.now());

        updateCardStatuses(wager, decision);

        wagerStateEventRepo.save(
                WagerStateEventEntity.of(
                        wager,
                        oldStatus,
                        WagerStatus.SETTLED,
                        actorUserId,
                        "event_completed_settlement",
                        Map.of(
                                "eventId", wager.getEventId().toString(),
                                "outcome", decision.outcome().name(),
                                "reason", decision.reason(),
                                "payoutTokens", payoutTokens,
                                "source", "wager_settlement_service"
                        )
                )
        );

        if (decision.outcome() == WagerOutcome.WON && payoutTokens > 0) {
            tokenLedgerService.creditPayoutOnce(
                    wager.getUserId(),
                    wager.getDomainId(),
                    wager.getEventId(),
                    wager.getId(),
                    payoutTokens,
                    "wager_payout_" + wager.getEventId() + "_" + wager.getId()
            );
        }

        if (decision.outcome() == WagerOutcome.VOID && payoutTokens > 0) {
            tokenLedgerService.creditRefundOnce(
                    wager.getUserId(),
                    wager.getDomainId(),
                    wager.getEventId(),
                    wager.getId(),
                    payoutTokens,
                    "wager_refund_" + wager.getEventId() + "_" + wager.getId()
            );
        }

        return true;
    }

    private void updateCardStatuses(
            WagerEntity wager,
            SettlementDecision decision
    ) {
        List<WagerCardEntity> cards = wagerCardRepo.findByWager_Id(wager.getId());

        String cardStatus;

        if (decision.outcome() == WagerOutcome.WON) {
            cardStatus = "Correct";
        } else if (decision.outcome() == WagerOutcome.LOST) {
            cardStatus = "Incorrect";
        } else {
            cardStatus = "In Play";
        }

        for (WagerCardEntity card : cards) {
            card.setStatus(cardStatus);
            card.setUpdatedAt(OffsetDateTime.now());
        }

        wagerCardRepo.saveAll(cards);
    }

    private int calculatePayoutTokens(
            WagerEntity wager,
            SettlementDecision decision
    ) {
        if (decision.outcome() == WagerOutcome.LOST) {
            return 0;
        }

        if (decision.outcome() == WagerOutcome.VOID) {
            return wager.getStakeTokens();
        }

        if (decision.outcome() == WagerOutcome.WON) {
            if (wager.getStakeTokens() <= 0) {
                return 0;
            }

            BigDecimal decimalOdds = resolveBestLockedOdds(wager);

            BigDecimal payout = BigDecimal.valueOf(wager.getStakeTokens())
                    .multiply(decimalOdds);

            return payout.setScale(0, RoundingMode.DOWN).intValue();
        }

        return 0;
    }

    private BigDecimal resolveBestLockedOdds(WagerEntity wager) {
        List<WagerCardBindingEntity> bindings =
                wagerCardBindingRepo.findByWagerEventIdAndWagerId(
                        wager.getEventId(),
                        wager.getId()
                );

        BigDecimal bestOdds = null;

        for (WagerCardBindingEntity binding : bindings) {
            if (binding.getLockedDecimalOdds() == null) {
                continue;
            }

            if (bestOdds == null || binding.getLockedDecimalOdds().compareTo(bestOdds) > 0) {
                bestOdds = binding.getLockedDecimalOdds();
            }
        }

        if (bestOdds != null && bestOdds.compareTo(BigDecimal.ZERO) > 0) {
            return bestOdds;
        }

        return BigDecimal.valueOf(2.0);
    }

    /**
     * Priority:
     * 1. value column
     * 2. binding_value_id -> concept_terms.name
     * 3. pickPayload.selectedValue
     * 4. pickPayload.value
     */
    private String selectedValue(WagerCardBindingEntity binding) {
        if (binding.getValue() != null && !binding.getValue().isBlank()) {
            return binding.getValue();
        }

        if (binding.getBindingValue() != null
                && binding.getBindingValue().getName() != null
                && !binding.getBindingValue().getName().isBlank()) {
            return binding.getBindingValue().getName();
        }

        Map<String, Object> payload = binding.getPickPayload();

        if (payload == null) {
            return null;
        }

        Object selectedValue = payload.get("selectedValue");

        if (selectedValue != null && !selectedValue.toString().isBlank()) {
            return selectedValue.toString();
        }

        Object value = payload.get("value");

        if (value != null && !value.toString().isBlank()) {
            return value.toString();
        }

        return null;
    }

    private boolean numberMatchesRange(int actual, String selectedValue) {
        if (selectedValue == null || selectedValue.isBlank()) {
            return false;
        }

        String value = selectedValue.trim();

        try {
            if (value.contains("-")) {
                String[] parts = value.split("-");

                if (parts.length != 2) {
                    return false;
                }

                int min = Integer.parseInt(parts[0].trim());
                int max = Integer.parseInt(parts[1].trim());

                return actual >= min && actual <= max;
            }

            int exact = Integer.parseInt(value);

            return actual == exact;
        } catch (Exception ex) {
            return false;
        }
    }

    private String textAt(JsonNode node, String pointer) {
        JsonNode value = node.at(pointer);

        if (value == null || value.isMissingNode() || value.isNull()) {
            return null;
        }

        return value.asText();
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }

        return value
                .trim()
                .toLowerCase()
                .replace("_", "")
                .replace("-", "")
                .replace(" ", "");
    }

    private enum BindingDecision {
        WIN,
        LOSE,
        VOID
    }

    private record SettlementDecision(
            WagerOutcome outcome,
            String reason
    ) {
        static SettlementDecision won(String reason) {
            return new SettlementDecision(WagerOutcome.WON, reason);
        }

        static SettlementDecision lost(String reason) {
            return new SettlementDecision(WagerOutcome.LOST, reason);
        }

        static SettlementDecision voided(String reason) {
            return new SettlementDecision(WagerOutcome.VOID, reason);
        }
    }
}