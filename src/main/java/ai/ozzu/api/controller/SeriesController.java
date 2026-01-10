package ai.ozzu.api.controller;

import ai.ozzu.api.generated.api.SeriesApi;
import ai.ozzu.api.generated.model.Series;
import ai.ozzu.api.generated.model.SeriesCreateRequest;
import ai.ozzu.api.service.SeriesService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.NativeWebRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
public class SeriesController implements SeriesApi {

    private final SeriesService seriesService;

    public SeriesController(SeriesService seriesService) {
        this.seriesService = seriesService;
    }


    @Override
    public Optional<NativeWebRequest> getRequest() {
        return SeriesApi.super.getRequest();
    }

    @Override
    public ResponseEntity<List<Series>> ozzuDomainsDomainIdSeriesGet(UUID domainId) {
        return ResponseEntity.ok(seriesService.listSeries(domainId));
    }

    @Override
    public ResponseEntity<Series> ozzuDomainsDomainIdSeriesPost(UUID domainId, SeriesCreateRequest seriesCreateRequest) {
        Series created = seriesService.createSeries(domainId, seriesCreateRequest);
        return ResponseEntity.status(201).body(created);
    }

    @Override
    public ResponseEntity<Series> ozzuDomainsDomainIdSeriesSeriesIdGet(UUID domainId, UUID seriesId) {
        return ResponseEntity.ok(seriesService.getSeries(domainId, seriesId));
    }
}
