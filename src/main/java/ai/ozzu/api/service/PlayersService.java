package ai.ozzu.api.service;

import ai.ozzu.api.exceptions.EntityAlreadyExistsException;
import ai.ozzu.api.exceptions.EntityNotFoundException;
import ai.ozzu.api.exceptions.MissingFieldException;
import ai.ozzu.api.generated.model.Player;
import ai.ozzu.api.generated.model.PlayerCreateRequest;
import ai.ozzu.api.persistence.entity.DomainEntity;
import ai.ozzu.api.persistence.entity.PlayerEntity;
import ai.ozzu.api.persistence.repo.DomainRepository;
import ai.ozzu.api.persistence.repo.PlayerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class PlayersService {

    private static final Logger log = LoggerFactory.getLogger(PlayersService.class);

    private final PlayerRepository playerRepository;
    private final DomainRepository domainRepository;

    public PlayersService(PlayerRepository playerRepository, DomainRepository domainRepository) {
        this.playerRepository = playerRepository;
        this.domainRepository = domainRepository;
    }

    @Transactional(readOnly = true)
    public List<Player> listPlayers(UUID domainId) {
        log.info("Listing players for domainId={}", domainId);

        ensureDomainExists(domainId);

        List<Player> players = playerRepository
                .findByDomain_IdOrderByCreatedAtDesc(domainId)
                .stream()
                .map(this::toApi)
                .toList();

        log.info("Found {} players for domainId={}", players.size(), domainId);
        return players;
    }

    @Transactional(readOnly = true)
    public Player getPlayer(UUID domainId, UUID playerId) {
        PlayerEntity player = playerRepository.findByIdAndDomain_Id(playerId, domainId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Player not found: " + playerId
                ));

        return toApi(player);
    }

    @Transactional
    public Player createPlayer(UUID domainId, PlayerCreateRequest req) {
        log.info("Creating player in domainId={} with request={}", domainId, req);

        DomainEntity domain = domainRepository.findById(domainId)
                .orElseThrow(() -> {
                    log.warn("Domain not found while creating player: {}", domainId);
                    return new EntityNotFoundException("Domain not found: " + domainId);
                });

        if (req == null || req.getName() == null || req.getName().isBlank()) {
            log.warn("Missing player name for domainId={}", domainId);
            throw new MissingFieldException("Player name is required");
        }

        String name = req.getName().trim();

        playerRepository.findByDomain_IdAndName(domainId, name)
                .ifPresent(p -> {
                    log.warn(
                            "Duplicate player creation attempt: domainId={}, playerName={}",
                            domainId, name
                    );
                    throw new EntityAlreadyExistsException(
                            "Player already exists in domain: " + name
                    );
                });

        PlayerEntity entity = new PlayerEntity();
        entity.setDomain(domain);
        entity.setName(name);
        entity.setObjectProfile(req.getObjectProfile());
        entity.setObjectStatus(req.getObjectStatus());
        entity.setInternalProperties(
                req.getInternalProperties() != null ? req.getInternalProperties() : Map.of()
        );

        PlayerEntity saved = playerRepository.save(entity);

        log.info(
                "Player created successfully: playerId={}, domainId={}, name={}",
                saved.getId(), domainId, saved.getName()
        );

        return toApi(saved);
    }

    private void ensureDomainExists(UUID domainId) {
        if (!domainRepository.existsById(domainId)) {
            log.warn("Domain does not exist: {}", domainId);
            throw new EntityNotFoundException("Domain not found: " + domainId);
        }
    }

    private Player toApi(PlayerEntity e) {
        log.debug("Mapping PlayerEntity to API model: playerId={}", e.getId());

        Player p = new Player();
        p.setId(e.getId());
        p.setDomainId(e.getDomain() != null ? e.getDomain().getId() : null);
        p.setName(e.getName());
        p.setObjectProfile(e.getObjectProfile());
        p.setObjectStatus(e.getObjectStatus());
        p.setTimeCreated(e.getCreatedAt());
        p.setTimeUpdated(e.getUpdatedAt());
        p.setInternalProperties(e.getInternalProperties());
        p.setImageUrl(e.getImageUrl());
        p.setVideoUrl(e.getVideoUrl());
        p.setThumbnailUrl(e.getThumbnailUrl());
        return p;
    }
}