package com.innowise.orderservice.repository;

import com.innowise.orderservice.model.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * @Classname ItemRepository
 * Repository interface for accessing and managing {@link Item} entities.
 * @Author dshparko
 * @Date 13.10.2025 16:25
 * @Version 1.0
 */
public interface ItemRepository extends JpaRepository<Item, Long> {
    /**
     * Retrieves an {@link Item} entity by its unique identifier.
     * <p>
     * This method is redundant as {@link JpaRepository} already provides it,
     * but it can be overridden for custom behavior if needed.
     *
     * @param id the unique identifier of the item
     * @return an {@link Optional} containing the item if found, or empty if not
     */
    Optional<Item> findById(Long id);
}
