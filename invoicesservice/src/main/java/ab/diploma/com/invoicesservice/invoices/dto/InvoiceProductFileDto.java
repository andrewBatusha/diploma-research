package ab.diploma.com.invoicesservice.invoices.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record InvoiceProductFileDto(
        String id, int quantity
) { }
