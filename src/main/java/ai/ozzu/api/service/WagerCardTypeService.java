package ai.ozzu.api.service;

import ai.ozzu.api.generated.model.WagerCardType;
import ai.ozzu.api.generated.model.WagerCardTypeBinding;
import ai.ozzu.api.generated.model.WagerCardTypeBindingCreateRequest;
import ai.ozzu.api.generated.model.WagerCardTypeCreateRequest;
import ai.ozzu.api.persistence.entity.*;
import ai.ozzu.api.persistence.repo.ConceptTermRepository;
import ai.ozzu.api.persistence.repo.DomainRepository;
import ai.ozzu.api.persistence.repo.WagerCardTypeBindingRepository;
import ai.ozzu.api.persistence.repo.WagerCardTypeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class WagerCardTypeService {

    private static final Logger log = LoggerFactory.getLogger(WagerCardTypeService.class);

    private final DomainRepository domainRepo;
    private final WagerCardTypeRepository cardTypeRepo;
    private final WagerCardTypeBindingRepository bindingRepo;
    private final ConceptTermRepository conceptTermRepo;

    public WagerCardTypeService(
            DomainRepository domainRepo,
            WagerCardTypeRepository cardTypeRepo,
            WagerCardTypeBindingRepository bindingRepo,
            ConceptTermRepository conceptTermRepo
    ) {
        this.domainRepo = domainRepo;
        this.cardTypeRepo = cardTypeRepo;
        this.bindingRepo = bindingRepo;
        this.conceptTermRepo = conceptTermRepo;
    }

    @Transactional(readOnly = true)
    public List<WagerCardType> listCardTypes(UUID domainId) {
        log.info("Listing wager card types: domainId={}", domainId);

        List<WagerCardType> types = cardTypeRepo.findByDomain_Id(domainId)
                .stream()
                .map(this::toApi)
                .toList();

        log.info("Found {} wager card types: domainId={}", types.size(), domainId);
        return types;
    }

    @Transactional
    public WagerCardType createCardType(UUID domainId, WagerCardTypeCreateRequest req) {
        log.info("Creating wager card type: domainId={}, request={}", domainId, req);

        DomainEntity domain = domainRepo.findById(domainId)
                .orElseThrow(() -> {
                    log.warn("Domain not found while creating wager card type: domainId={}", domainId);
                    return new IllegalArgumentException("Domain not found: " + domainId);
                });

        WagerCardTypeEntity e = new WagerCardTypeEntity();
        e.setDomain(domain);
        e.setName(req.getName());
        e.setDescription(req.getDescription());
        e.setInternalProperties(req.getInternalProperties());

        WagerCardTypeEntity saved = cardTypeRepo.save(e);

        log.info(
                "Wager card type created successfully: wagerCardTypeId={}, domainId={}, name={}",
                saved.getId(),
                domainId,
                saved.getName()
        );

        return toApi(saved);
    }

    @Transactional(readOnly = true)
    public WagerCardType getCardType(UUID domainId, UUID typeId) {
        log.info("Getting wager card type: domainId={}, typeId={}", domainId, typeId);

        WagerCardTypeEntity e = cardTypeRepo.findById(typeId)
                .filter(ct -> ct.getDomain().getId().equals(domainId))
                .orElseThrow(() -> {
                    log.warn("WagerCardType not found: domainId={}, typeId={}", domainId, typeId);
                    return new IllegalArgumentException("WagerCardType not found");
                });

        return toApi(e);
    }

    @Transactional(readOnly = true)
    public List<WagerCardTypeBinding> listBindings(UUID domainId, UUID typeId) {
        log.info("Listing wager card type bindings: domainId={}, typeId={}", domainId, typeId);

        WagerCardTypeEntity type = cardTypeRepo.findById(typeId)
                .filter(ct -> ct.getDomain().getId().equals(domainId))
                .orElseThrow(() -> {
                    log.warn("WagerCardType not found while listing bindings: domainId={}, typeId={}",
                            domainId, typeId);
                    return new IllegalArgumentException("WagerCardType not found");
                });

        List<WagerCardTypeBinding> bindings = bindingRepo.findByWagerCardType_Id(type.getId())
                .stream()
                .map(this::toApi)
                .toList();

        log.info(
                "Found {} bindings for wager card type: domainId={}, typeId={}",
                bindings.size(),
                domainId,
                typeId
        );

        return bindings;
    }

    @Transactional
    public WagerCardTypeBinding createBinding(
            UUID domainId,
            UUID typeId,
            WagerCardTypeBindingCreateRequest req
    ) {
        log.info(
                "Creating wager card type binding: domainId={}, typeId={}, request={}",
                domainId,
                typeId,
                req
        );

        WagerCardTypeEntity type = cardTypeRepo.findById(typeId)
                .filter(ct -> ct.getDomain().getId().equals(domainId))
                .orElseThrow(() -> {
                    log.warn("WagerCardType not found while creating binding: domainId={}, typeId={}",
                            domainId, typeId);
                    return new IllegalArgumentException("WagerCardType not found");
                });

        ConceptTermEntity conceptTerm = conceptTermRepo.findById(req.getConceptTermId())
                .orElseThrow(() -> {
                    log.warn(
                            "ConceptTerm not found while creating binding: conceptTermId={}, domainId={}, typeId={}",
                            req.getConceptTermId(),
                            domainId,
                            typeId
                    );
                    return new IllegalArgumentException("ConceptTerm not found");
                });

        WagerCardTypeBindingEntity b = new WagerCardTypeBindingEntity();
        b.setDomain(type.getDomain());
        b.setWagerCardType(type);
        b.setConceptTerm(conceptTerm);
        b.setInternalProperties(req.getInternalProperties());

        WagerCardTypeBindingEntity saved = bindingRepo.save(b);

        log.info(
                "Wager card type binding created successfully: bindingId={}, domainId={}, typeId={}, conceptTermId={}",
                saved.getId(),
                domainId,
                typeId,
                conceptTerm.getId()
        );

        return toApi(saved);
    }

    private WagerCardType toApi(WagerCardTypeEntity e) {
        log.debug("Mapping WagerCardTypeEntity to API model: wagerCardTypeId={}", e.getId());

        WagerCardType api = new WagerCardType();
        api.setId(e.getId());
        api.setDomainId(e.getDomain().getId());
        api.setName(e.getName());
        api.setDescription(e.getDescription());
        api.setInternalProperties(e.getInternalProperties());
        return api;
    }

    private WagerCardTypeBinding toApi(WagerCardTypeBindingEntity e) {
        log.debug("Mapping WagerCardTypeBindingEntity to API model: bindingId={}", e.getId());

        WagerCardTypeBinding api = new WagerCardTypeBinding();
        api.setId(e.getId());
        api.setWagerCardTypeId(e.getWagerCardType().getId());
        api.setConceptTermId(e.getConceptTerm().getId());
        api.setInternalProperties(e.getInternalProperties());
        return api;
    }
}