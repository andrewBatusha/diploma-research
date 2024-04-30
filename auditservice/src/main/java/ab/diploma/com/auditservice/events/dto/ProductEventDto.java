package ab.diploma.com.auditservice.events.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ProductEventDto(
        String id,
        String code,
        String email,
        float price
) {
}
