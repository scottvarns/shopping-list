package com.scottvarns.shoppinglist.repository;

import com.scottvarns.shoppinglist.entity.ListItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ListItemRepository extends JpaRepository<ListItem, Long> {

    boolean existsByListIdAndItemNameIgnoreCase(Long listId, String itemName);

    @Query("SELECT MAX(listItem.listPosition) FROM ListItem listItem WHERE listItem.listId = :listId")
    Integer findMaxListPositionByListId(@Param("listId") Long listId);

    Optional<ListItem> findByItemIdAndListId(Long itemId, Long listId);

    List<ListItem> findAllByListIdOrderByListPositionAsc(Long listId);

    List<ListItem> findAllByListIdAndListPositionGreaterThanOrderByListPositionAsc(
            Long listId,
            Integer listPosition
    );
}
