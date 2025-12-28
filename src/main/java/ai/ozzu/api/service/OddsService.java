package ai.ozzu.api.service;

import ai.ozzu.api.generated.model.OddsBindingQuote;
import ai.ozzu.api.generated.model.OddsPickQuote;
import ai.ozzu.api.generated.model.OddsQuoteResponse;
import ai.ozzu.api.persistence.enums.EventStatus;
import ai.ozzu.api.persistence.repo.EventRepository;
import ai.ozzu.api.persistence.repo.ScopedReferentRepository;
import ai.ozzu.api.persistence.repo.WagerCardTypeBindingRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * OddsService
 *
 * Core responsibility:
 * --------------------
 * Computes *quotable odds* for a given:
 *  - Domain
 *  - Event
 *  - WagerCardType
 *
 * Output:
 * -------
 * A fully materialized OddsQuoteResponse that contains:
 *  - Whether the event is locked for betting
 *  - Odds grouped by wager card type bindings
 *  - Picks under each binding with decimal odds + implied probability
 *
 * Design principles:
 * ------------------
 * - Stateless
 * - Deterministic (same inputs → same outputs)
 * - Pricing logic driven via internalProperties (future-proof)
 * - No DB mutation (read-only quoting service)
 */
@Service
public class OddsService {

    private final EventRepository eventRepo;
    private final WagerCardTypeBindingRepository bindingRepo;
    private final ScopedReferentRepository scopedRepo;

    public OddsService(EventRepository eventRepo,
                       WagerCardTypeBindingRepository bindingRepo,
                       ScopedReferentRepository scopedRepo) {
        this.eventRepo = eventRepo;
        this.bindingRepo = bindingRepo;
        this.scopedRepo = scopedRepo;
    }

    /**
     * Generates odds quote for a given event and wager card type.
     *
     * High-level flow:
     * ----------------
     * 1. Validate event belongs to domain
     * 2. Determine if event is locked (LIVE / COMPLETED / CANCELED)
     * 3. Fetch wager card type bindings
     * 4. For each binding:
     *    - Determine pricing model (currently fixed decimal)
     *    - Fetch scoped referents (picks)
     *    - Calculate implied probability
     * 5. Assemble OddsQuoteResponse
     *
     * @param domainId         Domain identifier
     * @param eventId          Event identifier
     * @param wagerCardTypeId  Wager card type being quoted
     * @return OddsQuoteResponse
     */
    public OddsQuoteResponse quote(UUID domainId, UUID eventId, UUID wagerCardTypeId) {

        // 1. Validate event + domain
        var event = eventRepo.findByIdAndDomainId(eventId, domainId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid event/domain"));

        // 2. Check if odds quoting should be locked
        boolean locked = isLockedEventStatus(event.getStatus());

        // 3. Fetch all bindings under this wager card type
        var bindings = bindingRepo.findByDomainIdAndWagerCardTypeId(domainId, wagerCardTypeId);

        // 4. Build binding-wise odds
        List<OddsBindingQuote> bindingQuotes = bindings.stream().map(b -> {

            // Determine pricing model from binding.internalProperties
            double fixedDecimalOdds =
                    extractFixedDecimalOddsOrDefault(b.getInternalProperties(), 2.0);

            Double decimalOdds = fixedDecimalOdds; // boxed once

            // Fetch all picks (scoped referents) under this concept term
            List<OddsPickQuote> picks = scopedRepo
                    .findByEventIdAndConceptTermId(eventId, b.getConceptTermId())
                    .stream()
                    .map(sr -> {

                        // Create pick with required constructor
                        OddsPickQuote pick =
                                new OddsPickQuote(sr.getId(), sr.getName(), decimalOdds);

                        // Implied probability = 1 / decimalOdds
                        pick.impliedProb(round4(1.0 / fixedDecimalOdds));

                        // Optional future metadata
                        // pick.putInternalPropertiesItem("pricingMode", "fixed_decimal");

                        return pick;
                    })
                    .toList();

            // Group picks under a binding
            return new OddsBindingQuote(
                    b.getId(),
                    b.getConceptTermId(),
                    picks
            );
        }).toList();

        // 5. Assemble response
        OddsQuoteResponse response =
                new OddsQuoteResponse(domainId, eventId, wagerCardTypeId, bindingQuotes);

        response.locked(locked);

        return response;
    }

    /**
     * Determines whether odds quoting should be disabled for an event.
     *
     * Locking rules:
     * --------------
     * - LIVE       → locked (odds frozen)
     * - COMPLETED  → locked
     * - CANCELED   → locked
     *
     * @param status Event status
     * @return true if locked
     */
    private boolean isLockedEventStatus(EventStatus status) {
        return status != null && (
                "LIVE".equals(status.name()) ||
                        "COMPLETED".equals(status.name()) ||
                        "CANCELED".equals(status.name())
        );
    }

    /**
     * Extracts fixed decimal odds from internalProperties.
     *
     * Expected structure:
     * -------------------
     * {
     *   "pricing": {
     *     "mode": "fixed_decimal",
     *     "decimalOdds": 2.15
     *   }
     * }
     *
     * Why internalProperties?
     * -----------------------
     * - Avoids schema changes
     * - Allows multiple pricing strategies later:
     *   - fixed_decimal
     *   - dynamic
     *   - market_maker
     *   - AI-driven pricing
     *
     * @param internalProps Raw JSON properties
     * @param def           Default odds if parsing fails
     * @return decimal odds
     */
    private double extractFixedDecimalOddsOrDefault(
            Map<String, Object> internalProps,
            double def
    ) {
        if (internalProps == null) return def;

        Object pricingObj = internalProps.get("pricing");
        if (!(pricingObj instanceof Map<?, ?> pricing)) return def;

        Object modeObj = pricing.get("mode");
        String mode = modeObj == null ? null : modeObj.toString();
        if (!"fixed_decimal".equalsIgnoreCase(mode)) return def;

        Object oddsObj = pricing.get("decimalOdds");
        if (oddsObj == null) return def;

        if (oddsObj instanceof Number n) {
            return n.doubleValue();
        }

        try {
            return Double.parseDouble(oddsObj.toString());
        } catch (Exception e) {
            return def;
        }
    }

    private static Double round4(double v) {
        return Math.round(v * 10000.0) / 10000.0;
    }
}