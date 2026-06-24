package ai.ozzu.api.service;

import ai.ozzu.api.exceptions.EntityAlreadyExistsException;
import ai.ozzu.api.exceptions.EntityNotFoundException;
import ai.ozzu.api.exceptions.MissingFieldException;
import ai.ozzu.api.generated.model.Domain;
import ai.ozzu.api.generated.model.Lounge;
import ai.ozzu.api.generated.model.LoungeCreateRequest;
import ai.ozzu.api.persistence.entity.DomainEntity;
import ai.ozzu.api.persistence.entity.LoungeEntity;
import ai.ozzu.api.persistence.entity.LoungeMembershipEntity;
import ai.ozzu.api.persistence.entity.UserEntity;
import ai.ozzu.api.persistence.enums.LoungeMemberRole;
import ai.ozzu.api.persistence.enums.LoungeMemberStatus;
import ai.ozzu.api.persistence.repo.DomainRepository;
import ai.ozzu.api.persistence.repo.LoungeMembershipRepository;
import ai.ozzu.api.persistence.repo.LoungeRepository;
import ai.ozzu.api.persistence.repo.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.swing.text.html.Option;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class LoungeService {

    private static final Logger log = LoggerFactory.getLogger(LoungeService.class);

    private final DomainRepository domainRepository;
    private final UserRepository userRepository;
    private final LoungeRepository loungeRepository;
    private final LoungeMembershipRepository loungeMembershipRepository;

    public LoungeService(
            DomainRepository domainRepository,
            UserRepository userRepository,
            LoungeRepository loungeRepository,
            LoungeMembershipRepository loungeMembershipRepository
    ) {
        this.domainRepository = domainRepository;
        this.userRepository = userRepository;
        this.loungeRepository = loungeRepository;
        this.loungeMembershipRepository = loungeMembershipRepository;
    }

    public Lounge getLounge(UUID domainId, UUID loungeId) {
        Optional<LoungeEntity> loungeEntityOptional = loungeRepository.findById(loungeId);
        if(loungeEntityOptional.isEmpty()) {
            throw new EntityNotFoundException("Lounge not found:"+loungeId);
        }
        if(! loungeEntityOptional.get().getDomain().getId().equals(domainId)) {
            throw new EntityNotFoundException("Domain ID not matching with Lounge");
        }
        return toApi(loungeEntityOptional.get());
    }

    @Transactional(readOnly = true)
    public List<Lounge> listMyLounges(UUID domainId, UUID userId) {
        log.info("lounges.listMyLounges domainId={} userId={}", domainId, userId);

        ensureDomainExists(domainId);
        ensureUserExists(userId);

        // "My lounges" = lounges where membership status is INVITED or JOINED (you can tweak)
        List<LoungeMembershipEntity> memberships =
                loungeMembershipRepository.findByUser_IdAndLounge_Domain_IdAndStatusIn(
                        userId,
                        domainId,
                        List.of(LoungeMemberStatus.INVITED, LoungeMemberStatus.ACTIVE)
                );

        List<Lounge> result = memberships.stream()
                .map(m -> toApi(m.getLounge()))
                .toList();

        log.info("lounges.listMyLounges.result domainId={} userId={} count={}",
                domainId, userId, result.size());

        return result;
    }

    @Transactional
    public Lounge createLounge(UUID domainId, UUID ownerUserId, LoungeCreateRequest req) {
        log.info("lounges.create.start domainId={} ownerUserId={} request={}", domainId, ownerUserId, req);

        DomainEntity domain = domainRepository.findById(domainId)
                .orElseThrow(() -> {
                    log.warn("lounges.create.domainNotFound domainId={}", domainId);
                    return new EntityNotFoundException("Domain not found: " + domainId);
                });

        UserEntity owner = userRepository.findById(ownerUserId)
                .orElseThrow(() -> {
                    log.warn("lounges.create.ownerNotFound ownerUserId={}", ownerUserId);
                    return new EntityNotFoundException("User not found: " + ownerUserId);
                });

        if (req == null || req.getName() == null || req.getName().isBlank()) {
            log.warn("lounges.create.missingName domainId={} ownerUserId={}", domainId, ownerUserId);
            throw new MissingFieldException("Lounge name is required");
        }

        String name = req.getName().trim();

        loungeRepository.findByDomain_IdAndName(domainId, name)
                .ifPresent(existing -> {
                    log.warn("lounges.create.duplicate domainId={} name={}", domainId, name);
                    throw new EntityAlreadyExistsException("Lounge already exists in domain: " + name);
                });

        LoungeEntity lounge = new LoungeEntity();
        lounge.setDomain(domain);
        lounge.setName(name);
        lounge.setDescription(req.getDescription());
        lounge.setOwnerUser(owner);
        lounge.setInternalProperties(req.getInternalProperties() != null ? req.getInternalProperties() : Map.of());

        LoungeEntity saved = loungeRepository.save(lounge);

        // Owner membership: OWNER + JOINED
        LoungeMembershipEntity membership = new LoungeMembershipEntity();
        membership.setLounge(saved);
        membership.setUser(owner);
        membership.setRole(LoungeMemberRole.OWNER);
        membership.setStatus(LoungeMemberStatus.ACTIVE);
        membership.setInvitedByUser(owner);
        membership.setJoinedAt(OffsetDateTime.now());
        membership.setInternalProperties(Map.of());
        loungeMembershipRepository.save(membership);

        log.info("lounges.create.success domainId={} loungeId={} ownerUserId={} name={}",
                domainId, saved.getId(), ownerUserId, saved.getName());

        return toApi(saved);
    }

    private void ensureDomainExists(UUID domainId) {
        if (!domainRepository.existsById(domainId)) {
            log.warn("lounges.domainNotFound domainId={}", domainId);
            throw new EntityNotFoundException("Domain not found: " + domainId);
        }
    }

    private void ensureUserExists(UUID userId) {
        if (!userRepository.existsById(userId)) {
            log.warn("lounges.userNotFound userId={}", userId);
            throw new EntityNotFoundException("User not found: " + userId);
        }
    }

    private Lounge toApi(LoungeEntity e) {
        // NOTE: adjust fields to match your generated model exactly
        Lounge api = new Lounge();
        api.setId(e.getId());
        api.setDomainId(e.getDomain() != null ? e.getDomain().getId() : null);
        api.setName(e.getName());
        api.setDescription(e.getDescription());
        api.setOwnerUserId(e.getOwnerUser() != null ? e.getOwnerUser().getId() : null);
        api.setTimeCreated(e.getCreatedAt());
        api.setTimeUpdated(e.getUpdatedAt());
        api.setInternalProperties(e.getInternalProperties());
        return api;
    }
}
