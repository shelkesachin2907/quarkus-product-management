package org.product.management;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import java.util.Comparator;
import java.util.Objects;
import java.util.stream.Collectors;

@Path("/product")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProductResource {

    private static final Logger LOGGER = Logger.getLogger(ProductResource.class);

    @Inject
    private ProductRepository repository;

    @GET
    public Uni<Response> listAll() {
        return repository.listAll()
                .map(
                        v -> Response.status(Response.Status.OK).entity(
                                new ResponseDto<>(
                                        Response.Status.OK.getStatusCode(),
                                        Response.Status.OK.name(),
                                        v.stream()
                                                .sorted(Comparator.comparing(Product::getPrice))
                                                .collect(Collectors.toList())
                                )
                        ).build()
                )
                .onFailure().recoverWithItem(throwable -> {
                    LOGGER.error("Error while fetching products: {}" + throwable.getMessage());
                    return createFailedResponse("Error while fetching products");
                });

    }

    @POST
    @WithTransaction
    public Uni<Response> create(ProductDto productInfo) {

        LOGGER.info("New product info: " + productInfo.toString());

        Product product = new Product(productInfo.getName(), productInfo.getPrice(), productInfo.getQuantity(), productInfo.getDescription());
        return repository.saveProduct(product)
                .map(
                        v -> Response.status(Response.Status.CREATED).entity(
                                new ResponseDto<Product>(
                                        Response.Status.CREATED.getStatusCode(),
                                        Response.Status.CREATED.name(),
                                        product
                                )
                        ).build()
                )
                .onFailure().recoverWithItem(throwable -> {
                    LOGGER.error("Failed to create product: {}" + throwable.getMessage());
                    return createFailedResponse("Error creating product");
                });
    }

    @DELETE()
    @Path("/{id}")
    @WithTransaction
    public Uni<Response> delete(@PathParam("id") Long id) {
        return repository.deleteById(id).map(
                v -> Response.status(Response.Status.CREATED).entity(
                        new ResponseDto<String>(
                                Response.Status.CREATED.getStatusCode(),
                                Response.Status.CREATED.name(),
                                v ? id + " deleted successfully": "No such product"
                        )
                ).build()
        ).onFailure().recoverWithItem(throwable -> {
            LOGGER.error("Failed to delete product: {}" + throwable.getMessage());
            return createFailedResponse("Error delete product");
        });
    }

    @PUT
    @Path("/{id}")
    @WithTransaction
    public Uni<Response> update(@PathParam("id") Long id, Product updatedProduct) {
        Uni<Product> productUni = repository.findById(id)
                .onItem().ifNotNull()
                .invoke(existing -> {
                    if(!updatedProduct.getName().isEmpty()) {
                        existing.setName(updatedProduct.getName());
                    }

                    if(!updatedProduct.getDescription().isEmpty()) {
                        existing.setDescription(updatedProduct.getDescription());
                    }

                    if(!Objects.equals(updatedProduct.getPrice(), existing.getPrice())) {
                        existing.setPrice(updatedProduct.getPrice());
                    }

                    if(!Objects.equals(updatedProduct.getQuantity(), existing.getQuantity())) {
                        existing.setQuantity(updatedProduct.getQuantity());
                    }
                })
                .call(existing -> repository.persist(existing));

        return productUni.map(
                v -> Response.status(Response.Status.OK).entity(
                        new ResponseDto<Product>(
                                Response.Status.OK.getStatusCode(),
                                Response.Status.OK.name(), v
                        )
                ).build()
        ).onFailure().recoverWithItem(throwable -> {
            LOGGER.error("Failed to create product: {}" + throwable.getMessage());
            return createFailedResponse("Error delete product");
        });

    }

    @GET
    @Path("/{id}/checkStock")
    public Uni<Boolean> checkStock(@PathParam("id") Long id, @QueryParam("count") Integer count) {
        return repository.findById(id)
                .onItem().ifNotNull().transform(existing -> existing.getQuantity() >= count)
                .onItem().ifNull().failWith(new WebApplicationException("Product not found", 404));
    }


    private Response createFailedResponse(String message) {
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new ResponseDto<String>(
                Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                Response.Status.INTERNAL_SERVER_ERROR.name(),
                message
        )).build();
    }
}
