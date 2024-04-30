package ab.diploma.com.auditservice.events.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SnsMessageAttribute(
        @JsonProperty("Type")
        String type,
        @JsonProperty("Value")
        String value
) {
}
