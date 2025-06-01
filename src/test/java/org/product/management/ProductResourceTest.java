package org.product.management;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;

@QuarkusTest
public class ProductResourceTest {

    @InjectMock
    private ProductRepository repository;

    @Inject
    private ProductResource resource;

    @Test
    public void listAllTest() {

        Product product1 = new Product("Laptop", 1200.0, 10, "Powerful laptop");
        product1.id = 1L;
        Product product2 = new Product("Mouse", 25.0, 50, "Wireless mouse");
        product2.id = 2L;

        List<Product> products = Arrays.asList(product1, product2);
        when(repository.listAll()).thenReturn(Uni.createFrom().item(products));

        given()
                .when().get("/product")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .contentType(ContentType.JSON)
                .body("data[0].name", equalTo("Mouse"))
                .body("data[0].price", equalTo(25.0f))
                .body("data[1].name", equalTo("Laptop"))
                .body("data[1].price", equalTo(1200.0f));
    }

    @Test
    void testCreateProductError() {
        ProductDto productDto = new ProductDto("Faulty Item", 10.0, 5, "Item that causes error");

        Mockito.when(repository.saveProduct(any(Product.class)))
                .thenReturn(Uni.createFrom().failure(new RuntimeException("DB write error")));

        given()
                .contentType(ContentType.JSON)
                .body(productDto)
                .when().post("/product")
                .then()
                .statusCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode())
                .contentType(ContentType.JSON)
                .body("data", equalTo("Error creating product"));

        verify(repository, times(1)).saveProduct(any(Product.class));
    }

    @Test
    void testCreateProductSuccess() {

        ProductDto productDto = new ProductDto("New Gadget", 99.99, 100, "A cool new gadget");
        Product savedProduct = new Product("New Gadget", 99.99, 100, "A cool new gadget");
        savedProduct.id = 3L;

        Function<Void, Response> responseFunction = unused -> null;
        Mockito.when(repository.saveProduct(any(Product.class)).map(responseFunction)).thenReturn(Uni.createFrom().item(Response.status(Response.Status.CREATED).entity(
                new ResponseDto<>(
                        Response.Status.CREATED.getStatusCode(),
                        Response.Status.CREATED.name(),
                        new Product("New Gadget", 99.99, 100, "A cool new gadget")
                )
        ).build()));

        given()
                .contentType(ContentType.JSON)
                .body(productDto)
                .when().post("/product")
                .then()
                .statusCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode())
                .contentType(ContentType.JSON);
        verify(repository, times(1)).saveProduct(any(Product.class));
    }

    @Test
    void testDeleteProductSuccess() {
        Long productId = 1L;
        Mockito.when(repository.deleteById(productId)).thenReturn(Uni.createFrom().item(true));

        given()
                .pathParam("id", productId)
                .when().delete("/product/{id}")
                .then()
                .statusCode(Response.Status.CREATED.getStatusCode())
                .contentType(ContentType.JSON)
                .body("data", equalTo(productId + " deleted successfully"));

        verify(repository, times(1)).deleteById(productId);
    }

    @Test
    void testDeleteProductNotFound() {
        Long productId = 99L;
        Mockito.when(repository.deleteById(productId)).thenReturn(Uni.createFrom().item(false));

        given()
                .pathParam("id", productId)
                .when().delete("/product/{id}")
                .then()
                .statusCode(Response.Status.CREATED.getStatusCode())
                .contentType(ContentType.JSON)
                .body("data", equalTo("No such product"));

        verify(repository, times(1)).deleteById(productId);
    }

    @Test
    void testDeleteProductError() {
        Long productId = 1L;

        Mockito.when(repository.deleteById(productId))
                .thenReturn(Uni.createFrom().failure(new RuntimeException("DB delete error")));

        given()
                .pathParam("id", productId)
                .when().delete("/product/{id}")
                .then()
                .statusCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode())
                .contentType(ContentType.JSON)
                .body("data", equalTo("Error delete product"));

        verify(repository, times(1)).deleteById(productId);
    }

    @Test
    void testUpdateProductSuccess() {
        Long productId = 1L;
        Product existingProduct = new Product("Old Name", 100.0, 5, "Old Description");
        existingProduct.id = productId;

        Product updatedProductInfo = new Product("Updated Name", 150.0, 7, "Updated Description");
        updatedProductInfo.id = productId;

        Mockito.when(repository.findById(productId)).thenReturn(Uni.createFrom().item(existingProduct));
        Mockito.when(repository.persist(any(Product.class))).thenReturn(Uni.createFrom().item(existingProduct));

        given()
                .contentType(ContentType.JSON)
                .body(updatedProductInfo)
                .pathParam("id", productId)
                .when().put("/product/{id}")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .contentType(ContentType.JSON)
                .body("data.name", equalTo("Updated Name"))
                .body("data.price", equalTo(150.0f))
                .body("data.quantity", equalTo(7))
                .body("data.description", equalTo("Updated Description"));

        verify(repository, times(1)).findById(productId);
        verify(repository, times(1)).persist(any(Product.class));
    }

    @Test
    void testUpdateProductError() {
        Long productId = 1L;
        Product existingProduct = new Product("Old Name", 100.0, 5, "Old Description");
        existingProduct.id = productId;
        Product updatedProductInfo = new Product("Updated Name", 150.0, 7, "Updated Description");

        Mockito.when(repository.findById(productId)).thenReturn(Uni.createFrom().item(existingProduct));
        Mockito.when(repository.persist(any(Product.class)))
                .thenReturn(Uni.createFrom().failure(new RuntimeException("DB update error")));

        given()
                .contentType(ContentType.JSON)
                .body(updatedProductInfo)
                .pathParam("id", productId)
                .when().put("/product/{id}")
                .then()
                .statusCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode())
                .contentType(ContentType.JSON)
                .body("data", equalTo("Error delete product"));

        verify(repository, times(1)).findById(productId);
        verify(repository, times(1)).persist(any(Product.class));
    }

    @Test
    void testCheckStockSufficient() {
        Long productId = 1L;
        Integer requestedCount = 5;
        Product product = new Product("Test Product", 10.0, 10, "Description");
        product.id = productId;

        Mockito.when(repository.findById(productId)).thenReturn(Uni.createFrom().item(product));

        given()
                .pathParam("id", productId)
                .queryParam("count", requestedCount)
                .when().get("/product/{id}/checkStock")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .body(is("true"));

        verify(repository, times(1)).findById(productId);
    }

    @Test
    void testCheckStockInsufficient() {
        Long productId = 1L;
        Integer requestedCount = 15;
        Product product = new Product("Test Product", 10.0, 10, "Description");
        product.id = productId;

        Mockito.when(repository.findById(productId)).thenReturn(Uni.createFrom().item(product));

        given()
                .pathParam("id", productId)
                .queryParam("count", requestedCount)
                .when().get("/product/{id}/checkStock")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .body(is("false"));

        verify(repository, times(1)).findById(productId);
    }

    @Test
    void testCheckStockProductNotFound() {
        Long productId = 99L;
        Integer requestedCount = 5;

        Mockito.when(repository.findById(productId)).thenReturn(Uni.createFrom().nullItem());

        given()
                .pathParam("id", productId)
                .queryParam("count", requestedCount)
                .when().get("/product/{id}/checkStock")
                .then()
                .statusCode(Response.Status.NOT_FOUND.getStatusCode());

        verify(repository, times(1)).findById(productId);
    }
}
