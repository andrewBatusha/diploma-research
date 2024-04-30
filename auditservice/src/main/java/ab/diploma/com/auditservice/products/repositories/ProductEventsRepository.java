package ab.diploma.com.auditservice.products.repositories;

import ab.diploma.com.auditservice.events.dto.ProductEventDto;
import ab.diploma.com.auditservice.events.dto.ProductEventType;
import ab.diploma.com.auditservice.products.models.ProductEvent;
import ab.diploma.com.auditservice.products.models.ProductInfoEvent;
import com.amazonaws.xray.spring.aop.XRayEnabled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Repository
@XRayEnabled
public class ProductEventsRepository {

    private static final Logger LOG = LoggerFactory.getLogger(ProductEventsRepository.class);
    private final DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient;

    private final DynamoDbAsyncTable<ProductEvent> productEventsTable;

    @Autowired
    public ProductEventsRepository(@Value("${aws.events.ddb}") String eventsDdbName, DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient) {
        this.dynamoDbEnhancedAsyncClient = dynamoDbEnhancedAsyncClient;
        this.productEventsTable = dynamoDbEnhancedAsyncClient.table(eventsDdbName, TableSchema.fromBean(ProductEvent.class));
    }

    public CompletableFuture<Void> create(ProductEventDto productEventDto, ProductEventType productEventType, String messageId, String requestId, String traceId) {
        long timestamp = Instant.now().toEpochMilli();
        long ttl = Instant.now().plusSeconds(300).getEpochSecond();

        ProductEvent productEvent = new ProductEvent();
        productEvent.setPk("#product_".concat(productEventType.name()));
        productEvent.setSk(String.valueOf(timestamp));
        productEvent.setCreatedAt(timestamp);
        productEvent.setTtl(ttl);
        productEvent.setEmail(productEventDto.email());

        ProductInfoEvent productInfoEvent = new ProductInfoEvent();
        productInfoEvent.setCode(productEventDto.code());
        productInfoEvent.setId(productEventDto.id());
        productInfoEvent.setPrice(productEventDto.price());
        productInfoEvent.setMessageId(messageId);
        productInfoEvent.setRequestId(requestId);
        productInfoEvent.setTraceId(traceId);

        productEvent.setInfo(productInfoEvent);
        return productEventsTable.putItem(productEvent);
    }

    private Map<String, AttributeValue> buildExclusiveStartKey(String pk, String exclusiveStartTimestamp) {
        return exclusiveStartTimestamp != null
                ? Map.of("pk", AttributeValue.builder().s(pk).build(),
                "sk", AttributeValue.builder().s(exclusiveStartTimestamp).build())
                : null;

    }

    public SdkPublisher<Page<ProductEvent>> findByType(String productEventType, String exclusiveStartTimestamp, int limit) {
        String pk = "#product_".concat(productEventType);
        return productEventsTable.query(QueryEnhancedRequest.builder()
                .queryConditional
                        (QueryConditional.keyEqualTo(Key.builder().partitionValue(pk).build()))
                .exclusiveStartKey(buildExclusiveStartKey(pk, exclusiveStartTimestamp))
                .limit(limit)
                .build()).limit(1);
    }

    public SdkPublisher<Page<ProductEvent>> findByTypeAndRange(String productEventType,
                                                               String exclusiveStartTimestamp,
                                                               String from,
                                                               String to,
                                                               int limit) {
        String pk = "#product_".concat(productEventType);
        return productEventsTable.query(QueryEnhancedRequest.builder()
                .queryConditional
                        (QueryConditional.sortBetween(
                                Key.builder().partitionValue(pk).sortValue(from).build(),
                                Key.builder().partitionValue(pk).sortValue(to).build()))
                .exclusiveStartKey(buildExclusiveStartKey(pk, exclusiveStartTimestamp))
                .limit(limit)
                .build()).limit(1);
    }
}
