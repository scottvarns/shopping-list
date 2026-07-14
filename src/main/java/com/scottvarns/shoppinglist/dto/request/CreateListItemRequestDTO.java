package com.scottvarns.shoppinglist.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateListItemRequestDTO(
        @NotBlank(message = "CreateListItemRequestDTO.itemName is required")
        @Size(max = 255)
        String itemName,

        @NotNull(message = "CreateListItemRequestDTO.unitCost is required")
        @DecimalMin(value = "0.00", message = "CreateListItemRequestDTO.unitCost must be greater than or equal to 0.00")
        BigDecimal unitCost,

        @NotNull(message = "CreateListItemRequestDTO.quantity is required")
        @Positive(message = "CreateListItemRequestDTO.quantity must be greater than zero")
        Integer quantity
) {
}
