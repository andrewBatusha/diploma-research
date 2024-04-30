package ab.diploma.com.auditservice.events.dto;

public record SnsAttributes(
        SnsMessageAttribute traceId,
        SnsMessageAttribute eventType,
        SnsMessageAttribute requestId
) {
}
