package ab.diploma.com.productservice.products.dto;

import ab.diploma.com.productservice.products.models.Product;
import com.fasterxml.jackson.annotation.JsonInclude;

public record ProductDto(
        String id, String name, String code, float price, String model,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String url
) {
    public ProductDto(Product product) {
        this(product.getId(), product.getProductName(), product.getCode(), product.getPrice(), product.getModel(),
                product.getProductUrl());
    }

    static public Product toProduct(ProductDto productDto) {
        Product product = new Product();
        product.setId(productDto.id());
        product.setProductName(productDto.name());
        product.setCode(productDto.code());
        product.setModel(productDto.model());
        product.setPrice(productDto.price());
        product.setProductUrl(productDto.url());
        return product;
    }
}
