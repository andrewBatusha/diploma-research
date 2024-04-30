package ab.diploma.com.productservice.products.repositories;

import ab.diploma.com.productservice.exceptions.ProductException;
import ab.diploma.com.productservice.products.controllers.ProductsController;
import ab.diploma.com.productservice.products.enums.ProductErrors;
import ab.diploma.com.productservice.products.models.Product;
import com.amazonaws.xray.spring.aop.XRayEnabled;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.PagePublisher;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Repository
@XRayEnabled
public class ProductsRepository {

    private static final Logger LOG = LogManager.getLogger(ProductsRepository.class);

    private final DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient;

    private final DynamoDbAsyncTable<Product> productsTable;

    public ProductsRepository(DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient,
                              @Value("${aws.productsddb.name}") String productsDdbName) {
        this.dynamoDbEnhancedAsyncClient = dynamoDbEnhancedAsyncClient;
        this.productsTable = dynamoDbEnhancedAsyncClient.table(productsDdbName, TableSchema.fromBean(Product.class));
    }

    private CompletableFuture<Product> checkIfCodeExists(String code) {
        List<Product> products = new ArrayList<>();
        productsTable.index("codeIdx").query(QueryEnhancedRequest.builder()
                .limit(1)
                .queryConditional(QueryConditional.keyEqualTo(Key.builder()
                        .partitionValue(code).build()))
                .build()).subscribe(productPage -> products.addAll(productPage.items())).join();
        if (!products.isEmpty()) {
            return CompletableFuture.supplyAsync(products::getFirst);
        } else {
            return CompletableFuture.supplyAsync(() -> null);
        }
    }

    public CompletableFuture<Product> getByCode(String code) {
        Product productByCode = checkIfCodeExists(code).join();
        if (productByCode != null) {
            return getById(productByCode.getId());
        } else {
            return CompletableFuture.supplyAsync(() -> null);
        }
    }

    public PagePublisher<Product> getAll() {
        return productsTable.scan();
    }

    public CompletableFuture<Product> getById(String productId) {
        LOG.info("ProductId: {}", productId);
        return productsTable.getItem(Key.builder()
                .partitionValue(productId)
                .build());
    }

    public CompletableFuture<Void> create(Product product) throws ProductException {
        Product productWithSameCode = checkIfCodeExists(product.getCode()).join();
        if (productWithSameCode != null) {
            LOG.error("Can not create a product with same code");
            throw new ProductException(ProductErrors.PRODUCT_CODE_ALREADY_EXISTS, productWithSameCode.getId());
        }
        return productsTable.putItem(product);
    }

    public CompletableFuture<Product> deleteById(String productId) {
        return productsTable.deleteItem(Key.builder()
                .partitionValue(productId)
                .build());
    }

    public CompletableFuture<Product> update(Product product, String productId) throws ProductException {
        product.setId(productId);
        Product productWithSameCode = checkIfCodeExists(product.getCode()).join();
        if (productWithSameCode != null && !productWithSameCode.getId().equals(product.getId())) {
            throw new ProductException(ProductErrors.PRODUCT_CODE_ALREADY_EXISTS, productWithSameCode.getId());
        }
        return productsTable.updateItem(
                UpdateItemEnhancedRequest.builder(Product.class)
                        .item(product)
                        .conditionExpression(Expression.builder()
                                .expression("attribute_exists(id)")
                                .build())
                        .build());
    }
}
