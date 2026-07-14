package com.scottvarns.shoppinglist.service;

import com.scottvarns.shoppinglist.dto.request.CreateListItemRequestDTO;
import com.scottvarns.shoppinglist.dto.request.ReorderListItemRequestDTO;
import com.scottvarns.shoppinglist.dto.response.ListItemResponseDTO;

import java.util.List;

public interface ListItemService {

    /**
     * Appends a new item to a shopping list after list-level validation has been completed.
     *
     * @param listId the shopping list to receive the item
     * @param request the new item's details
     * @return the created list item
     */
    ListItemResponseDTO createListItem(Long listId, CreateListItemRequestDTO request);

    /**
     * Retrieves every item on a shopping list in list-position order.
     *
     * @param listId the shopping list whose items should be retrieved
     * @return the ordered list items
     */
    List<ListItemResponseDTO> getListItems(Long listId);

    /**
     * Deletes an item from a shopping list and closes the resulting position gap.
     *
     * @param listId the shopping list containing the item
     * @param itemId the item to delete
     */
    void deleteListItem(Long listId, Long itemId);

    /**
     * Toggles whether an existing shopping-list item is in the basket.
     *
     * @param listId the shopping list containing the item
     * @param itemId the item whose in-basket state should be toggled
     */
    void toggleListItemInBasket(Long listId, Long itemId);

    /**
     * Reorders all affected items while preserving the relative order of unspecified items.
     *
     * @param listId the shopping list whose items should be reordered
     * @param request the requested item positions
     */
    void reorderListItems(Long listId, List<ReorderListItemRequestDTO> request);
}
