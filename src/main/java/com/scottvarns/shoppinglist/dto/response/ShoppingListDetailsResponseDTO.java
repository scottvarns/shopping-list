package com.scottvarns.shoppinglist.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ShoppingListDetailsResponseDTO(
        Long listId,
        Long userId,
        String listName,
        BigDecimal spendingLimit,
        LocalDate date,
        BigDecimal totalCost,
        BigDecimal remainingBudget,
        boolean overBudget,
        List<ShoppingListItemResponseDTO> items
) {
}
