package ab.diploma.com.invoicesservice.invoices.repositories;

import ab.diploma.com.invoicesservice.invoices.dto.InvoiceFileDto;
import ab.diploma.com.invoicesservice.invoices.models.Invoice;
import ab.diploma.com.invoicesservice.invoices.models.InvoiceFileTransaction;
import ab.diploma.com.invoicesservice.invoices.models.InvoiceProduct;
import com.amazonaws.xray.spring.aop.XRayEnabled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

@Repository
@XRayEnabled
public class InvoiceRepository {

    private static final String PARTITION_KEY = "#invoice_";
    private final DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient;

    private final DynamoDbAsyncTable<Invoice> invoiceTable;

    @Autowired
    public InvoiceRepository(
            @Value("${invoices.ddb.name}") String invoicesDdbName,
            DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient) {
        this.dynamoDbEnhancedAsyncClient = dynamoDbEnhancedAsyncClient;
        this.invoiceTable = dynamoDbEnhancedAsyncClient.table(invoicesDdbName, TableSchema.fromBean(Invoice.class));
    }

    public CompletableFuture<Void> createInvoice(InvoiceFileDto invoiceFileDto, String invoiceTransactionId,
                                                 String invoiceFileTransactionId) {
        long timestamp = Instant.now().toEpochMilli();

        Invoice invoice = new Invoice();
        invoice.setPk(PARTITION_KEY.concat(invoiceFileDto.customerEmail()));
        invoice.setSk(invoiceFileDto.invoiceNumber());
        invoice.setTotalValue(invoiceFileDto.totalValue());
        invoice.setProducts(invoiceFileDto.products().stream().map(invoiceProductFileDto -> {
            InvoiceProduct invoiceProduct = new InvoiceProduct();
            invoiceProduct.setId(invoiceProductFileDto.id());
            invoiceProduct.setQuantity(invoiceProductFileDto.quantity());
            return invoiceProduct;
        }).toList());
        invoice.setInvoiceTransactionId(invoiceTransactionId);
        invoice.setFileTransactionId(invoiceFileTransactionId);
        invoice.setCreatedAt(timestamp);
        invoice.setTtl(0L);

        return invoiceTable.putItem(invoice);
    }

    public SdkPublisher<Page<Invoice>> findByCustomerEmail(String email) {
        String pk = PARTITION_KEY.concat(email);
        return invoiceTable.query(QueryEnhancedRequest.builder()
                .queryConditional(QueryConditional.keyEqualTo(Key.builder()
                        .partitionValue(pk)
                        .build()))
                .build());
    }
}
