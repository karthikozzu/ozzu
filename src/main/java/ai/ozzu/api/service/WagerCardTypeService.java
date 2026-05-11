package ai.ozzu.api.service;

import ai.ozzu.api.generated.model.WagerCardType;
import ai.ozzu.api.generated.model.WagerCardTypeBinding;
import ai.ozzu.api.generated.model.WagerCardTypeBindingCreateRequest;
import ai.ozzu.api.generated.model.WagerCardTypeBindingListResponse;
import ai.ozzu.api.generated.model.WagerCardTypeCreateRequest;
import ai.ozzu.api.generated.model.WagerCardTypeListResponse;
import ai.ozzu.api.persistence.entity.ConceptTermEntity;
import ai.ozzu.api.persistence.entity.DomainEntity;
import ai.ozzu.api.persistence.entity.WagerCardTypeBindingEntity;
import ai.ozzu.api.persistence.entity.WagerCardTypeEntity;
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
    public WagerCardTypeListResponse listCardTypesResponse(UUID domainId) {
        log.info("Listing wager card type response: domainId={}", domainId);

        List<WagerCardType> cardTypes = listCardTypes(domainId);

        WagerCardTypeListResponse response = new WagerCardTypeListResponse();
        response.setDomainId(domainId);
        response.setWagerCardTypes(cardTypes);

        return response;
    }

    @Transactional(readOnly = true)
    public List<WagerCardType> listCardTypes(UUID domainId) {
        log.info("Listing wager card types: domainId={}", domainId);

        List<WagerCardType> types = cardTypeRepo.findByDomain_Id(domainId)
                .stream()
                .map(this::toApiWithBindings)
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

        if (req == null) {
            throw new IllegalArgumentException("WagerCardTypeCreateRequest is required");
        }

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

        return toApiWithBindings(saved);
    }

    @Transactional(readOnly = true)
    public WagerCardType getCardType(UUID domainId, UUID typeId) {
        log.info("Getting wager card type: domainId={}, typeId={}", domainId, typeId);

        WagerCardTypeEntity e = cardTypeRepo.findById(typeId)
                .filter(ct -> ct.getDomain() != null && ct.getDomain().getId().equals(domainId))
                .orElseThrow(() -> {
                    log.warn("WagerCardType not found: domainId={}, typeId={}", domainId, typeId);
                    return new IllegalArgumentException("WagerCardType not found");
                });

        return toApiWithBindings(e);
    }

    @Transactional(readOnly = true)
    public WagerCardTypeBindingListResponse listBindingsResponse(UUID domainId, UUID typeId) {
        log.info("Listing wager card type binding response: domainId={}, typeId={}", domainId, typeId);

        List<WagerCardTypeBinding> bindings = listBindings(domainId, typeId);

        WagerCardTypeBindingListResponse response = new WagerCardTypeBindingListResponse();
        response.setDomainId(domainId);
        response.setWagerCardTypeId(typeId);
        response.setWagerCardTypeBindings(bindings);

        return response;
    }

    @Transactional(readOnly = true)
    public List<WagerCardTypeBinding> listBindings(UUID domainId, UUID typeId) {
        log.info("Listing wager card type bindings: domainId={}, typeId={}", domainId, typeId);

        WagerCardTypeEntity type = cardTypeRepo.findById(typeId)
                .filter(ct -> ct.getDomain() != null && ct.getDomain().getId().equals(domainId))
                .orElseThrow(() -> {
                    log.warn(
                            "WagerCardType not found while listing bindings: domainId={}, typeId={}",
                            domainId,
                            typeId
                    );
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

        if (req == null) {
            throw new IllegalArgumentException("WagerCardTypeBindingCreateRequest is required");
        }

        WagerCardTypeEntity type = cardTypeRepo.findById(typeId)
                .filter(ct -> ct.getDomain() != null && ct.getDomain().getId().equals(domainId))
                .orElseThrow(() -> {
                    log.warn(
                            "WagerCardType not found while creating binding: domainId={}, typeId={}",
                            domainId,
                            typeId
                    );
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

    private WagerCardType toApiWithBindings(WagerCardTypeEntity e) {
        WagerCardType api = toApi(e);

        List<WagerCardTypeBinding> bindings = bindingRepo.findByWagerCardType_Id(e.getId())
                .stream()
                .map(this::toApi)
                .toList();

        api.setBindings(bindings);

        return api;
    }

    private WagerCardType toApi(WagerCardTypeEntity e) {
        log.debug("Mapping WagerCardTypeEntity to API model: wagerCardTypeId={}", e.getId());

        WagerCardType api = new WagerCardType();

        api.setId(e.getId());
        api.setDomainId(e.getDomain() != null ? e.getDomain().getId() : null);
        api.setName(e.getName());
        api.setDescription(e.getDescription());

        //api.setMaxBindings(e.getMaxBindings());

        api.setTimeCreated(e.getCreatedAt());
        api.setTimeUpdated(e.getUpdatedAt());

        api.setInternalProperties(e.getInternalProperties());

        return api;
    }

    private WagerCardTypeBinding toApi(WagerCardTypeBindingEntity e) {
        log.debug("Mapping WagerCardTypeBindingEntity to API model: bindingId={}", e.getId());

        WagerCardTypeBinding api = new WagerCardTypeBinding();

        api.setId(e.getId());
        api.setWagerCardTypeId(e.getWagerCardType() != null ? e.getWagerCardType().getId() : null);
        api.setConceptTermId(e.getConceptTerm() != null ? e.getConceptTerm().getId() : null);

        //api.setIsOptional(e.getIsOptional());
        //api.setGroupAffiliation(e.getGroupAffiliation());
        //api.setPointsValue(e.getPointsValue());
        api.setTimeCreated(e.getCreatedAt());
        //api.setTimeUpdated(e.getUpdatedAt());

        api.setInternalProperties(e.getInternalProperties());

        return api;
    }
}