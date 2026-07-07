package ai.ozzu.api.controller;

import ai.ozzu.api.generated.api.PlayersApi;
import ai.ozzu.api.generated.model.Player;
import ai.ozzu.api.generated.model.PlayerCreateRequest;
import ai.ozzu.api.service.PlayersService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.NativeWebRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
public class TeamPlayerController implements PlayersApi {

    private final PlayersService playersService;

    public TeamPlayerController(PlayersService playersService) {
        this.playersService = playersService;
    }

    @Override
    public Optional<NativeWebRequest> getRequest() {
        return PlayersApi.super.getRequest();
    }

    @Override
    public ResponseEntity<List<Player>> ozzuDomainsDomainIdPlayersGet(UUID domainId) {
        return ResponseEntity.ok(playersService.listPlayers(domainId));
    }

    @Override
    public ResponseEntity<Player> ozzuDomainsDomainIdPlayersPost(UUID domainId, PlayerCreateRequest playerCreateRequest) {
        Player created = playersService.createPlayer(domainId, playerCreateRequest);
        return ResponseEntity.status(201).body(created);
    }

    @Override
    public ResponseEntity<Player> ozzuDomainsDomainIdPlayersPlayerIdGet(UUID domainId, UUID playerId) {
        Player player = playersService.getPlayer(domainId, playerId);
        return ResponseEntity.ok(player);
    }
}
