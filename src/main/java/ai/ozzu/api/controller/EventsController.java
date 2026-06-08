package ai.ozzu.api.controller;

import ai.ozzu.api.exceptions.BadRequestException;
import ai.ozzu.api.generated.api.EventsApi;
import ai.ozzu.api.generated.model.Event;
import ai.ozzu.api.generated.model.EventListResponse;
import ai.ozzu.api.generated.model.EventPageResponse;
import ai.ozzu.api.persistence.models.EventCreateRequest;
import ai.ozzu.api.service.EventsService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

@RestController
public class EventsController implements EventsApi {

    private final EventsService eventsService;
    private final ObjectMapper objectMapper;

    public EventsController(EventsService eventsService, ObjectMapper objectMapper) {
        this.eventsService = eventsService;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<NativeWebRequest> getRequest() {
        return EventsApi.super.getRequest();
    }

    @Override
    public ResponseEntity<EventPageResponse> ozzuDomainsDomainIdEventsEventIdGet(UUID domainId, UUID eventId) {
        return ResponseEntity.ok(eventsService.getEventPage(domainId, eventId));
    }

    @Override
    public ResponseEntity<Event> ozzuDomainsDomainIdEventsPost(UUID domainId, String name, String description,
                                                               UUID seriesId, OffsetDateTime timeEventStart,
                                                               OffsetDateTime timeEventEnd, MultipartFile image,
                                                              String internalProperties) {
        EventCreateRequest eventCreateRequest = new EventCreateRequest();
        eventCreateRequest.setImage(image.getResource());
        eventCreateRequest.setName(name);
        eventCreateRequest.setTimeEventEnd(timeEventEnd);
        eventCreateRequest.setTimeEventStart(timeEventStart);
        eventCreateRequest.setDescription(description);
        eventCreateRequest.setSeriesId(seriesId);
        try {
            eventCreateRequest.setInternalProperties(internalProperties == null || internalProperties.isBlank() ?
                    new HashMap<>() :
                    objectMapper.readValue(
                            internalProperties,
                            new TypeReference<>() {
                            }

                    ));
        }
        catch (Exception e) {
            throw new BadRequestException(e.getMessage());
        }
        Event created = eventsService.createEvent(domainId, eventCreateRequest);
        return ResponseEntity.status(201).body(created);
    }

    @Override
    public ResponseEntity<EventListResponse> ozzuDomainsDomainIdEventsGet(
            UUID domainId,
            UUID seriesId,
            UUID teamId,
            LocalDate fromDate,
            LocalDate toDate,
            String status,
            Integer limit,
            String cursor
    ) {
        return ResponseEntity.ok(eventsService.listEvents(domainId, seriesId, teamId, fromDate, toDate, status, limit, cursor));
    }
}
