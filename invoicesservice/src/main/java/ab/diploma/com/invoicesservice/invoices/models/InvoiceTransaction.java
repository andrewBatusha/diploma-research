package ab.diploma.com.invoicesservice.invoices.models;

import ab.diploma.com.invoicesservice.invoices.enums.InvoiceTransactionStatus;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

@DynamoDbBean
public class InvoiceTransaction {
    private String pk; //#invoiceTransaction_fileTransactionId
    private String sk; // invoiceTransactionId
    private Long ttl;
    private Long createdAt;
    private String customerEmail;
    private String invoiceNumber;

    private InvoiceTransactionStatus transactionStatus;

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

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public InvoiceTransactionStatus getTransactionStatus() {
        return transactionStatus;
    }

    public void setTransactionStatus(InvoiceTransactionStatus transactionStatus) {
        this.transactionStatus = transactionStatus;
    }
}
