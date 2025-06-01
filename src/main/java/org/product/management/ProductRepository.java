package org.product.management;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ProductRepository implements PanacheRepository<Product> {

    public Uni<Void> saveProduct(Product product) {
        return persistAndFlush(product).replaceWithVoid();
    }
}
