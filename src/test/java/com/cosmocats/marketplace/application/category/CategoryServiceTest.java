package com.cosmocats.marketplace.application.category;

import com.cosmocats.marketplace.domain.exception.CategoryNotFoundException;
import com.cosmocats.marketplace.domain.exception.DuplicateCategoryNameException;
import com.cosmocats.marketplace.domain.model.Category;
import com.cosmocats.marketplace.domain.repository.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categories;
    @InjectMocks
    private CategoryService service;

    @Test
    void findAllDelegates() {
        List<Category> all = List.of(new Category(UUID.randomUUID(), "Toys"));
        when(categories.findAll()).thenReturn(all);

        assertThat(service.findAll()).isEqualTo(all);
    }

    @Test
    void findByIdThrowsWhenMissing() {
        UUID id = UUID.randomUUID();
        when(categories.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(id)).isInstanceOf(CategoryNotFoundException.class);
    }

    @Test
    void createStripsNameAndSaves() {
        when(categories.findByNameIgnoreCase("Beds")).thenReturn(Optional.empty());
        when(categories.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));

        Category created = service.create("  Beds ");

        assertThat(created.name()).isEqualTo("Beds");
        assertThat(created.id()).isNotNull();
    }

    @Test
    void createFailsWhenNameIsTaken() {
        when(categories.findByNameIgnoreCase("Toys"))
                .thenReturn(Optional.of(new Category(UUID.randomUUID(), "toys")));

        assertThatThrownBy(() -> service.create("Toys")).isInstanceOf(DuplicateCategoryNameException.class);
        verify(categories, never()).save(any());
    }

    @Test
    void renameKeepsIdAndAllowsOwnName() {
        Category existing = new Category(UUID.randomUUID(), "Toys");
        when(categories.findById(existing.id())).thenReturn(Optional.of(existing));
        when(categories.findByNameIgnoreCase("TOYS")).thenReturn(Optional.of(existing));
        when(categories.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));

        Category renamed = service.rename(existing.id(), "TOYS");

        assertThat(renamed.id()).isEqualTo(existing.id());
        assertThat(renamed.name()).isEqualTo("TOYS");
    }

    @Test
    void renameFailsWhenNameBelongsToAnotherCategory() {
        Category existing = new Category(UUID.randomUUID(), "Toys");
        when(categories.findById(existing.id())).thenReturn(Optional.of(existing));
        when(categories.findByNameIgnoreCase("Food"))
                .thenReturn(Optional.of(new Category(UUID.randomUUID(), "Food")));

        assertThatThrownBy(() -> service.rename(existing.id(), "Food"))
                .isInstanceOf(DuplicateCategoryNameException.class);
    }

    @Test
    void deleteFailsWhenMissing() {
        UUID id = UUID.randomUUID();
        when(categories.deleteById(id)).thenReturn(false);

        assertThatThrownBy(() -> service.delete(id)).isInstanceOf(CategoryNotFoundException.class);
    }

    @Test
    void deleteRemovesExistingCategory() {
        UUID id = UUID.randomUUID();
        when(categories.deleteById(id)).thenReturn(true);

        service.delete(id);

        verify(categories).deleteById(id);
    }
}
