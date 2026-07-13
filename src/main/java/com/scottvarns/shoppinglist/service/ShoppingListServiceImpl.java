package com.scottvarns.shoppinglist.service;

import com.scottvarns.shoppinglist.dto.request.CreateShoppingListRequestDTO;
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

import java.math.BigDecimal;

@RequiredArgsConstructor
@Service
public class ShoppingListServiceImpl implements ShoppingListService {

    private static final Logger log = LoggerFactory.getLogger(ShoppingListServiceImpl.class);

    private final UserRepository userRepository;

    private final ShoppingListRepository shoppingListRepository;

    /**
     * Creates a new shopping list based on the provided request.
     *
     * @param request The request containing shopping list details.
     * @param callingUserId The User ID of the user making the request.
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
        return mapToResponseDTO(savedShoppingList);
    }


    private ShoppingListResponseDTO mapToResponseDTO(ShoppingList shoppingList) {
        return new ShoppingListResponseDTO(
                shoppingList.getListId(),
                shoppingList.getUserId(),
                shoppingList.getListName(),
                shoppingList.getSpendingLimit(),
                shoppingList.getDate(),
                BigDecimal.ZERO, // Default total cost
                shoppingList.getSpendingLimit(), // Default remaining budget equals spending limit
                false // Default over budget status
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
