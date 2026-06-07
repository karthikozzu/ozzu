package ai.ozzu.api.persistence.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * EventCreateRequest
 */

public class EventCreateRequest {

    private String name;

    private String description;

    private UUID seriesId;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime timeEventStart;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime timeEventEnd;

    private org.springframework.core.io.Resource image;

    @Valid
    private Map<String, Object> internalProperties = new HashMap<>();

    public EventCreateRequest() {
        super();
    }

    /**
     * Constructor with only required parameters
     */
    public EventCreateRequest(String name) {
        this.name = name;
    }

    /**
     * Get name
     * @return name
     */
    @NotNull
    @Schema(name = "name", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("name")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * Get description
     * @return description
     */

    @Schema(name = "description", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @JsonProperty("description")
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Get seriesId
     * @return seriesId
     */
    @Valid
    @Schema(name = "seriesId", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @JsonProperty("seriesId")
    public UUID getSeriesId() {
        return seriesId;
    }

    public void setSeriesId(UUID seriesId) {
        this.seriesId = seriesId;
    }

    /**
     * Get timeEventStart
     * @return timeEventStart
     */
    @Valid
    @Schema(name = "timeEventStart", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @JsonProperty("timeEventStart")
    public OffsetDateTime getTimeEventStart() {
        return timeEventStart;
    }

    public void setTimeEventStart(OffsetDateTime timeEventStart) {
        this.timeEventStart = timeEventStart;
    }

    /**
     * Get timeEventEnd
     * @return timeEventEnd
     */
    @Valid
    @Schema(name = "timeEventEnd", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @JsonProperty("timeEventEnd")
    public OffsetDateTime getTimeEventEnd() {
        return timeEventEnd;
    }

    public void setTimeEventEnd(OffsetDateTime timeEventEnd) {
        this.timeEventEnd = timeEventEnd;
    }

    /**
     * Base64 encoded profile photo
     * @return image
     */
    @Valid
    @Schema(name = "image", description = "Base64 encoded profile photo", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @JsonProperty("image")
    public org.springframework.core.io.Resource getImage() {
        return image;
    }

    public void setImage(org.springframework.core.io.Resource image) {
        this.image = image;
    }

    /**
     * Arbitrary JSON properties for forward compatibility.
     * @return internalProperties
     */

    @Schema(name = "internalProperties", description = "Arbitrary JSON properties for forward compatibility.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @JsonProperty("internalProperties")
    public Map<String, Object> getInternalProperties() {
        return internalProperties;
    }

    public void setInternalProperties(Map<String, Object> internalProperties) {
        this.internalProperties = internalProperties;
    }
}


