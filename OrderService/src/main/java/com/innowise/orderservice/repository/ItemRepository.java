package com.innowise.orderservice.repository;

import com.innowise.orderservice.model.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * @Classname ItemRepository
 * Repository interface for accessing and managing {@link Item} entities.
 * @Author dshparko
 * @Date 13.10.2025 16:25
 * @Version 1.0
 */
@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {
    /**
     * Retrieves an {@link Item} entity by its unique identifier.
     *
     * @param id the unique identifier of the item
     * @return an {@link Optional} containing the item if found, or empty if not
     */
    Optional<Item> findById(Long id);
}
