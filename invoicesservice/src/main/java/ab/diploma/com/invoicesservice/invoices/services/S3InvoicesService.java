package ab.diploma.com.invoicesservice.invoices.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;

@Service
public class S3InvoicesService {

    private final S3Presigner s3Presigner;
    private final String bucketName;

    @Autowired
    public S3InvoicesService(S3Presigner s3Presigner,
                             @Value("${invoices.bucket.name}") String bucketName) {
        this.s3Presigner = s3Presigner;
        this.bucketName = bucketName;
    }

    public String generatePreSignedUrl(String key, int expiresIn) {
        return s3Presigner.presignPutObject(PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(expiresIn))
                        .putObjectRequest(p -> p
                                .bucket(bucketName)
                                .key(key)
                                .build())
                .build()).url().toString();

    }
}
