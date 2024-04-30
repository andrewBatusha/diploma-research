package ab.diploma.com.invoicesservice.invoices.models;

import ab.diploma.com.invoicesservice.invoices.enums.InvoiceFileTransactionStatus;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

@DynamoDbBean
public class InvoiceFileTransaction {
    private String pk; //#fileTransaction
    private String sk; // file transaction id
    private String requestId;
    private Long createdAt;
    private Long ttl;
    private Integer expiresIn;
    private InvoiceFileTransactionStatus fileTransactionStatus;

    @DynamoDbPartitionKey
    public String getPk() {
        return pk;
    }

    @DynamoDbSortKey
    public String getSk() {
        return sk;
    }

    public void setPk(String pk) {
        this.pk = pk;
    }

    public void setSk(String sk) {
        this.sk = sk;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

    public Long getTtl() {
        return ttl;
    }

    public void setTtl(Long ttl) {
        this.ttl = ttl;
    }

    public Integer getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(Integer expiresIn) {
        this.expiresIn = expiresIn;
    }

    public InvoiceFileTransactionStatus getFileTransactionStatus() {
        return fileTransactionStatus;
    }

    public void setFileTransactionStatus(InvoiceFileTransactionStatus fileTransactionStatus) {
        this.fileTransactionStatus = fileTransactionStatus;
    }
}
