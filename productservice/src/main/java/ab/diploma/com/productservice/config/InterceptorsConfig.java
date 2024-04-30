package ab.diploma.com.productservice.config;

import ab.diploma.com.productservice.products.interceptors.ProductsInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class InterceptorsConfig implements WebMvcConfigurer {

    private final ProductsInterceptor productsInterceptor;

    public InterceptorsConfig(ProductsInterceptor productsInterceptor) {
        this.productsInterceptor = productsInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(this.productsInterceptor)
                .addPathPatterns("/api/products/**");
    }
}
