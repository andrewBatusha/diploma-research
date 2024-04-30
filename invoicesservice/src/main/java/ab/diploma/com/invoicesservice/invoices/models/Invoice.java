package ab.diploma.com.invoicesservice.invoices.models;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

import java.util.List;

@DynamoDbBean
public class Invoice {
    private String pk; //#invoice_customerEmail
    private String sk; //invoiceNumber

    private Float totalValue;
    private List<InvoiceProduct> products;
    private String invoiceTransactionId;
    private String fileTransactionId;
    private Long ttl;
    private Long createdAt;

    @DynamoDbPartitionKey
    public String getPk() {
        return pk;
    }

    public void setPk(String pk) {
        this.pk = pk;
    }

    @DynamoDbSortKey
    public String getSk() {
        return sk;
    }

    public void setSk(String sk) {
        this.sk = sk;
    }

    public Float getTotalValue() {
        return totalValue;
    }

    public void setTotalValue(Float totalValue) {
        this.totalValue = totalValue;
    }

    public List<InvoiceProduct> getProducts() {
        return products;
    }

    public void setProducts(List<InvoiceProduct> products) {
        this.products = products;
    }

    public String getInvoiceTransactionId() {
        return invoiceTransactionId;
    }

    public void setInvoiceTransactionId(String invoiceTransactionId) {
        this.invoiceTransactionId = invoiceTransactionId;
    }

    public String getFileTransactionId() {
        return fileTransactionId;
    }

    public void setFileTransactionId(String fileTransactionId) {
        this.fileTransactionId = fileTransactionId;
    }

    public Long getTtl() {
        return ttl;
    }

    public void setTtl(Long ttl) {
        this.ttl = ttl;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }
}
