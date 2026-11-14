package com.cosmocats.marketplace.infrastructure.persistence;

import com.cosmocats.marketplace.support.AbstractPostgresTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class SchemaIT extends AbstractPostgresTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void allChangeSetsAreApplied() {
        List<String> files = jdbc.queryForList(
                "select distinct filename from databasechangelog", String.class);

        assertThat(files).anyMatch(f -> f.endsWith("001-initial-schema.yaml"))
                .anyMatch(f -> f.endsWith("002-unique-category-name.yaml"))
                .anyMatch(f -> f.endsWith("003-unique-product-per-category.yaml"))
                .anyMatch(f -> f.endsWith("004-index-product-name.yaml"));
    }

    @Test
    void idSequencesExist() {
        List<String> sequences = jdbc.queryForList(
                "select sequencename from pg_sequences where schemaname = 'public'", String.class);

        assertThat(sequences).contains("category_id_seq", "product_id_seq", "orders_id_seq", "order_item_id_seq");
    }

    @Test
    void sequencesIncrementByOneLikeHibernateAllocationSize() {
        Integer wrong = jdbc.queryForObject(
                "select count(*) from pg_sequences where schemaname = 'public' and increment_by <> 1",
                Integer.class);

        assertThat(wrong).isZero();
    }

    @Test
    void indexesExist() {
        List<String> indexes = jdbc.queryForList(
                "select indexname from pg_indexes where schemaname = 'public'", String.class);

        assertThat(indexes).contains("idx_product_product_name", "idx_product_category_id",
                "idx_order_item_order_id", "idx_order_item_product_id", "idx_orders_status",
                "uq_product_name_per_category", "uq_category_name", "uq_orders_order_number");
    }

    @Test
    void demoCategoriesAreSeededWithFixedPublicIds() {
        List<String> names = jdbc.queryForList(
                "select name from category where public_id in "
                        + "('11111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222222', "
                        + "'33333333-3333-3333-3333-333333333333') order by name", String.class);

        assertThat(names).containsExactly("Food and drinks", "Toys", "Treats");
    }
}
