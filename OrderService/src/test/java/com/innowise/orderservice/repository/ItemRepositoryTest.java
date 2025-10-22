package com.innowise.orderservice.repository;

import com.innowise.orderservice.model.entity.Item;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class ItemRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private ItemRepository itemRepository;


    @Test
    void shouldSaveAndFindItem() {
        // given
        Item item = new Item(null, "Test Item", BigDecimal.valueOf(99.99));

        // when
        Item saved = itemRepository.save(item);
        var found = itemRepository.findById(saved.getId());

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Test Item");
        assertThat(found.get().getPrice()).isEqualTo(BigDecimal.valueOf(99.99));
    }
}
