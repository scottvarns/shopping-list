package com.scottvarns.shoppinglist.controller;

import com.scottvarns.shoppinglist.dto.request.CreateShoppingListRequestDTO;
import com.scottvarns.shoppinglist.dto.request.CreateListItemRequestDTO;
import com.scottvarns.shoppinglist.dto.request.ReorderListItemRequestDTO;
import com.scottvarns.shoppinglist.dto.response.ListItemResponseDTO;
import com.scottvarns.shoppinglist.dto.response.ShoppingListDetailsResponseDTO;
import com.scottvarns.shoppinglist.dto.response.ShoppingListResponseDTO;
import com.scottvarns.shoppinglist.exception.BadRequestException;
import com.scottvarns.shoppinglist.service.ShoppingListService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;
import java.util.List;

@RestController
@RequestMapping("/api/shopping-list")
@RequiredArgsConstructor
public class ShoppingListController {

    private static final Logger log = LoggerFactory.getLogger(ShoppingListController.class);

    private final ShoppingListService shoppingListService;

    /**
     * Retrieves a shopping list and its ordered items for the authenticated user.
     *
     * @param listId the shopping list to retrieve
     * @param correlationId correlation ID for end-to-end traceability
     * @param principal the authenticated user established from the JWT
     *
     * @return the shopping list, items and calculated budget information
     */
    @GetMapping("/{listId}")
    public ResponseEntity<ShoppingListDetailsResponseDTO> getShoppingList(
            @PathVariable String listId,
            @RequestHeader("X-Correlation-ID") UUID correlationId,
            Principal principal
    ) {
        log.info("Received get-shopping-list request [listId={}]", listId);
        Long parsedListId = convertPathParameterToLong(listId, "listId");
        ShoppingListDetailsResponseDTO response = shoppingListService.getShoppingList(
                parsedListId, Long.valueOf(principal.getName()));

        return ResponseEntity
                .ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }

    /**
     * Endpoint to create a new shopping list.
     *
     * @param correlationId Correlation ID for end-to-end traceability.
     * @param request       The request body containing shopping list details.
     * @param principal     The authenticated user established from the JWT.
     *
     * @return ResponseEntity containing the created shopping list response DTO.
     */
    @PostMapping
    public ResponseEntity<ShoppingListResponseDTO> createShoppingList(
            @RequestHeader(value = "X-Correlation-ID") UUID correlationId,
            @Valid @RequestBody CreateShoppingListRequestDTO request,
            Principal principal
    ) {
        log.info("Received shopping-list creation request for userId [{}]", request.userId());
        ShoppingListResponseDTO response =
                shoppingListService.createShoppingList(request, Long.valueOf(principal.getName()));

        return ResponseEntity
                .status(201)
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }

    /**
     * Appends an item to a shopping list owned by the authenticated user.
     *
     * @param listId the shopping list to receive the item
     * @param correlationId correlation ID for end-to-end traceability
     * @param request the new item's details
     * @param principal the authenticated user established from the JWT
     *
     * @return the created list item
     */
    @PostMapping("/{listId}/item")
    public ResponseEntity<ListItemResponseDTO> createListItem(
            @PathVariable String listId,
            @RequestHeader("X-Correlation-ID") UUID correlationId,
            @Valid @RequestBody CreateListItemRequestDTO request,
            Principal principal
    ) {
        log.info("Received list-item creation request [listId={}]", listId);
        Long parsedListId = convertPathParameterToLong(listId, "listId");
        ListItemResponseDTO response = shoppingListService.createListItem(
                parsedListId, request, Long.valueOf(principal.getName()));

        return ResponseEntity
                .status(201)
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }

    /**
     * Deletes an item from a shopping list owned by the authenticated user.
     *
     * @param listId the shopping list containing the item
     * @param itemId the item to delete
     * @param correlationId correlation ID for end-to-end traceability
     * @param principal the authenticated user established from the JWT
     *
     * @return an empty HTTP 204 No Content response
     */
    @DeleteMapping("/{listId}/item/{itemId}")
    public ResponseEntity<Void> deleteListItem(
            @PathVariable String listId,
            @PathVariable String itemId,
            @RequestHeader("X-Correlation-ID") UUID correlationId,
            Principal principal
    ) {
        log.info("Received list-item deletion request [listId={}, itemId={}]", listId, itemId);
        Long parsedListId = convertPathParameterToLong(listId, "listId");
        Long parsedItemId = convertPathParameterToLong(itemId, "itemId");
        shoppingListService.deleteListItem(
                parsedListId, parsedItemId, Long.valueOf(principal.getName()));

        return ResponseEntity
                .noContent()
                .build();
    }

    /**
     * Toggles whether an item is in the basket without removing or reordering it.
     *
     * @param listId the shopping list containing the item
     * @param itemId the item whose in-basket state should be toggled
     * @param correlationId correlation ID for end-to-end traceability
     * @param principal the authenticated user established from the JWT
     *
     * @return an empty HTTP 204 No Content response
     */
    @PatchMapping("/{listId}/item/{itemId}")
    public ResponseEntity<Void> toggleListItemInBasket(
            @PathVariable String listId,
            @PathVariable String itemId,
            @RequestHeader("X-Correlation-ID") UUID correlationId,
            Principal principal
    ) {
        log.info("Received toggle-list-item-in-basket request [listId={}, itemId={}]", listId, itemId);
        Long parsedListId = convertPathParameterToLong(listId, "listId");
        Long parsedItemId = convertPathParameterToLong(itemId, "itemId");
        shoppingListService.toggleListItemInBasket(
                parsedListId, parsedItemId, Long.valueOf(principal.getName()));

        return ResponseEntity
                .noContent()
                .build();
    }

    /**
     * Reorders items on a shopping list owned by the authenticated user.
     *
     * @param listId the shopping list whose items should be reordered
     * @param correlationId correlation ID for end-to-end traceability
     * @param request the requested item positions
     * @param principal the authenticated user established from the JWT
     *
     * @return an empty HTTP 204 No Content response
     */
    @PatchMapping("/{listId}/item/reorder")
    public ResponseEntity<Void> reorderListItems(
            @PathVariable String listId,
            @RequestHeader("X-Correlation-ID") UUID correlationId,
            @RequestBody List<@NotNull @Valid ReorderListItemRequestDTO> request,
            Principal principal
    ) {
        log.info("Received reorder-list-items request [listId={}]", listId);
        Long parsedListId = convertPathParameterToLong(listId, "listId");
        shoppingListService.reorderListItems(parsedListId, request, Long.valueOf(principal.getName()));

        return ResponseEntity
                .noContent()
                .build();
    }

    private Long convertPathParameterToLong(String value, String parameterName) {
        // Guard against non-numeric values in path parameters to prevent NumberFormatException being thrown
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException exception) {
            // Throw a BadRequestException with a clear message indicating which path parameter is invalid
            throw new BadRequestException(String.format(
                    "Path parameter [%s] must be a numeric value, non-numeric value [%s] provided", parameterName, value));
        }
    }

}
