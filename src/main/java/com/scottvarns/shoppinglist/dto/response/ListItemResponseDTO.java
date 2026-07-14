package com.scottvarns.shoppinglist.dto.response;

import java.math.BigDecimal;

public record ListItemResponseDTO(
        Long itemId,
        Long listId,
        String itemName,
        BigDecimal unitCost,
        Integer quantity,
        boolean inBasket,
        Integer listPosition
) {
}
