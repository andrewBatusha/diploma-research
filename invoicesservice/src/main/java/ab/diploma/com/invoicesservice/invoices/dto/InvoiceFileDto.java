package ab.diploma.com.invoicesservice.invoices.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)

public record InvoiceFileDto(
        String customerEmail,
        String invoiceNumber,
        Float totalValue,
        List<InvoiceProductFileDto> products
) {
}
