package ab.diploma.com.auditservice.products.models;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

@DynamoDbBean
public class ProductInfoEvent {
    private String id;
    private String code;
    private Float price;
    private String messageId;
    private String requestId;
    private String traceId;

    public void setId(String id) {
        this.id = id;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public void setPrice(Float price) {
        this.price = price;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public String getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public Float getPrice() {
        return price;
    }

    public String getMessageId() {
        return messageId;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getTraceId() {
        return traceId;
    }
}
