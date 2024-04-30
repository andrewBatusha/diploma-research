package ab.diploma.com.invoicesservice.invoices.repositories;

import ab.diploma.com.invoicesservice.invoices.enums.InvoiceTransactionStatus;
import ab.diploma.com.invoicesservice.invoices.models.InvoiceFileTransaction;
import ab.diploma.com.invoicesservice.invoices.models.InvoiceTransaction;
import com.amazonaws.xray.spring.aop.XRayEnabled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Repository
@XRayEnabled
public class InvoiceTransactionsRepository {

    private static final String PARTITION_KEY = "#invoiceTransaction_";
    private final DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient;

    private final DynamoDbAsyncTable<InvoiceTransaction> invoiceTransactionTable;

    @Autowired
    public InvoiceTransactionsRepository(DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient,
                                         @Value("${invoices.ddb.name}") String invoicesDdbName) {
        this.dynamoDbEnhancedAsyncClient = dynamoDbEnhancedAsyncClient;
        this.invoiceTransactionTable = this.dynamoDbEnhancedAsyncClient.table(invoicesDdbName, TableSchema.fromBean(InvoiceTransaction.class));
    }

    public CompletableFuture<Void> createInvoiceTransaction(String customerEmail, String invoiceNumber,
                                                            String invoiceTransactionId,
                                                            String invoiceFileTransactionId,
                                                            InvoiceTransactionStatus invoiceTransactionStatus) {
        long timestamp = Instant.now().toEpochMilli();
        long ttl = Instant.now().plusSeconds(300).getEpochSecond();

        InvoiceTransaction invoiceTransaction = new InvoiceTransaction();
        invoiceTransaction.setPk(PARTITION_KEY.concat(invoiceFileTransactionId));
        invoiceTransaction.setSk(invoiceTransactionId);
        invoiceTransaction.setTtl(ttl);
        invoiceTransaction.setCreatedAt(timestamp);
        invoiceTransaction.setCustomerEmail(customerEmail);
        invoiceTransaction.setInvoiceNumber(invoiceNumber);
        invoiceTransaction.setTransactionStatus(invoiceTransactionStatus);

        return invoiceTransactionTable.putItem(invoiceTransaction);
    }
}
