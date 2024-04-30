package ab.diploma.com.productservice.events.dto;

public record ProductEventDto(
        String id,
        String code,
        String email,
        float price
) {
}
