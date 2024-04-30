package ab.diploma.com.productservice.products.controllers;

import ab.diploma.com.productservice.events.dto.EventType;
import ab.diploma.com.productservice.events.service.EventsPublisher;
import ab.diploma.com.productservice.exceptions.ProductException;
import ab.diploma.com.productservice.products.dto.ProductDto;
import ab.diploma.com.productservice.products.enums.ProductErrors;
import ab.diploma.com.productservice.products.models.Product;
import ab.diploma.com.productservice.products.repositories.ProductsRepository;
import com.amazonaws.xray.spring.aop.XRayEnabled;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import software.amazon.awssdk.services.sns.model.PublishResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/api/products")
@XRayEnabled
public class ProductsController {
    private static final Logger LOG = LogManager.getLogger(ProductsController.class);

    private final ProductsRepository productsRepository;

    private final EventsPublisher eventsPublisher;

    @Autowired
    public ProductsController(ProductsRepository productsRepository, EventsPublisher eventsPublisher) {
        this.productsRepository = productsRepository;
        this.eventsPublisher = eventsPublisher;
    }

    @GetMapping
    public ResponseEntity<?> getAllProducts(@RequestParam(required = false) String code) throws ProductException {
        if (code != null) {
            LOG.info("Get product by code: {}", code);
            Product productByCode = productsRepository.getByCode(code).join();
            if (productByCode != null) {
                return new ResponseEntity<>(new ProductDto(productByCode), HttpStatus.OK);
            } else {
                throw new ProductException(ProductErrors.PRODUCT_NOT_FOUND, null);
            }
        } else {
            LOG.info("Get all products");
            List<ProductDto> productsDto = new ArrayList<>();

            productsRepository.getAll().items().subscribe(product -> productsDto.add(new ProductDto(product))).join();
            return ResponseEntity.ok(productsDto);
        }
    }

    @GetMapping("{id}")
    public ResponseEntity<ProductDto> getProductById(@PathVariable("id") String id) throws ProductException {
        Product product = productsRepository.getById(id).join();
        if (product != null) {
            LOG.info("Get product by its id {}", id);
            return ResponseEntity.ok(new ProductDto(product));
        } else {
            throw new ProductException(ProductErrors.PRODUCT_NOT_FOUND, id);
        }
    }

    @PostMapping
    public ResponseEntity<ProductDto> createProduct(@RequestBody ProductDto productDto)
            throws ProductException, JsonProcessingException, ExecutionException, InterruptedException {
        Product productCreated = ProductDto.toProduct(productDto);
        productCreated.setId(UUID.randomUUID().toString());
        CompletableFuture<Void> productCompletableFeature = productsRepository.create(productCreated);

        CompletableFuture<PublishResponse> publishResponseCompletableFuture = eventsPublisher.sendProductEvent(productCreated,
                EventType.PRODUCT_CREATED, "denys.batuiev@gmail.com");

        CompletableFuture.allOf(productCompletableFeature, publishResponseCompletableFuture).join();
        PublishResponse publishResponse = publishResponseCompletableFuture.get();

        ThreadContext.put("messageId", publishResponse.messageId());

        LOG.info("Product created - ID: {}", productCreated.getId());
        return new ResponseEntity<>(new ProductDto(productCreated), HttpStatus.CREATED);
    }

    @DeleteMapping("{id}")
    public ResponseEntity<ProductDto> deleteProductById(@PathVariable("id") String id) throws ProductException,
            JsonProcessingException {
        Product productDeleted = productsRepository.deleteById(id).join();
        if (productDeleted != null) {
            PublishResponse publishResponse = eventsPublisher.sendProductEvent(productDeleted,
                    EventType.PRODUCT_DELETED, "andrew.batuiev@gmail.com").join();
            ThreadContext.put("messageId", publishResponse.messageId());

            LOG.info("Product deleted - ID: {}", productDeleted.getId());
            return ResponseEntity.ok(new ProductDto(productDeleted));
        } else {
            throw new ProductException(ProductErrors.PRODUCT_NOT_FOUND, id);
        }
    }

    @PutMapping("{id}")
    public ResponseEntity<ProductDto> updateProduct(@RequestBody ProductDto productDto, @PathVariable("id") String id)
            throws ProductException, JsonProcessingException {
        try {
            Product productUpdated = productsRepository.update(ProductDto.toProduct(productDto), id).join();

            PublishResponse publishResponse = eventsPublisher.sendProductEvent(productUpdated,
                    EventType.PRODUCT_UPDATED, "dmytro.batuiev@gmail.com").join();
            ThreadContext.put("messageId", publishResponse.messageId());

            LOG.info("Product updated - ID: {}", productUpdated.getId());
            return ResponseEntity.ok(new ProductDto(productUpdated));
        } catch (CompletionException e) {
            throw new ProductException(ProductErrors.PRODUCT_NOT_FOUND, id);
        }
    }
}
