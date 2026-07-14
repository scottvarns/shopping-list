package com.scottvarns.shoppinglist.service;

import com.scottvarns.shoppinglist.dto.request.CreateShoppingListRequestDTO;
import com.scottvarns.shoppinglist.dto.request.CreateListItemRequestDTO;
import com.scottvarns.shoppinglist.dto.request.ReorderListItemRequestDTO;
import com.scottvarns.shoppinglist.dto.response.ListItemResponseDTO;
import com.scottvarns.shoppinglist.dto.response.ShoppingListDetailsResponseDTO;
import com.scottvarns.shoppinglist.dto.response.ShoppingListResponseDTO;
import com.scottvarns.shoppinglist.exception.BadRequestException;
import com.scottvarns.shoppinglist.exception.ConflictException;
import com.scottvarns.shoppinglist.exception.ForbiddenException;
import com.scottvarns.shoppinglist.exception.NotFoundException;

import java.util.List;

public interface ShoppingListService {

    /**
     * Creates a shopping list for the authenticated user.
     *
     * @param request the shopping-list details to create
     * @param callingUserId the ID extracted from the authenticated request
     * @return the created shopping list
     */
    ShoppingListResponseDTO createShoppingList(CreateShoppingListRequestDTO request, Long callingUserId) throws BadRequestException,  NotFoundException,
                                                                                            ForbiddenException, ConflictException;

    /**
     * Retrieves a shopping list, its ordered items and its calculated budget information.
     *
     * @param listId the shopping list to retrieve
     * @param callingUserId the ID extracted from the authenticated request
     * @return the shopping list details
     */
    ShoppingListDetailsResponseDTO getShoppingList(Long listId, Long callingUserId);

    /**
     * Validates list-level access and appends a new item to the shopping list.
     *
     * @param listId the shopping list to receive the item
     * @param request the new item's details
     * @param callingUserId the ID extracted from the authenticated request
     * @return the created list item
     */
    ListItemResponseDTO createListItem(Long listId, CreateListItemRequestDTO request, Long callingUserId);

    /**
     * Validates list-level access and deletes an item from the shopping list.
     *
     * @param listId the shopping list containing the item
     * @param itemId the item to delete
     * @param callingUserId the ID extracted from the authenticated request
     */
    void deleteListItem(Long listId, Long itemId, Long callingUserId);

    /**
     * Validates list-level access and toggles whether an item is in the basket.
     *
     * @param listId the shopping list containing the item
     * @param itemId the item whose in-basket state should be toggled
     * @param callingUserId the ID extracted from the authenticated request
     */
    void toggleListItemInBasket(Long listId, Long itemId, Long callingUserId);

    /**
     * Validates list-level access and reorders items on a shopping list.
     *
     * @param listId the shopping list whose items should be reordered
     * @param request the requested item positions
     * @param callingUserId the ID extracted from the authenticated request
     */
    void reorderListItems(Long listId, List<ReorderListItemRequestDTO> request, Long callingUserId);

}
