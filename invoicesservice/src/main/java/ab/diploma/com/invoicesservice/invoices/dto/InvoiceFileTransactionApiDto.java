package ab.diploma.com.invoicesservice.invoices.dto;

public record InvoiceFileTransactionApiDto(
        String transactionId,
        String status
) {
}
