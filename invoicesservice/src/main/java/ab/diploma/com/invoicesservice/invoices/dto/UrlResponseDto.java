package ab.diploma.com.invoicesservice.invoices.dto;

public record UrlResponseDto(
        String url,
        int expireIn,
        String transactionId
) {
}
