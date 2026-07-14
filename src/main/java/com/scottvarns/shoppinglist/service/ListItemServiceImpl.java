package com.scottvarns.shoppinglist.service;

import com.scottvarns.shoppinglist.dto.request.CreateListItemRequestDTO;
import com.scottvarns.shoppinglist.dto.request.ReorderListItemRequestDTO;
import com.scottvarns.shoppinglist.dto.response.ListItemResponseDTO;
import com.scottvarns.shoppinglist.entity.ListItem;
import com.scottvarns.shoppinglist.exception.ConflictException;
import com.scottvarns.shoppinglist.exception.BadRequestException;
import com.scottvarns.shoppinglist.exception.NotFoundException;
import com.scottvarns.shoppinglist.repository.ListItemRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@RequiredArgsConstructor
@Service
public class ListItemServiceImpl implements ListItemService {

    private static final Logger log = LoggerFactory.getLogger(ListItemServiceImpl.class);

    private final ListItemRepository listItemRepository;

    /**
     * Appends a new item to a shopping list after list-level validation has been completed.
     *
     * @param listId the shopping list to receive the item
     * @param request the new item's details
     *
     * @return the created list item
     */
    @Override
    public ListItemResponseDTO createListItem(Long listId, CreateListItemRequestDTO request) {

        // Trim whitespace from the item name
        String itemName = request.itemName().trim();

        // Validate that the item name does not already exist in the list
        validateListItemNameDoesNotExist(listId, itemName);

        // Determine the next list position, treating a null maximum as an empty list
        Integer maxListPosition = listItemRepository.findMaxListPositionByListId(listId);
        int nextListPosition = maxListPosition == null ? 1 : maxListPosition + 1;

        // Create and save the new list item with the details provided in the request
        ListItem savedListItem = listItemRepository.save(ListItem.builder()
                .listId(listId)
                .itemName(itemName)
                .unitPrice(request.unitCost())
                .quantity(request.quantity())
                .inBasket(false)
                .listPosition(nextListPosition)
                .build());

        log.info("Created list item [itemId={}, listId={}, listPosition={}]",
                savedListItem.getItemId(), savedListItem.getListId(), savedListItem.getListPosition());

        // Map the saved list item entity to a response DTO and return it
        return mapToListItemResponseDTO(savedListItem);
    }

    /**
     * Retrieves every item on a shopping list in list-position order.
     *
     * @param listId the shopping list whose items should be retrieved
     *
     * @return the ordered list items
     */
    @Override
    public List<ListItemResponseDTO> getListItems(Long listId) {
        // Retrieve the list items in their persisted queue order and map them to response DTOs
        return listItemRepository.findAllByListIdOrderByListPositionAsc(listId).stream()
                .map(this::mapToListItemResponseDTO)
                .toList();
    }

    /**
     * Deletes an item from a shopping list and closes the resulting position gap.
     *
     * @param listId the shopping list containing the item
     * @param itemId the item to delete
     */
    @Override
    public void deleteListItem(Long listId, Long itemId) {
        // Validate that the item exists on the specified shopping list
        ListItem listItem = getListItemOrThrow(listId, itemId);

        // Delete the item before recalculating the remaining positions
        listItemRepository.delete(listItem);

        // Shift subsequent items forward in Java to keep list positions contiguous
        decrementSubsequentListPositions(listId, listItem.getListPosition());
        log.info("Deleted list item [itemId={}, listId={}, listPosition={}]",
                itemId, listId, listItem.getListPosition());
    }

    /**
     * Toggles whether an existing shopping-list item is in the basket.
     *
     * @param listId the shopping list containing the item
     * @param itemId the item whose in-basket state should be toggled
     */
    @Override
    public void toggleListItemInBasket(Long listId, Long itemId) {
        // Validate that the item exists on the specified shopping list
        ListItem listItem = getListItemOrThrow(listId, itemId);

        // Toggle only the in-basket state and leave the item and list position unchanged
        listItem.setInBasket(!Boolean.TRUE.equals(listItem.getInBasket()));
        listItemRepository.save(listItem);
        log.info("Toggled list-item in-basket state [itemId={}, listId={}, inBasket={}]",
                itemId, listId, listItem.getInBasket());
    }

