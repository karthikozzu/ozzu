package ai.ozzu.api.controller;

import ai.ozzu.api.generated.api.LoungeEntriesApi;
import ai.ozzu.api.generated.model.LoungeEntry;
import ai.ozzu.api.generated.model.LoungeEntryCreateRequest;
import ai.ozzu.api.security.AuthContext;
import ai.ozzu.api.service.LoungeEntryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.NativeWebRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
public class LoungeEntryController implements LoungeEntriesApi {

    private final LoungeEntryService loungeEntryService;
    private final AuthContext authContext;

    public LoungeEntryController(LoungeEntryService loungeEntryService, AuthContext authContext) {
        this.loungeEntryService = loungeEntryService;
        this.authContext = authContext;
    }

    @Override
    public Optional<NativeWebRequest> getRequest() {
        return LoungeEntriesApi.super.getRequest();
    }

    @Override
    public ResponseEntity<LoungeEntry> ozzuDomainsDomainIdEventsEventIdEventLoungesEventLoungeIdLoungeEntriesPost(
            UUID domainId,
            UUID eventId,
            UUID eventLoungeId,
            LoungeEntryCreateRequest loungeEntryCreateRequest
    ) {
        UUID userId = authContext.currentUserId();
        LoungeEntry created = loungeEntryService.createEntry(
                domainId, eventId, eventLoungeId, userId, loungeEntryCreateRequest
        );
        return ResponseEntity.status(201).body(created);
    }

    @Override
    public ResponseEntity<List<LoungeEntry>> ozzuDomainsDomainIdEventsEventIdEventLoungesEventLoungeIdLoungeEntriesGet(
            UUID domainId, UUID eventId, UUID eventLoungeId, UUID userId) {

        List<LoungeEntry> loungeEntries = loungeEntryService.getLoungeEntries(domainId, eventId, eventLoungeId, userId);
        return ResponseEntity.status(200).body(loungeEntries);
    }
}