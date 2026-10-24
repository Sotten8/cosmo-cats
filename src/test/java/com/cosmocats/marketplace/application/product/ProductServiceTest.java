package com.cosmocats.marketplace.application.product;

import com.cosmocats.marketplace.domain.common.PageResult;
import com.cosmocats.marketplace.domain.exception.CategoryNotFoundException;
import com.cosmocats.marketplace.domain.exception.DuplicateProductNameException;
import com.cosmocats.marketplace.domain.exception.ProductNotFoundException;
import com.cosmocats.marketplace.domain.model.Product;
import com.cosmocats.marketplace.domain.repository.CategoryRepository;
import com.cosmocats.marketplace.domain.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.cosmocats.marketplace.support.TestData.FOOD;
import static com.cosmocats.marketplace.support.TestData.TOYS;
import static com.cosmocats.marketplace.support.TestData.gcr;
import static com.cosmocats.marketplace.support.TestData.product;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository products;
    @Mock
    private CategoryRepository categories;
    @InjectMocks
    private ProductService service;

    @Test
    void findAllDelegatesToRepository() {
        PageResult<Product> page = new PageResult<>(List.of(product()), 1, 5, 6);
        when(products.findAll(1, 5)).thenReturn(page);

        assertThat(service.findAll(1, 5)).isSameAs(page);
    }

    @Test
    void findByIdReturnsProduct() {
        Product existing = product();
        when(products.findById(existing.id())).thenReturn(Optional.of(existing));

        assertThat(service.findById(existing.id())).isEqualTo(existing);
    }

    @Test
    void findByIdThrowsWhenMissing() {
        UUID id = UUID.randomUUID();
        when(products.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(id))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining(id.toString());
    }

    @Test
    void createSavesProductWhenCategoryExistsAndNameIsFree() {
        Product newProduct = product();
        when(categories.existsById(newProduct.categoryId())).thenReturn(true);
        when(products.findByNameIgnoreCase(newProduct.name())).thenReturn(Optional.empty());
        when(products.save(newProduct)).thenReturn(newProduct);

        Product result = service.create(newProduct);

        assertThat(result).isEqualTo(newProduct);
        verify(products).save(newProduct);
    }

    @Test
    void createFailsWhenCategoryDoesNotExist() {
        Product newProduct = product();
        when(categories.existsById(newProduct.categoryId())).thenReturn(false);

        assertThatThrownBy(() -> service.create(newProduct))
                .isInstanceOf(CategoryNotFoundException.class)
                .hasMessageContaining(newProduct.categoryId().toString());
        verify(products, never()).save(any());
    }

    @Test
    void createFailsWhenNameIsTakenByAnotherProduct() {
        Product newProduct = product();
        Product other = product(UUID.randomUUID(), newProduct.name().toUpperCase());
        when(categories.existsById(newProduct.categoryId())).thenReturn(true);
        when(products.findByNameIgnoreCase(newProduct.name())).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> service.create(newProduct))
                .isInstanceOf(DuplicateProductNameException.class)
                .hasMessageContaining(newProduct.name());
        verify(products, never()).save(any());
    }

    @Test
    void updateKeepsIdAndCreatedAtButReplacesOtherFields() {
        Product existing = product();
        Product incoming = new Product(UUID.randomUUID(), "Cosmic Milk", "new description", gcr("7.50"),
                99, FOOD, "new@cosmo-cats.market", Instant.parse("2030-01-01T00:00:00Z"));
        when(products.findById(existing.id())).thenReturn(Optional.of(existing));
        when(categories.existsById(FOOD)).thenReturn(true);
        when(products.findByNameIgnoreCase("Cosmic Milk")).thenReturn(Optional.empty());
        when(products.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        Product result = service.update(existing.id(), incoming);

        ArgumentCaptor<Product> saved = ArgumentCaptor.forClass(Product.class);
        verify(products).save(saved.capture());
        assertThat(saved.getValue()).isEqualTo(result);
        assertThat(result.id()).isEqualTo(existing.id());
        assertThat(result.createdAt()).isEqualTo(existing.createdAt());
        assertThat(result.name()).isEqualTo("Cosmic Milk");
        assertThat(result.description()).isEqualTo("new description");
        assertThat(result.price()).isEqualTo(gcr("7.50"));
        assertThat(result.stock()).isEqualTo(99);
        assertThat(result.categoryId()).isEqualTo(FOOD);
        assertThat(result.sellerEmail()).isEqualTo("new@cosmo-cats.market");
    }

    @Test
    void updateAllowsKeepingTheProductsOwnName() {
        Product existing = product();
        Product incoming = product(UUID.randomUUID(), existing.name());
        when(products.findById(existing.id())).thenReturn(Optional.of(existing));
        when(categories.existsById(TOYS)).thenReturn(true);
        when(products.findByNameIgnoreCase(existing.name())).thenReturn(Optional.of(existing));
        when(products.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        Product result = service.update(existing.id(), incoming);

        assertThat(result.id()).isEqualTo(existing.id());
    }

    @Test
    void updateFailsWhenProductIsMissing() {
        UUID id = UUID.randomUUID();
        when(products.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(id, product()))
                .isInstanceOf(ProductNotFoundException.class);
        verify(products, never()).save(any());
    }

    @Test
    void updateFailsWhenCategoryDoesNotExist() {
        Product existing = product();
        Product incoming = product();
        when(products.findById(existing.id())).thenReturn(Optional.of(existing));
        when(categories.existsById(incoming.categoryId())).thenReturn(false);

        assertThatThrownBy(() -> service.update(existing.id(), incoming))
                .isInstanceOf(CategoryNotFoundException.class);
        verify(products, never()).save(any());
    }

    @Test
    void updateFailsWhenNameBelongsToAnotherProduct() {
        Product existing = product();
        Product other = product(UUID.randomUUID(), "Comet Catnip");
        Product incoming = product(UUID.randomUUID(), "Comet Catnip");
        when(products.findById(existing.id())).thenReturn(Optional.of(existing));
        when(categories.existsById(incoming.categoryId())).thenReturn(true);
        when(products.findByNameIgnoreCase("Comet Catnip")).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> service.update(existing.id(), incoming))
                .isInstanceOf(DuplicateProductNameException.class);
        verify(products, never()).save(any());
    }

    @Test
    void deleteRemovesExistingProduct() {
        UUID id = UUID.randomUUID();
        when(products.deleteById(id)).thenReturn(true);

        service.delete(id);

        verify(products).deleteById(id);
    }

    @Test
    void deleteFailsWhenProductIsMissing() {
        UUID id = UUID.randomUUID();
        when(products.deleteById(id)).thenReturn(false);

        assertThatThrownBy(() -> service.delete(id)).isInstanceOf(ProductNotFoundException.class);
    }
}
