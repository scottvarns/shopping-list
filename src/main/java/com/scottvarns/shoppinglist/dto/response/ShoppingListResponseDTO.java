package com.scottvarns.shoppinglist.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Builder
public record ShoppingListResponseDTO(

        @JsonProperty("listId")
        Long listId,

        @JsonProperty("userId")
        Long userId,

        @JsonProperty("listName")
        String listName,

        @JsonProperty("spendingLimit")
        BigDecimal spendingLimit,

        @JsonProperty("date")
        LocalDate date,

        @JsonProperty("totalCost")
        BigDecimal totalCost,

        @JsonProperty("remainingBudget")
        BigDecimal remainingBudget,

        @JsonProperty("overBudget")
        boolean overBudget
) {
}
