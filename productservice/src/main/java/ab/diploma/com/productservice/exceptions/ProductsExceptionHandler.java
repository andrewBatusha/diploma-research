package ab.diploma.com.productservice.exceptions;

import ab.diploma.com.productservice.events.dto.ProductFailureEventDto;
import ab.diploma.com.productservice.events.service.EventsPublisher;
import ab.diploma.com.productservice.products.dto.ProductErrorResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import software.amazon.awssdk.services.sns.model.PublishResponse;


@RestControllerAdvice
public class ProductsExceptionHandler extends ResponseEntityExceptionHandler {
    private static final Logger LOG = LogManager.getLogger(ProductsExceptionHandler.class);
    private final EventsPublisher eventsPublisher;

    @Autowired
    public ProductsExceptionHandler(EventsPublisher eventsPublisher) {
        this.eventsPublisher = eventsPublisher;
    }

    @ExceptionHandler(value = {ProductException.class})
    protected ResponseEntity<Object> handleProductError(ProductException productException, WebRequest webRequest)
            throws JsonProcessingException {
        ProductErrorResponse productErrorResponse = new ProductErrorResponse(
                productException.getProductErrors().getMessage(),
                productException.getProductErrors().getHttpStatus().value(),
                ThreadContext.get("requestId"),
                productException.getProductId()
        );

        ProductFailureEventDto productFailureEventDto = new ProductFailureEventDto(
                "sarrah.batuiev@gmail.com",
                productException.getProductErrors().getHttpStatus().value(),
                productException.getProductErrors().getMessage(),
                productException.getProductId()
        );

        PublishResponse publishResponse = eventsPublisher.sendProductFailureEvent(productFailureEventDto).join();
        ThreadContext.put("messageId", publishResponse.messageId());


        LOG.error(productException.getProductErrors().getMessage());

        return handleExceptionInternal(
                productException,
                productErrorResponse,
                new HttpHeaders(),
                productException.getProductErrors().getHttpStatus(),
                webRequest
        );
    }
}
