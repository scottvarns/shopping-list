package com.scottvarns.shoppinglist.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Builder
public record CreateShoppingListRequestDTO(

        @NotNull(message = "CreateShoppingListRequestDTO.userId is required")
        @Positive(message = "CreateShoppingListRequestDTO.userId must be greater than zero")
        Long userId,

        @NotBlank(message = "CreateShoppingListRequestDTO.listname is required")
        @Size(max = 150)
        String listName,

        @DecimalMin(value = "0.00", message = "CreateShoppingListRequestDTO.spendingLimit must be greater than or equal to 0.00")
        @DecimalMax(value = "99999999.99", message = "CreateShoppingListRequestDTO.spendingLimit must be less than or equal to 9999999999.99")
        BigDecimal spendingLimit,

        LocalDate date

) {
}
