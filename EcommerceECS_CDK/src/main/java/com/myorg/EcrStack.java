package com.myorg;

import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.ecr.Repository;
import software.amazon.awscdk.services.ecr.RepositoryProps;
import software.amazon.awscdk.services.ecr.TagMutability;
import software.constructs.Construct;


public class EcrStack extends Stack {
    private final  Repository productsServiceRepository;
    private final  Repository auditServiceRepository;
    private final  Repository invoicesServiceRepository;
    public  EcrStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        this.productsServiceRepository = new Repository(this, "ProductsService", RepositoryProps.builder()
                .repositoryName("productsservice")
                .removalPolicy(RemovalPolicy.DESTROY)
                .imageTagMutability(TagMutability.IMMUTABLE)
                .emptyOnDelete(true)
                .build());

        this.auditServiceRepository = new Repository(this, "AuditService", RepositoryProps.builder()
                .repositoryName("auditservice")
                .removalPolicy(RemovalPolicy.DESTROY)
                .imageTagMutability(TagMutability.IMMUTABLE)
                .emptyOnDelete(true)
                .build());

        this.invoicesServiceRepository = new Repository(this, "InvoicesService", RepositoryProps.builder()
                .repositoryName("invoicesservice")
                .removalPolicy(RemovalPolicy.DESTROY)
                .imageTagMutability(TagMutability.IMMUTABLE)
                .emptyOnDelete(true)
                .build());
    }

    public Repository getProductsServiceRepository() {
        return productsServiceRepository;
    }

    public Repository getAuditServiceRepository() {
        return auditServiceRepository;
    }

    public Repository getInvoicesServiceRepository() {
        return invoicesServiceRepository;
    }
}
