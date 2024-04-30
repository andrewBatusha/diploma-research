package ab.diploma.com.invoicesservice.controller;

import ab.diploma.com.invoicesservice.invoices.dto.InvoiceApiDto;
import ab.diploma.com.invoicesservice.invoices.dto.InvoiceFileTransactionApiDto;
import ab.diploma.com.invoicesservice.invoices.dto.InvoiceProductApiDto;
import ab.diploma.com.invoicesservice.invoices.dto.UrlResponseDto;
import ab.diploma.com.invoicesservice.invoices.models.InvoiceFileTransaction;
import ab.diploma.com.invoicesservice.invoices.repositories.InvoiceRepository;
import ab.diploma.com.invoicesservice.invoices.repositories.InvoicesFileTransactionsRepository;
import ab.diploma.com.invoicesservice.invoices.services.S3InvoicesService;
import com.amazonaws.xray.spring.aop.XRayEnabled;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@XRayEnabled
@RestController
@RequestMapping("/api/invoices")
public class InvoicesController {
    private static final Logger LOG = LogManager.getLogger(InvoicesController.class);
    private final S3InvoicesService s3InvoicesService;
    private final InvoiceRepository invoiceRepository;
    private final InvoicesFileTransactionsRepository invoicesFileTransactionsRepository;

    @Autowired
    public InvoicesController(S3InvoicesService s3InvoicesService,
                              InvoiceRepository invoiceRepository,
                              InvoicesFileTransactionsRepository invoicesFileTransactionsRepository) {
        this.s3InvoicesService = s3InvoicesService;
        this.invoiceRepository = invoiceRepository;
        this.invoicesFileTransactionsRepository = invoicesFileTransactionsRepository;
    }

    @GetMapping
    public ResponseEntity<List<InvoiceApiDto>> getAllInvoicesByEmail(@RequestParam String email) {
        LOG.info("Get all invoices by email");
        List<InvoiceApiDto> invoiceApiDto = new ArrayList<>();
        invoiceRepository.findByCustomerEmail(email).subscribe(invoicePage -> {
            invoiceApiDto.addAll(
                    invoicePage.items().parallelStream()
                            .map(invoice -> new InvoiceApiDto(
                                    invoice.getPk().split("_")[1],
                                    invoice.getSk(),
                                    invoice.getTotalValue(),
                                    invoice.getProducts().parallelStream()
                                            .map(invoiceProduct -> new InvoiceProductApiDto(
                                                    invoiceProduct.getId(),
                                                    invoiceProduct.getQuantity()
                                            )).toList(),
                                    invoice.getInvoiceTransactionId(),
                                    invoice.getFileTransactionId(),
                                    invoice.getCreatedAt()
                            )).toList());
        }).join();

        return new ResponseEntity<>(invoiceApiDto, HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<UrlResponseDto> generatePreSignedUrl(@RequestHeader("requestId") String requestId) {
        String key = UUID.randomUUID().toString();
        int expiresIn = 300;

        ThreadContext.put("invoiceFileTransactionId", key);

        String preSignedUrl = s3InvoicesService.generatePreSignedUrl(key, expiresIn);

        invoicesFileTransactionsRepository.createInvoiceFileTransaction(key, requestId, expiresIn).join();

        LOG.info("Invoice file transaction generated...");

        return new ResponseEntity<>(new UrlResponseDto(preSignedUrl, expiresIn, key), HttpStatus.OK);
    }

    @GetMapping("/transactions/{fileTransactionId}")
    public ResponseEntity<?> getInvoiceFileTransaction(@PathVariable("fileTransactionId") String fileTransactionId) {
        LOG.info("Get invoice file transaction by its id: {}", fileTransactionId);
        InvoiceFileTransaction invoiceFileTransaction = invoicesFileTransactionsRepository
                .getInvoiceFileTransaction(fileTransactionId).join();
        if (invoiceFileTransaction != null) {
            return new ResponseEntity<>(new InvoiceFileTransactionApiDto(
                    fileTransactionId, invoiceFileTransaction.getFileTransactionStatus().name()), HttpStatus.OK);
        } else {
            return new ResponseEntity<>("Invoice file transaction not found", HttpStatus.NOT_FOUND);
        }
    }
}
