package com.myorg;

import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.apigateway.*;
import software.amazon.awscdk.services.elasticloadbalancingv2.NetworkLoadBalancer;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.logs.LogGroupProps;
import software.constructs.Construct;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ApiStack extends Stack {
    public ApiStack(final Construct scope, final String id,
                    final StackProps props, ApiStackProps apiStackProps) {
        super(scope, id, props);

        LogGroup logGroup = new LogGroup(this, "ECommerceApiLogs", LogGroupProps.builder()
                .logGroupName("ECommerce")
                .build());
        RestApi restApi = new RestApi(this, "RestApi",
                RestApiProps.builder()
                        .restApiName("ECommerceAPI")
                        .cloudWatchRole(true)
                        .deployOptions(StageOptions.builder()
                                .loggingLevel(MethodLoggingLevel.INFO)
                                .accessLogDestination(new LogGroupLogDestination(logGroup))
                                .accessLogFormat(
                                        AccessLogFormat.jsonWithStandardFields(
                                                JsonWithStandardFieldProps.builder()
                                                        .caller(true)
                                                        .httpMethod(true)
                                                        .ip(true)
                                                        .protocol(true)
                                                        .requestTime(true)
                                                        .resourcePath(true)
                                                        .responseLength(true)
                                                        .status(true)
                                                        .user(true)
                                                        .build()
                                        )
                                )
                                .build())
                        .build());

        Resource productsResourse = this.createProductsResource(restApi, apiStackProps);

        this.createProductEventsResource(restApi, apiStackProps, productsResourse);

        this.createInvoicesResource(restApi, apiStackProps);
    }

    private void createInvoicesResource(RestApi restApi, ApiStackProps apiStackProps) {
        // invoices
        Resource invoicesResource = restApi.getRoot().addResource("invoices");

        Map<String, String> invoicesIntegrationParameters = new HashMap<>();
        invoicesIntegrationParameters.put("integration.request.header.requestId", "context.requestId");

        Map<String, Boolean> invoicesMethodParameters = new HashMap<>();
        invoicesMethodParameters.put("method.request.header.requestId", false);

        // POST /invoices
        invoicesResource.addMethod("POST", new Integration(IntegrationProps.builder()
                .type(IntegrationType.HTTP_PROXY)
                .integrationHttpMethod("POST")
                .uri("http://" + apiStackProps.networkLoadBalancer().getLoadBalancerDnsName() +
                        ":9095/api/invoices")
                .options(IntegrationOptions.builder()
                        .vpcLink(apiStackProps.vpcLink())
                        .connectionType(ConnectionType.VPC_LINK)
                        .requestParameters(invoicesIntegrationParameters)
                        .build())
                .build()), MethodOptions.builder()
                .requestValidator(new RequestValidator(this, "InvoicesValidator", RequestValidatorProps.builder()
                        .restApi(restApi)
                        .requestValidatorName("InvoicesValidator")
                        .validateRequestParameters(true)
                        .build()))
                .requestParameters(invoicesMethodParameters)
                .build());

        Map<String, String> invoicesFileTransactionIntegrationParameters = new HashMap<>();
        invoicesFileTransactionIntegrationParameters.put("integration.request.path.fileTransactionId",
                "method.request.path.fileTransactionId");
        invoicesFileTransactionIntegrationParameters.put("integration.request.header.requestId", "context.requestId");

        Map<String, Boolean> invoicesFileTransactionMethodParameters = new HashMap<>();
        invoicesFileTransactionMethodParameters.put("method.request.path.fileTransactionId", true);
        invoicesFileTransactionMethodParameters.put("method.request.header.requestId", false);

        // GET /invoices/transactions/{fileTransactionId}
        Resource invoiceTransactionsResource = invoicesResource.addResource("transactions");
        Resource invoiceFileTransactionsResource = invoiceTransactionsResource.addResource("{fileTransactionId}");

        invoiceFileTransactionsResource.addMethod("GET", new Integration(IntegrationProps.builder()
                .type(IntegrationType.HTTP_PROXY)
                .integrationHttpMethod("GET")
                .uri("http://" + apiStackProps.networkLoadBalancer().getLoadBalancerDnsName() +
                        ":9095/api/invoices/transactions/{fileTransactionId}")
                .options(IntegrationOptions.builder()
                        .vpcLink(apiStackProps.vpcLink())
                        .connectionType(ConnectionType.VPC_LINK)
                        .requestParameters(invoicesFileTransactionIntegrationParameters)
                        .build())
                .build()), MethodOptions.builder()
                .requestValidator(new RequestValidator(this, "InvoiceTransactionsValidator",
                        RequestValidatorProps.builder()
                                .restApi(restApi)
                                .requestValidatorName("InvoiceTransactionsValidator")
                                .validateRequestParameters(true)
                                .build()))
                .requestParameters(invoicesFileTransactionMethodParameters)
                .build());

        // GET /invoices?email=andrii.batuiev@gmail.com
        Map<String, Boolean> customerInvoicesMethodParameters = new HashMap<>();
        customerInvoicesMethodParameters.put("method.request.header.requestId", false);
        customerInvoicesMethodParameters.put("method.request.querystring.email", true);

        invoicesResource.addMethod("GET", new Integration(IntegrationProps.builder()
                .type(IntegrationType.HTTP_PROXY)
                .integrationHttpMethod("GET")
                .uri("http://" + apiStackProps.networkLoadBalancer().getLoadBalancerDnsName() +
                        ":9095/api/invoices")
                .options(IntegrationOptions.builder()
                        .vpcLink(apiStackProps.vpcLink())
                        .connectionType(ConnectionType.VPC_LINK)
                        .requestParameters(invoicesIntegrationParameters)
                        .build())
                .build()), MethodOptions.builder()
                .requestValidator(new RequestValidator(this, "CustomerInvoicesValidator", RequestValidatorProps.builder()
                        .restApi(restApi)
                        .requestValidatorName("CustomerInvoicesValidator")
                        .validateRequestParameters(true)
                        .build()))
                .requestParameters(customerInvoicesMethodParameters)
                .build());
    }

    private void createProductEventsResource(RestApi restApi, ApiStackProps apiStackProps, Resource productsResource) {
        // /products/events
        Resource productEventsResource = productsResource.addResource("events");

        Map<String, String> productEventsIntegrationParameters = new HashMap<>();
        productEventsIntegrationParameters.put("integration.request.header.requestId", "context.requestId");

        Map<String, Boolean> productEventsMethodParameters = new HashMap<>();
        productEventsMethodParameters.put("method.request.header.requestId", false);
        productEventsMethodParameters.put("method.request.querystring.eventType", true);
        productEventsMethodParameters.put("method.request.querystring.limit", false);
        productEventsMethodParameters.put("method.request.querystring.from", false);
        productEventsMethodParameters.put("method.request.querystring.to", false);
        productEventsMethodParameters.put("method.request.querystring.exclusiveStartTimestamp", false);

        // GET /products/events?eventType=PRODUCT_CREATED&limit=10&from=1&to=5&exclusiveStartTimestamp=123
        productEventsResource.addMethod("GET", new Integration(
                        IntegrationProps.builder()
                                .type(IntegrationType.HTTP_PROXY)
                                .integrationHttpMethod("GET")
                                .uri("http://" + apiStackProps.networkLoadBalancer().getLoadBalancerDnsName() +
                                        ":9090/api/products/events")
                                .options(IntegrationOptions.builder()
                                        .vpcLink(apiStackProps.vpcLink())
                                        .connectionType(ConnectionType.VPC_LINK)
                                        .requestParameters(productEventsIntegrationParameters)
                                        .build())
                                .build()),
                MethodOptions.builder()
                        .requestValidator(new RequestValidator(this, "ProductEventsValidator",
                                RequestValidatorProps.builder()
                                        .restApi(restApi)
                                        .requestValidatorName("ProductEventsValidator")
                                        .validateRequestParameters(true)
                                        .build()))
                        .requestParameters(productEventsMethodParameters)
                        .build());

    }

    private Resource createProductsResource(RestApi restApi, ApiStackProps apiStackProps) {
        Map<String, String> productsIntegrationParameters = new HashMap<>();
        productsIntegrationParameters.put("integration.request.header.requestId", "context.requestId");

        Map<String, Boolean> productsMethodParameters = new HashMap<>();
        productsMethodParameters.put("method.request.header.requestId", false);
        productsMethodParameters.put("method.request.querystring.code", false);

        Resource productsResource = restApi.getRoot().addResource("products");

        // GET /products
        // GET /products?code=Code1
        productsResource.addMethod("GET", new Integration(
                        IntegrationProps.builder()
                                .type(IntegrationType.HTTP_PROXY)
                                .integrationHttpMethod("GET")
                                .uri("http://" + apiStackProps.networkLoadBalancer().getLoadBalancerDnsName() + ":8080/api/products")
                                .options(IntegrationOptions.builder()
                                        .vpcLink(apiStackProps.vpcLink())
                                        .connectionType(ConnectionType.VPC_LINK)
                                        .requestParameters(productsIntegrationParameters)
                                        .build())
                                .build()),
                MethodOptions.builder()
                        .requestParameters(productsMethodParameters)
                        .build());

        RequestValidator productRequestValidator = new RequestValidator(this, "ProductRequestValidator",
                RequestValidatorProps.builder()
                        .restApi(restApi)
                        .requestValidatorName("Product request validator")
                        .validateRequestBody(true).build());
        Map<String, JsonSchema> productModelProperties = new HashMap<>();
        productModelProperties.put("name", JsonSchema.builder()
                .type(JsonSchemaType.STRING)
                .minLength(5)
                .maxLength(50)
                .build());
        productModelProperties.put("code", JsonSchema.builder()
                .type(JsonSchemaType.STRING)
                .minLength(5)
                .maxLength(15)
                .build());
        productModelProperties.put("model", JsonSchema.builder()
                .type(JsonSchemaType.STRING)
                .minLength(5)
                .maxLength(50)
                .build());
        productModelProperties.put("price", JsonSchema.builder()
                .type(JsonSchemaType.NUMBER)
                .minimum(10.0)
                .maximum(1000.0)
                .build());

        Model productModel = new Model(this, "ProductModel",
                ModelProps.builder()
                        .modelName("ProductModel")
                        .restApi(restApi)
                        .contentType("application/json")
                        .schema(JsonSchema.builder()
                                .type(JsonSchemaType.OBJECT)
                                .properties(productModelProperties)
                                .required(Arrays.asList("name", "code"))
                                .build())
                        .build());

        Map<String, Model> productRequestModels = new HashMap<>();
        productRequestModels.put("application/json", productModel);

        // POST /products
        productsResource.addMethod("POST", new Integration(
                        IntegrationProps.builder()
                                .type(IntegrationType.HTTP_PROXY)
                                .integrationHttpMethod("POST")
                                .uri("http://" + apiStackProps.networkLoadBalancer().getLoadBalancerDnsName() + ":8080/api/products")
                                .options(IntegrationOptions.builder()
                                        .vpcLink(apiStackProps.vpcLink())
                                        .connectionType(ConnectionType.VPC_LINK)
                                        .requestParameters(productsIntegrationParameters)
                                        .build())
                                .build()),
                MethodOptions.builder()
                        .requestParameters(productsMethodParameters)
                        .requestValidator(productRequestValidator)
                        .requestModels(productRequestModels)
                        .build());

        // PUT /products/{id}
        Map<String, String> productIdIntegrationParameters = new HashMap<>();
        productIdIntegrationParameters.put("integration.request.path.id", "method.request.path.id");
        productIdIntegrationParameters.put("integration.request.header.requestId", "context.requestId");


        Map<String, Boolean> productIdMethodParameters = new HashMap<>();
        productIdMethodParameters.put("method.request.path.id", true);
        productIdMethodParameters.put("method.request.header.requestId", false);


        Resource productIdResource = productsResource.addResource("{id}");
        productIdResource.addMethod("PUT", new Integration(
                IntegrationProps.builder()
                        .type(IntegrationType.HTTP_PROXY)
                        .integrationHttpMethod("PUT")
                        .uri("http://" + apiStackProps.networkLoadBalancer().getLoadBalancerDnsName() + ":8080/api/products/{id}")
                        .options(IntegrationOptions.builder()
                                .vpcLink(apiStackProps.vpcLink())
                                .connectionType(ConnectionType.VPC_LINK)
                                .requestParameters(productIdIntegrationParameters)
                                .build())
                        .build()), MethodOptions.builder()
                .requestParameters(productIdMethodParameters)
                .requestValidator(productRequestValidator)
                .requestModels(productRequestModels)
                .build());

        // GET /products/{id}
        productIdResource.addMethod("GET", new Integration(
                IntegrationProps.builder()
                        .type(IntegrationType.HTTP_PROXY)
                        .integrationHttpMethod("GET")
                        .uri("http://" + apiStackProps.networkLoadBalancer().getLoadBalancerDnsName() + ":8080/api/products/{id}")
                        .options(IntegrationOptions.builder()
                                .vpcLink(apiStackProps.vpcLink())
                                .connectionType(ConnectionType.VPC_LINK)
                                .requestParameters(productIdIntegrationParameters)
                                .build())
                        .build()), MethodOptions.builder()
                .requestParameters(productIdMethodParameters)
                .build());

        // DELETE /products/{id}
        productIdResource.addMethod("DELETE", new Integration(
                IntegrationProps.builder()
                        .type(IntegrationType.HTTP_PROXY)
                        .integrationHttpMethod("DELETE")
                        .uri("http://" + apiStackProps.networkLoadBalancer().getLoadBalancerDnsName() + ":8080/api/products/{id}")
                        .options(IntegrationOptions.builder()
                                .vpcLink(apiStackProps.vpcLink())
                                .connectionType(ConnectionType.VPC_LINK)
                                .requestParameters(productIdIntegrationParameters)
                                .build())
                        .build()), MethodOptions.builder()
                .requestParameters(productIdMethodParameters)
                .build());

        return productsResource;
    }

}

record ApiStackProps(
        NetworkLoadBalancer networkLoadBalancer,
        VpcLink vpcLink
) {
}
