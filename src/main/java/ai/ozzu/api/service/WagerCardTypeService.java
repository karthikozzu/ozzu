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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class WagerCardTypeService {

    private final DomainRepository domainRepo;
    private final WagerCardTypeRepository cardTypeRepo;
    private final WagerCardTypeBindingRepository bindingRepo;
    private final ConceptTermRepository conceptTermRepo;

    public WagerCardTypeService(DomainRepository domainRepo, WagerCardTypeRepository cardTypeRepo, WagerCardTypeBindingRepository bindingRepo, ConceptTermRepository conceptTermRepo) {
        this.domainRepo = domainRepo;
        this.cardTypeRepo = cardTypeRepo;
        this.bindingRepo = bindingRepo;
        this.conceptTermRepo = conceptTermRepo;
    }

    @Transactional(readOnly = true)
    public List<WagerCardType> listCardTypes(UUID domainId) {
        return cardTypeRepo.findByDomain_Id(domainId)
                .stream()
                .map(this::toApi)
                .toList();
    }

    @Transactional
    public WagerCardType createCardType(UUID domainId, WagerCardTypeCreateRequest req) {
        DomainEntity domain = domainRepo.findById(domainId)
                .orElseThrow(() -> new IllegalArgumentException("Domain not found: " + domainId));

        WagerCardTypeEntity e = new WagerCardTypeEntity();
        e.setDomain(domain);
        e.setName(req.getName());
        e.setDescription(req.getDescription());
        e.setInternalProperties(req.getInternalProperties());
        WagerCardTypeEntity saved = cardTypeRepo.save(e);
        return toApi(saved);
    }

    @Transactional(readOnly = true)
    public WagerCardType getCardType(UUID domainId, UUID typeId) {
        WagerCardTypeEntity e = cardTypeRepo.findById(typeId)
                .filter(ct -> ct.getDomain().getId().equals(domainId))
                .orElseThrow(() -> new IllegalArgumentException("WagerCardType not found"));
        return toApi(e);
    }

    @Transactional(readOnly = true)
    public List<WagerCardTypeBinding> listBindings(UUID domainId, UUID typeId) {
        WagerCardTypeEntity type = cardTypeRepo.findById(typeId)
                .filter(ct -> ct.getDomain().getId().equals(domainId))
                .orElseThrow(() -> new IllegalArgumentException("WagerCardType not found"));
        return bindingRepo.findByWagerCardType_Id(type.getId())
                .stream()
                .map(this::toApi)
                .toList();
    }

    @Transactional
    public WagerCardTypeBinding createBinding(UUID domainId, UUID typeId, WagerCardTypeBindingCreateRequest req) {
        WagerCardTypeEntity type = cardTypeRepo.findById(typeId)
                .filter(ct -> ct.getDomain().getId().equals(domainId))
                .orElseThrow(() -> new IllegalArgumentException("WagerCardType not found"));

        ConceptTermEntity conceptTerm = conceptTermRepo.findById(req.getConceptTermId())
                .orElseThrow(() -> new IllegalArgumentException("ConceptTerm not found"));

        WagerCardTypeBindingEntity b = new WagerCardTypeBindingEntity();
        b.setDomain(type.getDomain());
        b.setWagerCardType(type);
        b.setConceptTerm(conceptTerm);
        b.setInternalProperties(req.getInternalProperties());
        WagerCardTypeBindingEntity saved = bindingRepo.save(b);
        return toApi(saved);
    }

    private WagerCardType toApi(WagerCardTypeEntity e) {
        WagerCardType api = new WagerCardType();
        api.setId(e.getId());
        api.setDomainId(e.getDomain().getId());
        api.setName(e.getName());
        api.setDescription(e.getDescription());
        api.setInternalProperties(e.getInternalProperties());
        return api;
    }

    private WagerCardTypeBinding toApi(WagerCardTypeBindingEntity e) {
        WagerCardTypeBinding api = new WagerCardTypeBinding();
        api.setId(e.getId());
        api.setWagerCardTypeId(e.getWagerCardType().getId());
        api.setConceptTermId(e.getConceptTerm().getId());
        api.setInternalProperties(e.getInternalProperties());
        return api;
    }
}