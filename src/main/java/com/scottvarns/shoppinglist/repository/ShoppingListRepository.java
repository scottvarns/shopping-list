package com.scottvarns.shoppinglist.repository;

import com.scottvarns.shoppinglist.entity.ShoppingList;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShoppingListRepository extends JpaRepository<ShoppingList, Long> {

    ShoppingList findByListId(Long listId);

    boolean existsByUserIdAndListNameIgnoreCase(Long userId, String listName);
}
