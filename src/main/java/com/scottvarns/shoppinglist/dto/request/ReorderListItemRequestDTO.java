package com.scottvarns.shoppinglist.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ReorderListItemRequestDTO(
        @NotNull(message = "ReorderListItemRequestDTO.itemId is required")
        @Positive(message = "ReorderListItemRequestDTO.itemId must be greater than zero")
        Long itemId,

        @NotNull(message = "ReorderListItemRequestDTO.listPosition is required")
        @Positive(message = "ReorderListItemRequestDTO.listPosition must be greater than zero")
        Integer listPosition
) {
}