    /**
     * Reorders all affected items while preserving the relative order of unspecified items.
     *
     * @param listId the shopping list whose items should be reordered
     * @param request the requested item positions
     */
    @Override
    public void reorderListItems(Long listId, List<ReorderListItemRequestDTO> request) {
        // Require at least one requested item position
        if (request == null || request.isEmpty()) {
            throw new BadRequestException("At least one list item position must be provided");
        }

        // Retrieve all items in their current order before validating and calculating the new order
        List<ListItem> currentItems = listItemRepository.findAllByListIdOrderByListPositionAsc(listId);

        // Validate the reorder request against the current items to ensure no duplicates, missing items, or invalid positions
        validateReorderRequest(listId, request, currentItems);

        // Build the new order of items based on the request while preserving the relative order of unspecified items
        List<ListItem> reorderedItems = buildReorderedItems(request, currentItems);

        // Create a list to hold the items whose positions have changed and need to be persisted
        List<ListItem> changedItems = new ArrayList<>();
        // Loop through the list containing the re-ordered items
        for (int index = 0; index < reorderedItems.size(); index++) {

            ListItem item = reorderedItems.get(index);
            // If the item's current list position does not match its new position, update it and add it to the list of changed items
            if (!item.getListPosition().equals(index + 1)) {
                item.setListPosition(index + 1);
                changedItems.add(item);
            }
        }

        // Persist all changed items together within the API request transaction
        if (!changedItems.isEmpty()) {
            listItemRepository.saveAll(changedItems);
        }

        log.info("Reordered shopping-list items [listId={}, requestedPositions={}]", listId, request.size());
    }

    private void validateReorderRequest(
            Long listId,
            List<ReorderListItemRequestDTO> request,
            List<ListItem> currentItems) {

        // Set ensures a unique collection of item IDs and list positions for validation
        Set<Long> itemIds = new HashSet<>();
        Set<Integer> listPositions = new HashSet<>();

        // Get the list of existing item IDs for the specified shopping list
        Set<Long> existingItemIds = currentItems.stream()
                .map(ListItem::getItemId)
                .collect(java.util.stream.Collectors.toSet());

        // Loop through each requested item
        for (ReorderListItemRequestDTO requestedItem : request) {

            // If the item ID or list position is already in the set, throw a BadRequestException
            if (!itemIds.add(requestedItem.itemId())) {
                throw new BadRequestException(String.format(
                        "List item ID [%d] must only be provided once", requestedItem.itemId()));
            }
            // If the list position is already in the set, throw a BadRequestException
            if (!listPositions.add(requestedItem.listPosition())) {
                throw new BadRequestException(String.format(
                        "List position [%d] must only be provided once", requestedItem.listPosition()));
            }
            // If the requested item ID does not exist in the current list, throw a NotFoundException
            if (!existingItemIds.contains(requestedItem.itemId())) {
                throw new NotFoundException(String.format(
                        "List item ID [%d] does not exist on shopping list ID [%d]",
                        requestedItem.itemId(), listId));
            }
            // If the requested list position exceeds the number of items in the current list, throw a BadRequestException
            if (requestedItem.listPosition() > currentItems.size()) {
                throw new BadRequestException(String.format(
                        "List position [%d] exceeds the number of items [%d] on shopping list ID [%d]",
                        requestedItem.listPosition(), currentItems.size(), listId));
            }
        }
    }

