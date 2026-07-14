package com.scottvarns.shoppinglist.service;

import com.scottvarns.shoppinglist.dto.request.CreateShoppingListRequestDTO;
import com.scottvarns.shoppinglist.dto.request.CreateListItemRequestDTO;
import com.scottvarns.shoppinglist.dto.request.ReorderListItemRequestDTO;
import com.scottvarns.shoppinglist.dto.response.ListItemResponseDTO;
import com.scottvarns.shoppinglist.dto.response.ShoppingListDetailsResponseDTO;
import com.scottvarns.shoppinglist.dto.response.ShoppingListItemResponseDTO;
import com.scottvarns.shoppinglist.dto.response.ShoppingListResponseDTO;
import com.scottvarns.shoppinglist.exception.ConflictException;
import com.scottvarns.shoppinglist.exception.ForbiddenException;
import com.scottvarns.shoppinglist.exception.NotFoundException;
import com.scottvarns.shoppinglist.entity.ShoppingList;
import com.scottvarns.shoppinglist.exception.UnauthorizedException;
import com.scottvarns.shoppinglist.repository.ShoppingListRepository;
import com.scottvarns.shoppinglist.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@RequiredArgsConstructor
@Service
@Transactional
public class ShoppingListServiceImpl implements ShoppingListService {

    private static final Logger log = LoggerFactory.getLogger(ShoppingListServiceImpl.class);

    private final UserRepository userRepository;

    private final ShoppingListRepository shoppingListRepository;

    private final ListItemService listItemService;

    /**
     * Creates a new shopping list based on the provided request.
     *
     * @param request The request containing shopping list details.
     * @param callingUserId The User ID of the user making the request.
     *
     * @return The response DTO representing the created shopping list.
     */
    @Override
    public ShoppingListResponseDTO createShoppingList(CreateShoppingListRequestDTO request, Long callingUserId) {

        // Validate user access and check if the user exists
        validateUserAccessAndCheckUserExists(request.userId(), callingUserId);

        // Ensure no shopping list exists for the same user ID and list name combination
        if (shoppingListRepository.existsByUserIdAndListNameIgnoreCase(request.userId(), request.listName().trim())) {
            log.warn("Rejected shopping-list creation because the list name already exists [userId={}]", request.userId());
            throw new ConflictException(String.format("A shopping list with the name [%s] already exists for user ID [%d]", request.listName(), request.userId()));
        }

        // Create and save the new shopping list
        ShoppingList newShoppingList = ShoppingList.builder()
                .userId(request.userId())
                .listName(request.listName().trim())
                .spendingLimit(request.spendingLimit())
                .date(request.date())
                .build();

        ShoppingList savedShoppingList = shoppingListRepository.save(newShoppingList);

        log.info("Created shopping list [listId={}, userId={}]", savedShoppingList.getListId(), savedShoppingList.getUserId());
        // Return the response DTO
        return mapToCreateListResponseDTO(savedShoppingList);
    }

    /**
     * Retrieves a shopping list, its ordered items and its calculated budget information.
     *
     * @param listId the shopping list to retrieve
     * @param callingUserId the ID extracted from the authenticated request
     *
     * @return the shopping list details
     */
    @Override
    @Transactional(readOnly = true)
    public ShoppingListDetailsResponseDTO getShoppingList(Long listId, Long callingUserId) {
        // Validate that the shopping list exists
        ShoppingList shoppingList = getShoppingListOrThrow(listId);

        // Validate that the calling user exists and has access to the shopping list
        validateUserAccessAndCheckUserExists(shoppingList.getUserId(), callingUserId);

        // Retrieve the ordered list items using the ListItemService
        List<ListItemResponseDTO> listItems = listItemService.getListItems(listId);

        // Calculate the total cost, remaining budget, and over-budget status
        BigDecimal totalCost = calculateTotalCost(listItems);

        BigDecimal remainingBudget = shoppingList.getSpendingLimit() == null
                ? null
                : shoppingList.getSpendingLimit().subtract(totalCost);

        boolean overBudget = shoppingList.getSpendingLimit() != null
                && totalCost.compareTo(shoppingList.getSpendingLimit()) > 0;

        // Build the combined list response without repeating the list ID on every item
        return mapToShoppingListDetailsResponseDTO(
                shoppingList, listItems, totalCost, remainingBudget, overBudget);
    }

    /**
     * Validates list-level access and appends a new item to the shopping list.
     *
     * @param listId the shopping list to receive the item
     * @param request the new item's details
     * @param callingUserId the ID extracted from the authenticated request
     *
     * @return the created list item
     */
    @Override
    public ListItemResponseDTO createListItem(Long listId, CreateListItemRequestDTO request, Long callingUserId) {
        // Validate that the shopping list exists
        ShoppingList shoppingList = getShoppingListOrThrow(listId);

        // Validate that the calling user exists and has access to the shopping list
        validateUserAccessAndCheckUserExists(shoppingList.getUserId(), callingUserId);

        // Create the list item using the ListItemService
        return listItemService.createListItem(listId, request);
    }

