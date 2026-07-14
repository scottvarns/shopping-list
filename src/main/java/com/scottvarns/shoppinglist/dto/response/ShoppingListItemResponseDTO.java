package com.scottvarns.shoppinglist.dto.response;

import java.math.BigDecimal;

public record ShoppingListItemResponseDTO(
        Long itemId,
        String itemName,
        BigDecimal unitCost,
        Integer quantity,
        boolean inBasket,
        Integer listPosition
) {
}
