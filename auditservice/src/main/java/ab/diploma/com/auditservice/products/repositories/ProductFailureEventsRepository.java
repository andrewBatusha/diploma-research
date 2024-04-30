package ab.diploma.com.auditservice.products.repositories;

import ab.diploma.com.auditservice.events.dto.ProductEventDto;
import ab.diploma.com.auditservice.events.dto.ProductEventType;
import ab.diploma.com.auditservice.events.dto.ProductFailureEventDto;
import ab.diploma.com.auditservice.products.models.ProductEvent;
import ab.diploma.com.auditservice.products.models.ProductFailureEvent;
import ab.diploma.com.auditservice.products.models.ProductInfoEvent;
import ab.diploma.com.auditservice.products.models.ProductInfoFailureEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

@Repository
public class ProductFailureEventsRepository {
    private static final Logger LOG = LoggerFactory.getLogger(ProductFailureEventsRepository.class);
    private final DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient;

    private final DynamoDbAsyncTable<ProductFailureEvent> productFailureEventsTable;

    @Autowired
    public ProductFailureEventsRepository(@Value("${aws.events.ddb}") String eventsDdbName,
                                   DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient) {
        this.dynamoDbEnhancedAsyncClient = dynamoDbEnhancedAsyncClient;
        this.productFailureEventsTable = dynamoDbEnhancedAsyncClient.table(eventsDdbName, TableSchema.fromBean(ProductFailureEvent.class));
    }

    public CompletableFuture<Void> create(ProductFailureEventDto productEventDto,
                                          ProductEventType productEventType,
                                          String messageId, String requestId, String traceId) {
        long timestamp = Instant.now().toEpochMilli();
        long ttl = Instant.now().plusSeconds(300).getEpochSecond();

        ProductFailureEvent productFailureEvent = new ProductFailureEvent();
        productFailureEvent.setPk("#product_".concat(productEventType.name()));
        productFailureEvent.setSk(String.valueOf(timestamp));
        productFailureEvent.setCreatedAt(timestamp);
        productFailureEvent.setTtl(ttl);
        productFailureEvent.setEmail(productEventDto.email());

        ProductInfoFailureEvent productInfoFailureEvent = new ProductInfoFailureEvent();
        productInfoFailureEvent.setId(productEventDto.id());
        productInfoFailureEvent.setMessageId(messageId);
        productInfoFailureEvent.setRequestId(requestId);
        productInfoFailureEvent.setTraceId(traceId);
        productInfoFailureEvent.setError(productEventDto.error());
        productInfoFailureEvent.setStatus(productEventDto.status());

        productFailureEvent.setInfo(productInfoFailureEvent);
        return productFailureEventsTable.putItem(productFailureEvent);
    }
}
