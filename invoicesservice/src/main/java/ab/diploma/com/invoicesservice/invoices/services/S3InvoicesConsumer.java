package ab.diploma.com.invoicesservice.invoices.services;

import ab.diploma.com.invoicesservice.invoices.dto.InvoiceFileDto;
import ab.diploma.com.invoicesservice.invoices.enums.InvoiceFileTransactionStatus;
import ab.diploma.com.invoicesservice.invoices.enums.InvoiceTransactionStatus;
import ab.diploma.com.invoicesservice.invoices.models.InvoiceFileTransaction;
import ab.diploma.com.invoicesservice.invoices.repositories.InvoiceRepository;
import ab.diploma.com.invoicesservice.invoices.repositories.InvoiceTransactionsRepository;
import ab.diploma.com.invoicesservice.invoices.repositories.InvoicesFileTransactionsRepository;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification;
import com.amazonaws.services.lambda.runtime.serialization.PojoSerializer;
import com.amazonaws.services.lambda.runtime.serialization.events.LambdaEventSerializers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class S3InvoicesConsumer {
    private static final Logger LOG = LogManager.getLogger(S3InvoicesConsumer.class);
    private final S3AsyncClient s3AsyncClient;
    private final InvoicesFileTransactionsRepository invoicesFileTransactionsRepository;
    private final InvoiceTransactionsRepository invoiceTransactionsRepository;
    private final InvoiceRepository invoiceRepository;
    private final ObjectMapper objectMapper;
    private final SqsAsyncClient sqsAsyncClient;
    private final String invoiceEventsQueueUrl;

    private final ReceiveMessageRequest receiveMessageRequest;

    @Autowired
    public S3InvoicesConsumer(S3AsyncClient s3AsyncClient,
                              InvoicesFileTransactionsRepository invoicesFileTransactionsRepository,
                              InvoiceTransactionsRepository invoiceTransactionsRepository,
                              InvoiceRepository invoiceRepository,
                              ObjectMapper objectMapper,
                              SqsAsyncClient sqsAsyncClient,
                              @Value("${aws.sqs.queue.invoice.events.url}") String invoiceEventsQueueUrl) {
        this.s3AsyncClient = s3AsyncClient;
        this.invoicesFileTransactionsRepository = invoicesFileTransactionsRepository;
        this.invoiceTransactionsRepository = invoiceTransactionsRepository;
        this.invoiceRepository = invoiceRepository;
        this.objectMapper = objectMapper;
        this.sqsAsyncClient = sqsAsyncClient;
        this.invoiceEventsQueueUrl = invoiceEventsQueueUrl;
        this.receiveMessageRequest = ReceiveMessageRequest.builder()
                .maxNumberOfMessages(5)
                .queueUrl(invoiceEventsQueueUrl)
                .build();
    }

    @Scheduled(fixedDelay = 1000)
    public void receiveInvoiceEventsMessage() {
        List<Message> messages;
        while (!(messages = sqsAsyncClient.receiveMessage(receiveMessageRequest).join().messages()).isEmpty()) {
            AtomicBoolean allInvoicesProcessed = new AtomicBoolean(true);
            LOG.info("Reading messages: {}", messages.size());

            messages.parallelStream().forEach(message -> {
                S3EventNotification eventNotification;
                LOG.info("Parsing S3 event");
                PojoSerializer<S3EventNotification> s3EventNotificationPojoSerializer =
                        LambdaEventSerializers.serializerFor(S3EventNotification.class, ClassLoader.getSystemClassLoader());
                eventNotification = s3EventNotificationPojoSerializer.fromJson(message.body());
                if (eventNotification == null || eventNotification.getRecords().isEmpty()) {
                    LOG.error("Failed to parse S3 event notification");
                    deleteMessage(message);
                    return;
                }
                LOG.info("Number of records: {}", eventNotification.getRecords().size());

                List<CompletableFuture<Boolean>> futures = new ArrayList<>();
                eventNotification.getRecords().parallelStream().forEach(s3EventNotificationRecord -> {
                    String key = s3EventNotificationRecord.getS3().getObject().getKey();
                    ThreadContext.put("InvoiceFileTransactionId", key);
                    LOG.info("Invoice file transactionId: {}", key);

                    futures.add(this.processRecord(s3EventNotificationRecord));

                    ThreadContext.clearAll();
                });

                CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
                allInvoicesProcessed.set(futures.stream().allMatch(CompletableFuture::join));

                LOG.info("All records were processed...");

                deleteMessage(message);

                if (allInvoicesProcessed.get()) {
                    LOG.info("All invoices processed");
                } else {
                    LOG.info("Some invoice file was not treated.");
                }
                LOG.info("Finish...");
            });
        }
    }

    private CompletableFuture<Boolean> processRecord(S3EventNotification.S3EventNotificationRecord record) {
        String key = record.getS3().getObject().getKey();
        LOG.info("Start processing the record - key: {}", key);
        String bucketName = record.getS3().getBucket().getName();

        //Get the invoice file transaction
        InvoiceFileTransaction invoiceFileTransaction = invoicesFileTransactionsRepository.getInvoiceFileTransaction(key).join();
        if (invoiceFileTransaction == null ||
                (!invoiceFileTransaction.getFileTransactionStatus().name().equals(InvoiceFileTransactionStatus.GENERATED.name()))) {
            LOG.error("Invoice file transaction not found or non valid transaction status - key: {}", key);
            return CompletableFuture.supplyAsync(() -> false);
        }

        //Get the S3 object (invoice file)
        LOG.info("Reading invoice file from S3 bucket...");
        ResponseInputStream<GetObjectResponse> s3Object = this.s3AsyncClient.getObject(GetObjectRequest.builder()
                .key(key)
                .bucket(bucketName)
                .build(), AsyncResponseTransformer.toBlockingInputStream()).join();

        //Update the invoice file transaction status
        invoicesFileTransactionsRepository.updateInvoiceFileTransactionStatus(key,
                InvoiceFileTransactionStatus.FILE_RECEIVED).join();

        int invoiceCount = 0;
        try (s3Object; BufferedReader reader = new BufferedReader(new InputStreamReader(s3Object))) {
            //Process each invoice from the file(also update the invoice transaction status)
            String line;
            while ((line = reader.readLine()) != null) {
                processInvoice(line, invoiceFileTransaction).join();
                LOG.info("Invoice processed...");
                invoiceCount++;
            }
            //Delete the object from the S3 bucket
            LOG.info("Deleting the invoice file from S3...");
            CompletableFuture<DeleteObjectResponse> deleteObjectResponseCompletableFuture =
                    this.s3AsyncClient.deleteObject(DeleteObjectRequest.builder()
                            .bucket(bucketName)
                            .key(key)
                            .build());
            //Update the invoice file transaction status
            CompletableFuture<InvoiceFileTransaction> invoiceFileTransactionCompletableFuture =
                    this.invoicesFileTransactionsRepository
                            .updateInvoiceFileTransactionStatus(key, InvoiceFileTransactionStatus.FILE_PROCESSED);

            CompletableFuture.allOf(deleteObjectResponseCompletableFuture, invoiceFileTransactionCompletableFuture).join();
        } catch (IOException e) {
            LOG.error("Failed to read invoice file");
            invoicesFileTransactionsRepository.updateInvoiceFileTransactionStatus(key,
                    InvoiceFileTransactionStatus.ERROR).join();
            return CompletableFuture.supplyAsync(() -> false);
        }
        LOG.info("Number of invoices processed: {}", invoiceCount);
        return CompletableFuture.supplyAsync(() -> true);
    }

    private CompletableFuture<Boolean> processInvoice(String line, InvoiceFileTransaction invoiceFileTransaction)
            throws JsonProcessingException {
        InvoiceTransactionStatus invoiceTransactionStatus;
        String invoiceFileTransactionId = invoiceFileTransaction.getSk();
        LOG.info("Persisting the invoice...");

        String invoiceTransactionId = UUID.randomUUID().toString();
        InvoiceFileDto invoiceFileDto = objectMapper.readValue(line, InvoiceFileDto.class);
        if (invoiceFileDto.products().isEmpty()) {
            LOG.error("Invoice import failed - empty products list");
            invoiceTransactionStatus = InvoiceTransactionStatus.EMPTY_PRODUCTS_LIST;
        } else {
            invoiceTransactionStatus = InvoiceTransactionStatus.OK;
        }
        CompletableFuture<Void> createInvoiceFuture =
                invoiceRepository.createInvoice(invoiceFileDto, invoiceTransactionId, invoiceFileTransactionId);
        CompletableFuture<Void> createInvoiceTransactionFuture =
                invoiceTransactionsRepository.createInvoiceTransaction(invoiceFileDto.customerEmail(),
                        invoiceFileDto.invoiceNumber(),
                        invoiceTransactionId,
                        invoiceFileTransactionId,
                        invoiceTransactionStatus);
        CompletableFuture.allOf(createInvoiceFuture, createInvoiceTransactionFuture).join();
        LOG.info("Invoice persisted");

        return CompletableFuture.supplyAsync(() -> true);
    }

    private void deleteMessage(Message message) {
        sqsAsyncClient.deleteMessage(DeleteMessageRequest.builder()
                .queueUrl(invoiceEventsQueueUrl)
                .receiptHandle(message.receiptHandle())
                .build()).join();
        LOG.info("Message deleted...");
    }
}