    /**
     * Validates list-level access and deletes an item from the shopping list.
     *
     * @param listId the shopping list containing the item
     * @param itemId the item to delete
     * @param callingUserId the ID extracted from the authenticated request
     */
    @Override
    public void deleteListItem(Long listId, Long itemId, Long callingUserId) {
        // Validate that the shopping list exists
        ShoppingList shoppingList = getShoppingListOrThrow(listId);

        // Validate that the calling user exists and has access to the shopping list
        validateUserAccessAndCheckUserExists(shoppingList.getUserId(), callingUserId);

        // Delete the item and reorder the remaining items using the ListItemService
        listItemService.deleteListItem(listId, itemId);
    }

    /**
     * Validates list-level access and toggles whether an item is in the basket.
     *
     * @param listId the shopping list containing the item
     * @param itemId the item whose in-basket state should be toggled
     * @param callingUserId the ID extracted from the authenticated request
     */
    @Override
    public void toggleListItemInBasket(Long listId, Long itemId, Long callingUserId) {
        // Validate that the shopping list exists
        ShoppingList shoppingList = getShoppingListOrThrow(listId);

        // Validate that the calling user exists and has access to the shopping list
        validateUserAccessAndCheckUserExists(shoppingList.getUserId(), callingUserId);

        // Toggle the item's in-basket state using the ListItemService
        listItemService.toggleListItemInBasket(listId, itemId);
    }

    /**
     * Validates list-level access and reorders items on a shopping list.
     *
     * @param listId the shopping list whose items should be reordered
     * @param request the requested item positions
     * @param callingUserId the ID extracted from the authenticated request
     */
    @Override
    public void reorderListItems(
            Long listId,
            List<ReorderListItemRequestDTO> request,
            Long callingUserId
    ) {
        // Validate that the shopping list exists
        ShoppingList shoppingList = getShoppingListOrThrow(listId);

        // Validate that the calling user exists and has access to the shopping list
        validateUserAccessAndCheckUserExists(shoppingList.getUserId(), callingUserId);

        // Reorder the list items using the ListItemService
        listItemService.reorderListItems(listId, request);
    }

    private ShoppingList getShoppingListOrThrow(Long listId) {
        ShoppingList shoppingList = shoppingListRepository.findByListId(listId);
        if (shoppingList == null) {
            // If the shopping list does not exist, throw a NotFoundException
            log.warn("Rejected shopping-list operation because the shopping list does not exist [listId={}]", listId);
            throw new NotFoundException(String.format("Shopping list ID [%d] does not exist", listId));
        }

        return shoppingList;
    }

    private ShoppingListResponseDTO mapToCreateListResponseDTO(ShoppingList shoppingList) {
        return new ShoppingListResponseDTO(
                shoppingList.getListId(),
                shoppingList.getUserId(),
                shoppingList.getListName(),
                shoppingList.getSpendingLimit(),
                shoppingList.getDate(),
                BigDecimal.ZERO, // Default total cost
                shoppingList.getSpendingLimit(), // Default remaining budget equals spending limit as list is empty when created
                false // Default over budget status
        );
    }

    private BigDecimal calculateTotalCost(List<ListItemResponseDTO> listItems) {
        // Calculate the total cost by summing the product of unit cost and quantity for each list item
        return listItems.stream()
                .map(listItem -> listItem.unitCost().multiply(BigDecimal.valueOf(listItem.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private ShoppingListDetailsResponseDTO mapToShoppingListDetailsResponseDTO(
            ShoppingList shoppingList,
            List<ListItemResponseDTO> listItems,
            BigDecimal totalCost,
            BigDecimal remainingBudget,
            boolean overBudget
    ) {
        List<ShoppingListItemResponseDTO> itemResponses = listItems.stream()
                .map(listItem -> new ShoppingListItemResponseDTO(
                        listItem.itemId(),
                        listItem.itemName(),
                        listItem.unitCost(),
                        listItem.quantity(),
                        listItem.inBasket(),
                        listItem.listPosition()))
                .toList();

        return new ShoppingListDetailsResponseDTO(
                shoppingList.getListId(),
                shoppingList.getUserId(),
                shoppingList.getListName(),
                shoppingList.getSpendingLimit(),
                shoppingList.getDate(),
                totalCost,
                remainingBudget,
                overBudget,
                itemResponses
        );
    }

    private void validateUserAccessAndCheckUserExists(Long userId, Long callingUserId) {
        // Null safety check on callingUserId
        if (callingUserId == null || callingUserId <= 0) {
            log.warn("Rejected shopping-list creation because the calling user is not authenticated");
            throw new UnauthorizedException("Authorization failed: calling user ID is invalid or not provided.");
        }

        // Check if the calling user ID matches the user ID in the request
        if (!userId.equals(callingUserId)) {
            log.warn("Rejected shopping-list creation because the authenticated user does not own the requested list [callingUserId={}, requestedUserId={}]", callingUserId, userId);
            throw new ForbiddenException(String.format("User ID [%d] is not authorized to create a shopping list for user ID [%d]", callingUserId, userId));
        }

        // Ensure the user exists
        if (!userRepository.existsByUserId(userId)) {
            log.warn("Rejected shopping-list creation because the requested user does not exist [userId={}]", userId);
            throw new NotFoundException(String.format("User ID [%d] does not exist", userId));
        }
    }

}