    private List<ListItem> buildReorderedItems(
            List<ReorderListItemRequestDTO> request,
            List<ListItem> currentItems
    ) {
        // Hashmap of current items keyed by item ID for quick lookup during reordering
        Map<Long, ListItem> itemsById = new HashMap<>();
        currentItems.forEach(item -> itemsById.put(item.getItemId(), item));

        // Identify the list of ID's for re-ordering
        Set<Long> requestedItemIds = request.stream()
                .map(ReorderListItemRequestDTO::itemId)
                .collect(java.util.stream.Collectors.toSet());

        // Create a new array to hold the reordered items, fixed to the size of the current items list
        ListItem[] reorderedItems = new ListItem[currentItems.size()];

        // Fix every explicitly requested item at its required position
        for (ReorderListItemRequestDTO requestedItem : request) {

            int position = requestedItem.listPosition() - 1;

            // Ensure that the requested position is within the bounds of the current items list
            // Should be impossible to reach this point if the request has been validated prior to invoking this method
            if (position < 0 || position >= reorderedItems.length) {

                // Throw a BadRequestException if the requested position is out of bounds
                throw new BadRequestException(String.format(
                        "List position [%d] is out of bounds for shopping list ID [%d]",
                        requestedItem.listPosition(), requestedItem.itemId()));
            }

            // Place the requested item in the correct position in the reordered items array
            reorderedItems[position] = itemsById.get(requestedItem.itemId());
        }

        // Get the list of items not explicitly requested in the reorder operation
        List<ListItem> unspecifiedItems = currentItems.stream()
                .filter(item -> !requestedItemIds.contains(item.getItemId()))
                .toList();

        int unspecifiedIndex = 0;
        // Loop through the reordered items array and fill in any null positions with the unspecified items in their original order
        for (int index = 0; index < reorderedItems.length; index++) {
            if (reorderedItems[index] == null) {
                // Fill in the null position with the next unspecified item
                reorderedItems[index] =  unspecifiedItems.get(unspecifiedIndex++);
            }
        }

        // Return the reordered items as a list
        return Arrays.asList(reorderedItems);
    }

    private void decrementSubsequentListPositions(Long listId, Integer deletedPosition) {
        // Retrieve subsequent items in queue order using a derived repository operation
        List<ListItem> subsequentItems =
                listItemRepository.findAllByListIdAndListPositionGreaterThanOrderByListPositionAsc(
                        listId, deletedPosition);

        // Update each subsequent item in Java
        for (ListItem subsequentItem : subsequentItems) {
            subsequentItem.setListPosition(subsequentItem.getListPosition() - 1);
        }

        // Persist all position changes together within the API request transaction
        if (!subsequentItems.isEmpty()) {
            listItemRepository.saveAll(subsequentItems);
        }
    }

    private ListItem getListItemOrThrow(Long listId, Long itemId) {
        return listItemRepository.findByItemIdAndListId(itemId, listId)
                .orElseThrow(() -> {
                    log.warn("Rejected list-item operation because the item does not exist on the shopping list "
                            + "[itemId={}, listId={}]", itemId, listId);
                    return new NotFoundException(String.format(
                            "List item ID [%d] does not exist on shopping list ID [%d]", itemId, listId));
                });
    }

    private void validateListItemNameDoesNotExist(Long listId, String itemName) {

        // Ensure that the item name does not already exist in the specified shopping list
        if (listItemRepository.existsByListIdAndItemNameIgnoreCase(listId, itemName)) {
            // Throw a ConflictException if the item name already exists
            log.warn("Rejected list-item creation because the item name already exists on the shopping list "
                    + "[listId={}]", listId);
            throw new ConflictException(String.format(
                    "A list item with the name [%s] already exists on shopping list ID [%d]", itemName, listId));
        }
    }

    private ListItemResponseDTO mapToListItemResponseDTO(ListItem listItem) {
        return new ListItemResponseDTO(
                listItem.getItemId(),
                listItem.getListId(),
                listItem.getItemName(),
                listItem.getUnitPrice(),
                listItem.getQuantity(),
                listItem.getInBasket(),
                listItem.getListPosition()
        );
    }
}
