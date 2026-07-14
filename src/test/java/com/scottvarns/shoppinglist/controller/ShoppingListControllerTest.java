package com.scottvarns.shoppinglist.controller;

import com.scottvarns.shoppinglist.dto.request.CreateShoppingListRequestDTO;
import com.scottvarns.shoppinglist.dto.request.CreateListItemRequestDTO;
import com.scottvarns.shoppinglist.dto.request.ReorderListItemRequestDTO;
import com.scottvarns.shoppinglist.dto.response.ListItemResponseDTO;
import com.scottvarns.shoppinglist.dto.response.ShoppingListDetailsResponseDTO;
import com.scottvarns.shoppinglist.dto.response.ShoppingListItemResponseDTO;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
     * When a valid request with all required fields is provided to the create shopping list endpoint by an authenticated user
     * Then a HTTP 201 Created with the correct response body is returned
     */
    @Test
    void test01_createShoppingList_whenValidRequest_thenReturns201() throws Exception {
        // Create a valid request and expected response
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

        // Mock the service layer to return the expected response when called with any request and user ID
        Mockito.when(shoppingListService.createShoppingList(any(CreateShoppingListRequestDTO.class), any(Long.class)))
                .thenReturn(response);

        // Invoke the controller endpoint and assert the response
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

        // Verify that the service layer was called with the correct parameters
        Mockito.verify(shoppingListService)
                .createShoppingList(any(CreateShoppingListRequestDTO.class), eq(1L));
    }

    /**
     * When a request is made to the create shopping list endpoint without an X-Correlation-ID header
     * Then a HTTP 400 Bad Request is returned
     */
    @Test
    void test02_createShoppingList_whenMissingCorrelationId_thenReturns400() throws Exception {
        // Create a valid request
        CreateShoppingListRequestDTO request = new CreateShoppingListRequestDTO(
                1L,
                "Weekly Shop",
                new BigDecimal("75.00"),
                LocalDate.of(2026, 7, 18)
        );

        // Invoke the controller endpoint without the X-Correlation-ID header and assert that a 400 Bad Request is returned
        mockMvc.perform(post("/api/shopping-list")
                        .principal(() -> "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    /**
     * When an invalid request with missing or invalid fields is provided to the create shopping list endpoint
     * Then a HTTP 400 Bad Request is returned
     */
    @Test
    void test03_createShoppingList_whenInvalidRequest_thenReturns400() throws Exception {
        // Create an invalid request with missing or invalid fields
        CreateShoppingListRequestDTO request = new CreateShoppingListRequestDTO(
                null,
                "",
                null,
                null
        );

        // Invoke the controller endpoint with the invalid request and assert that a 400 Bad Request is returned
        mockMvc.perform(post("/api/shopping-list")
                        .principal(() -> "1")
                        .header("X-Correlation-ID", "test-correlation-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    /**
     * When an authenticated owner submits a valid request to the create list item endpoint
     * Then a HTTP 201 Created with the created item in the request body is returned.
     */
    @Test
    void test04_createListItem_whenValidRequest_thenReturns201() throws Exception {
        // Create a valid request and expected response
        CreateListItemRequestDTO request = new CreateListItemRequestDTO(
                "Milk",
                new BigDecimal("1.25"),
                2
        );

        ListItemResponseDTO response = new ListItemResponseDTO(
                44L,
                12L,
                "Milk",
                new BigDecimal("1.25"),
                2,
                false,
                1
        );

        // Mock the service layer to return the expected response when called with any list ID, request, and user ID
        Mockito.when(shoppingListService.createListItem(any(Long.class), any(CreateListItemRequestDTO.class), any(Long.class)))
                .thenReturn(response);

        // Invoke the controller endpoint and assert the response
        mockMvc.perform(post("/api/shopping-list/12/item")
                        .principal(() -> "1")
                        .header("X-Correlation-ID", "e4b1a8f9-7c32-4d2d-a1b6-f5c9e2b8a7d3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.itemId").value(44L))
                .andExpect(jsonPath("$.listId").value(12L))
                .andExpect(jsonPath("$.itemName").value("Milk"))
                .andExpect(jsonPath("$.unitCost").value(1.25))
                .andExpect(jsonPath("$.quantity").value(2))
                .andExpect(jsonPath("$.inBasket").value(false))
                .andExpect(jsonPath("$.listPosition").value(1));

        // Verify that the service layer was called with the correct parameters
        Mockito.verify(shoppingListService).createListItem(eq(12L), any(CreateListItemRequestDTO.class), eq(1L));
    }

    /**
     * When a request is made to the create list item endpoint without an X-Correlation-ID header
     * Then a HTTP 400 Bad Request is returned
     */
    @Test
    void test05_createListItem_whenMissingCorrelationId_thenReturns400() throws Exception {
        // Create a valid request
        CreateListItemRequestDTO request = new CreateListItemRequestDTO(
                "Milk",
                new BigDecimal("1.25"),
                2
        );

        // Invoke the controller endpoint without the X-Correlation-ID header and assert that a 400 Bad Request is returned
        mockMvc.perform(post("/api/shopping-list/12/item")
                        .principal(() -> "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    /**
     * When an invalid request with missing or invalid fields is provided to the create list item endpoint
     * Then a HTTP 400 Bad Request is returned
     */
    @Test
    void test06_createListItem_whenInvalidRequest_thenReturns400() throws Exception {
        // Create an invalid request with missing or invalid fields
        CreateListItemRequestDTO request = new CreateListItemRequestDTO(
                "",
                new BigDecimal("-1.00"),
                0
        );

        // Invoke the controller endpoint with the invalid request and assert that a 400 Bad Request is returned
        mockMvc.perform(post("/api/shopping-list/12/item")
                        .principal(() -> "1")
                        .header("X-Correlation-ID", "e4b1a8f9-7c32-4d2d-a1b6-f5c9e2b8a7d3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        Mockito.verifyNoInteractions(shoppingListService);
    }

    /**
     * When an authenticated owner deletes an existing list item
     * Then a HTTP 204 No Content response with an empty body is returned.
     */
    @Test
    void test07_deleteListItem_whenValidRequest_thenReturns204() throws Exception {
        // Invoke the delete endpoint without a request body and assert the empty response
        mockMvc.perform(delete("/api/shopping-list/12/item/44")
                        .principal(() -> "1")
                        .header("X-Correlation-ID", "e4b1a8f9-7c32-4d2d-a1b6-f5c9e2b8a7d3"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        // Verify that the service layer was called with the list, item, and authenticated user IDs
        Mockito.verify(shoppingListService).deleteListItem(12L, 44L, 1L);
    }

    /**
     * When list-item deletion is requested without an X-Correlation-ID header
     * Then a HTTP 400 Bad Request response is returned.
     */
    @Test
    void test08_deleteListItem_whenMissingCorrelationId_thenReturns400() throws Exception {
        // Invoke the delete endpoint without the required correlation header
        mockMvc.perform(delete("/api/shopping-list/12/item/44")
                        .principal(() -> "1"))
                .andExpect(status().isBadRequest());

        // Verify that validation rejected the request before the service layer was called
        Mockito.verifyNoInteractions(shoppingListService);
    }

    /**
     * When the create list-item endpoint receives a non-numeric list ID
     * Then a HTTP 400 Bad Request response is returned.
     */
    @Test
    void test09_createListItem_whenListIdIsNonNumeric_thenReturns400() throws Exception {
        // Create a valid request so path conversion is the only invalid input
        CreateListItemRequestDTO request = new CreateListItemRequestDTO(
                "Milk",
                new BigDecimal("1.25"),
                2
        );

        // Invoke the endpoint with a non-numeric list ID and assert the bad-request response
        mockMvc.perform(post("/api/shopping-list/not-a-number/item")
                        .principal(() -> "1")
                        .header("X-Correlation-ID", "e4b1a8f9-7c32-4d2d-a1b6-f5c9e2b8a7d3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        // Verify that path conversion rejected the request before the service layer was called
        Mockito.verifyNoInteractions(shoppingListService);
    }

    /**
     * When the delete list-item endpoint receives a non-numeric list ID
     * Then a HTTP 400 Bad Request response is returned.
     */
    @Test
    void test10_deleteListItem_whenListIdIsNonNumeric_thenReturns400() throws Exception {
        // Invoke the endpoint with a non-numeric list ID and a valid item ID
        mockMvc.perform(delete("/api/shopping-list/not-a-number/item/44")
                        .principal(() -> "1")
                        .header("X-Correlation-ID", "e4b1a8f9-7c32-4d2d-a1b6-f5c9e2b8a7d3"))
                .andExpect(status().isBadRequest());

        // Verify that path conversion rejected the request before the service layer was called
        Mockito.verifyNoInteractions(shoppingListService);
    }

    /**
     * When the delete list-item endpoint receives a non-numeric item ID
     * Then a HTTP 400 Bad Request response is returned.
     */
    @Test
    void test11_deleteListItem_whenItemIdIsNonNumeric_thenReturns400() throws Exception {
        // Invoke the endpoint with a valid list ID and a non-numeric item ID
        mockMvc.perform(delete("/api/shopping-list/12/item/not-a-number")
                        .principal(() -> "1")
                        .header("X-Correlation-ID", "e4b1a8f9-7c32-4d2d-a1b6-f5c9e2b8a7d3"))
                .andExpect(status().isBadRequest());

        // Verify that path conversion rejected the request before the service layer was called
        Mockito.verifyNoInteractions(shoppingListService);
    }

    /**
     * When an authenticated owner toggles an existing item's in-basket state
     * Then a HTTP 204 No Content response with an empty body is returned.
     */
    @Test
    void test12_toggleListItemInBasket_whenValidRequest_thenReturns204() throws Exception {
        // Invoke the PATCH endpoint without a request body and assert the empty response
        mockMvc.perform(patch("/api/shopping-list/12/item/44")
                        .principal(() -> "1")
                        .header("X-Correlation-ID", "e4b1a8f9-7c32-4d2d-a1b6-f5c9e2b8a7d3"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        // Verify that the service layer received the list, item, and authenticated user IDs
        Mockito.verify(shoppingListService).toggleListItemInBasket(12L, 44L, 1L);
    }

    /**
     * When toggling an item without an X-Correlation-ID header
     * Then a HTTP 400 Bad Request response is returned.
     */
    @Test
    void test13_toggleListItemInBasket_whenMissingCorrelationId_thenReturns400() throws Exception {
        // Invoke the PATCH endpoint without the required correlation header
        mockMvc.perform(patch("/api/shopping-list/12/item/44")
                        .principal(() -> "1"))
                .andExpect(status().isBadRequest());

        // Verify that header validation rejected the request before the service was called
        Mockito.verifyNoInteractions(shoppingListService);
    }

    /**
     * When the PATCH endpoint receives a non-numeric list ID
     * Then a HTTP 400 Bad Request response is returned.
     */
    @Test
    void test14_toggleListItemInBasket_whenListIdIsNonNumeric_thenReturns400() throws Exception {
        // Invoke the endpoint with a non-numeric list ID
        mockMvc.perform(patch("/api/shopping-list/not-a-number/item/44")
                        .principal(() -> "1")
                        .header("X-Correlation-ID", "e4b1a8f9-7c32-4d2d-a1b6-f5c9e2b8a7d3"))
                .andExpect(status().isBadRequest());

        // Verify that path conversion rejected the request before the service was called
        Mockito.verifyNoInteractions(shoppingListService);
    }

    /**
     * When the PATCH endpoint receives a non-numeric item ID
     * Then a HTTP 400 Bad Request response is returned.
     */
    @Test
    void test15_toggleListItemInBasket_whenItemIdIsNonNumeric_thenReturns400() throws Exception {
        // Invoke the endpoint with a non-numeric item ID
        mockMvc.perform(patch("/api/shopping-list/12/item/not-a-number")
                        .principal(() -> "1")
                        .header("X-Correlation-ID", "e4b1a8f9-7c32-4d2d-a1b6-f5c9e2b8a7d3"))
                .andExpect(status().isBadRequest());

        // Verify that path conversion rejected the request before the service was called
        Mockito.verifyNoInteractions(shoppingListService);
    }

    /**
     * When an authenticated owner requests an existing shopping list
     * Then a HTTP 200 OK response containing the list, budget data and ordered items is returned.
     */
    @Test
    void test16_getShoppingList_whenValidRequest_thenReturns200() throws Exception {
        // Create the expected combined shopping-list response
        ShoppingListDetailsResponseDTO response = new ShoppingListDetailsResponseDTO(
                12L,
                1L,
                "Weekly Shop",
                new BigDecimal("10.00"),
                LocalDate.of(2026, 7, 18),
                new BigDecimal("11.00"),
                new BigDecimal("-1.00"),
                true,
                List.of(new ShoppingListItemResponseDTO(
                        44L, "Milk", new BigDecimal("5.50"), 2, false, 1))
        );
        Mockito.when(shoppingListService.getShoppingList(12L, 1L)).thenReturn(response);

        // Invoke the GET endpoint and assert the list and nested item response
        mockMvc.perform(get("/api/shopping-list/12")
                        .principal(() -> "1")
                        .header("X-Correlation-ID", "e4b1a8f9-7c32-4d2d-a1b6-f5c9e2b8a7d3"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.listId").value(12L))
                .andExpect(jsonPath("$.userId").value(1L))
                .andExpect(jsonPath("$.listName").value("Weekly Shop"))
                .andExpect(jsonPath("$.spendingLimit").value(10.00))
                .andExpect(jsonPath("$.date").value("2026-07-18"))
                .andExpect(jsonPath("$.totalCost").value(11.00))
                .andExpect(jsonPath("$.remainingBudget").value(-1.00))
                .andExpect(jsonPath("$.overBudget").value(true))
                .andExpect(jsonPath("$.items[0].itemId").value(44L))
                .andExpect(jsonPath("$.items[0].itemName").value("Milk"))
                .andExpect(jsonPath("$.items[0].unitCost").value(5.50))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.items[0].inBasket").value(false))
                .andExpect(jsonPath("$.items[0].listPosition").value(1))
                .andExpect(jsonPath("$.items[0].listId").doesNotExist());

        // Verify that the service received the parsed list ID and authenticated user ID
        Mockito.verify(shoppingListService).getShoppingList(12L, 1L);
    }

    /**
     * When a shopping-list retrieval request does not contain an X-Correlation-ID header
     * Then a HTTP 400 Bad Request response is returned.
     */
    @Test
    void test17_getShoppingList_whenMissingCorrelationId_thenReturns400() throws Exception {
        // Invoke the GET endpoint without the required correlation header
        mockMvc.perform(get("/api/shopping-list/12")
                        .principal(() -> "1"))
                .andExpect(status().isBadRequest());

        // Verify that header validation rejected the request before the service was called
        Mockito.verifyNoInteractions(shoppingListService);
    }

    /**
     * When the get shopping-list endpoint receives a non-numeric list ID
     * Then a HTTP 400 Bad Request response is returned.
     */
    @Test
    void test18_getShoppingList_whenListIdIsNonNumeric_thenReturns400() throws Exception {
        // Invoke the GET endpoint with a non-numeric list ID
        mockMvc.perform(get("/api/shopping-list/not-a-number")
                        .principal(() -> "1")
                        .header("X-Correlation-ID", "e4b1a8f9-7c32-4d2d-a1b6-f5c9e2b8a7d3"))
                .andExpect(status().isBadRequest());

        // Verify that path conversion rejected the request before the service was called
        Mockito.verifyNoInteractions(shoppingListService);
    }

    /**
     * When an authenticated owner submits valid item positions to the reorder endpoint
     * Then a HTTP 204 No Content response with an empty body is returned.
     */
    @Test
    void test19_reorderListItems_whenValidRequest_thenReturns204() throws Exception {
        // Create a valid partial reorder request
        List<ReorderListItemRequestDTO> request = List.of(
                new ReorderListItemRequestDTO(44L, 2),
                new ReorderListItemRequestDTO(45L, 1));

        // Invoke the PATCH endpoint and assert the empty response
        mockMvc.perform(patch("/api/shopping-list/12/item/reorder")
                        .principal(() -> "1")
                        .header("X-Correlation-ID", "e4b1a8f9-7c32-4d2d-a1b6-f5c9e2b8a7d3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        // Verify that the service received the parsed list ID, request and authenticated user ID
        Mockito.verify(shoppingListService).reorderListItems(12L, request, 1L);
    }

    /**
     * When a reorder request does not contain an X-Correlation-ID header
     * Then a HTTP 400 Bad Request response is returned.
     */
    @Test
    void test20_reorderListItems_whenMissingCorrelationId_thenReturns400() throws Exception {
        // Create a valid request so the missing header is the only invalid input
        List<ReorderListItemRequestDTO> request = List.of(new ReorderListItemRequestDTO(44L, 1));

        // Invoke the PATCH endpoint without the required correlation header
        mockMvc.perform(patch("/api/shopping-list/12/item/reorder")
                        .principal(() -> "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        // Verify that header validation rejected the request before the service was called
        Mockito.verifyNoInteractions(shoppingListService);
    }

    /**
     * When the reorder endpoint receives a non-numeric list ID
     * Then a HTTP 400 Bad Request response is returned.
     */
    @Test
    void test21_reorderListItems_whenListIdIsNonNumeric_thenReturns400() throws Exception {
        // Create a valid request so path conversion is the only invalid input
        List<ReorderListItemRequestDTO> request = List.of(new ReorderListItemRequestDTO(44L, 1));

        // Invoke the PATCH endpoint with a non-numeric list ID
        mockMvc.perform(patch("/api/shopping-list/not-a-number/item/reorder")
                        .principal(() -> "1")
                        .header("X-Correlation-ID", "e4b1a8f9-7c32-4d2d-a1b6-f5c9e2b8a7d3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        // Verify that path conversion rejected the request before the service was called
        Mockito.verifyNoInteractions(shoppingListService);
    }

    /**
     * When a reorder request contains an invalid item ID or list position
     * Then a HTTP 400 Bad Request response is returned.
     */
    @Test
    void test22_reorderListItems_whenRequestEntryIsInvalid_thenReturns400() throws Exception {
        // Create a request whose item ID is missing and position is not positive
        List<ReorderListItemRequestDTO> request = List.of(new ReorderListItemRequestDTO(null, 0));

        // Invoke the PATCH endpoint and assert request-body validation
        mockMvc.perform(patch("/api/shopping-list/12/item/reorder")
                        .principal(() -> "1")
                        .header("X-Correlation-ID", "e4b1a8f9-7c32-4d2d-a1b6-f5c9e2b8a7d3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        // Verify that body validation rejected the request before the service was called
        Mockito.verifyNoInteractions(shoppingListService);
    }

}
