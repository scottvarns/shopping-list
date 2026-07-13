package com.scottvarns.shoppinglist.controller;

import com.scottvarns.shoppinglist.dto.request.CreateShoppingListRequestDTO;
import com.scottvarns.shoppinglist.dto.response.ShoppingListResponseDTO;
import com.scottvarns.shoppinglist.security.JwtService;
import com.scottvarns.shoppinglist.service.ShoppingListService;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestMethodOrder(MethodOrderer.MethodName.class)
@WebMvcTest(ShoppingListController.class)
public class ShoppingListControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ShoppingListService shoppingListService;

    @MockitoBean
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Test case: Valid request with all required fields provided.
     * Expected outcome: HTTP 201 Created with the correct response body.
     */
    @Test
    void test01_createShoppingList_whenValidRequest_thenReturns201() throws Exception {
        // Arrange
        CreateShoppingListRequestDTO request = new CreateShoppingListRequestDTO(
                1L,
                "Weekly Shop",
                new BigDecimal("75.00"),
                LocalDate.of(2026, 7, 18)
        );

        ShoppingListResponseDTO response = new ShoppingListResponseDTO(
                12L,
                1L,
                "Weekly Shop",
                new BigDecimal("75.00"),
                LocalDate.of(2026, 7, 18),
                BigDecimal.ZERO,
                new BigDecimal("75.00"),
                false
        );

        Mockito.when(shoppingListService.createShoppingList(any(CreateShoppingListRequestDTO.class), any(Long.class)))
                .thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/shopping-list")
                        .principal(() -> "1")
                        .header("X-Correlation-ID", "e4b1a8f9-7c32-4d2d-a1b6-f5c9e2b8a7d3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.listId").value(12L))
                .andExpect(jsonPath("$.userId").value(1L))
                .andExpect(jsonPath("$.listName").value("Weekly Shop"))
                .andExpect(jsonPath("$.spendingLimit").value(75.00))
                .andExpect(jsonPath("$.date").value("2026-07-18"))
                .andExpect(jsonPath("$.totalCost").value(0.00))
                .andExpect(jsonPath("$.remainingBudget").value(75.00))
                .andExpect(jsonPath("$.overBudget").value(false));

        Mockito.verify(shoppingListService)
                .createShoppingList(any(CreateShoppingListRequestDTO.class), eq(1L));
    }

    /**
     * Test case: Missing required X-Correlation-ID header.
     * Expected outcome: HTTP 400 Bad Request.
     */
    @Test
    void test02_createShoppingList_whenMissingCorrelationId_thenReturns400() throws Exception {
        // Arrange
        CreateShoppingListRequestDTO request = new CreateShoppingListRequestDTO(
                1L,
                "Weekly Shop",
                new BigDecimal("75.00"),
                LocalDate.of(2026, 7, 18)
        );

        // Act & Assert
        mockMvc.perform(post("/api/shopping-list")
                        .principal(() -> "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    /**
     * Test case: Invalid request with missing or invalid fields.
     * Expected outcome: HTTP 400 Bad Request.
     */
    @Test
    void test03_createShoppingList_whenInvalidRequest_thenReturns400() throws Exception {
        // Arrange
        CreateShoppingListRequestDTO request = new CreateShoppingListRequestDTO(
                null,
                "",
                null,
                null
        );

        // Act & Assert
        mockMvc.perform(post("/api/shopping-list")
                        .principal(() -> "1")
                        .header("X-Correlation-ID", "test-correlation-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
