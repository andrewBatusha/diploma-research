package ab.diploma.com.auditservice.products;

import ab.diploma.com.auditservice.products.dto.ProductEventApiDto;
import ab.diploma.com.auditservice.products.dto.ProductEventApiPageDto;
import ab.diploma.com.auditservice.products.models.ProductEvent;
import ab.diploma.com.auditservice.products.repositories.ProductEventsRepository;
import com.amazonaws.xray.spring.aop.XRayEnabled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@RestController
@RequestMapping("/api/products/events")
@XRayEnabled
public class ProductEventsController {

    private final ProductEventsRepository productEventsRepository;

    @Autowired
    public ProductEventsController(ProductEventsRepository productEventsRepository) {
        this.productEventsRepository = productEventsRepository;
    }

    @GetMapping
    public ResponseEntity<ProductEventApiPageDto> getAll(
            @RequestParam String eventType,
            @RequestParam(defaultValue = "5") int limit,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) String exclusiveStartTimestamp) {
        List<ProductEventApiDto> productEventApiDtoList = new ArrayList<>();
        SdkPublisher<Page<ProductEvent>> productEventsPublisher =
                (from != null && to != null)
                ? productEventsRepository.findByTypeAndRange(eventType, exclusiveStartTimestamp, from, to, limit)
                : productEventsRepository.findByType(eventType, exclusiveStartTimestamp, limit);

        AtomicReference<String> lastEvaluatedTimestamp = new AtomicReference<>();
        productEventsPublisher.subscribe(productEventPage -> {
            productEventApiDtoList.addAll(productEventPage.items().stream().map(ProductEventApiDto::new).toList());
            if (productEventPage.lastEvaluatedKey() != null) {
                lastEvaluatedTimestamp.set(productEventPage.lastEvaluatedKey().get("sk").s());
            }
        }).join();
        return new ResponseEntity<>(
                new ProductEventApiPageDto(productEventApiDtoList,
                        lastEvaluatedTimestamp.get(),
                        productEventApiDtoList.size()), HttpStatus.OK
        );
    }
}
