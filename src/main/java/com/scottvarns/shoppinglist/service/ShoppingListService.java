package com.scottvarns.shoppinglist.service;

import com.scottvarns.shoppinglist.dto.request.CreateShoppingListRequestDTO;
import com.scottvarns.shoppinglist.dto.response.ShoppingListResponseDTO;
import com.scottvarns.shoppinglist.exception.BadRequestException;
import com.scottvarns.shoppinglist.exception.ConflictException;
import com.scottvarns.shoppinglist.exception.ForbiddenException;
import com.scottvarns.shoppinglist.exception.NotFoundException;

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

}
