package com.scottvarns.shoppinglist.service;

import com.scottvarns.shoppinglist.dto.request.CreateShoppingListRequestDTO;
import com.scottvarns.shoppinglist.dto.request.CreateListItemRequestDTO;
import com.scottvarns.shoppinglist.dto.request.ReorderListItemRequestDTO;
import com.scottvarns.shoppinglist.dto.response.ListItemResponseDTO;
import com.scottvarns.shoppinglist.dto.response.ShoppingListDetailsResponseDTO;
import com.scottvarns.shoppinglist.dto.response.ShoppingListResponseDTO;
import com.scottvarns.shoppinglist.entity.ShoppingList;
import com.scottvarns.shoppinglist.exception.ConflictException;
import com.scottvarns.shoppinglist.exception.ForbiddenException;
import com.scottvarns.shoppinglist.exception.NotFoundException;
import com.scottvarns.shoppinglist.exception.UnauthorizedException;
import com.scottvarns.shoppinglist.repository.ShoppingListRepository;
import com.scottvarns.shoppinglist.repository.UserRepository;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@TestMethodOrder(MethodOrderer.MethodName.class)
@ExtendWith(MockitoExtension.class)
class ShoppingListServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ShoppingListRepository shoppingListRepository;

    @Mock
    private ListItemService listItemService;

    @InjectMocks
    private ShoppingListServiceImpl shoppingListService;

    /**
     * When the createShoppingList method is called with a valid CreateShoppingListRequestDTO is provided, and the list name does not already exist for that user.
     * Then ensure a new shopping list is created for that user with the provided list name, date & spending limit.
     */
    @Test
    void test01_createShoppingList_whenValidDTOProvided_thenCreateNewList() {
        //Setup a valid CreateShoppingListRequestDTO with a user ID, list name, spending limit, and date.
        CreateShoppingListRequestDTO request = new CreateShoppingListRequestDTO(
                1L,
                "Weekly Shop",
                new BigDecimal("75.00"),
                LocalDate.of(2026, 7, 18)
        );

        //Mock the ShoppingList object that would be returned by the repository when saving a new shopping list.
        ShoppingList savedShoppingList = ShoppingList.builder()
                .listId(12L)
                .userId(1L)
                .listName("Weekly Shop")
                .spendingLimit(new BigDecimal("100.00"))
                .date(LocalDate.of(2026, 7, 13))
                .build();

        //Mock JPA respository methods
        when(userRepository.existsByUserId(1L))
                .thenReturn(true);

        when(shoppingListRepository
                .existsByUserIdAndListNameIgnoreCase(
                        1L,
                        "Weekly Shop")
        )
                .thenReturn(false);

        when(shoppingListRepository.save(any(ShoppingList.class)))
                .thenReturn(savedShoppingList);

        // Invoke the service method to create a shopping list
        ShoppingListResponseDTO response =
                shoppingListService.createShoppingList(request, 1L);

        // Assert values are as expected
        assertThat(response.listId()).isEqualTo(12L);
        assertThat(response.userId()).isEqualTo(1L);
        assertThat(response.listName()).isEqualTo("Weekly Shop");
        assertThat(response.spendingLimit())
                .isEqualByComparingTo("100.00");
        assertThat(response.date())
                .isEqualTo(LocalDate.of(2026, 7, 13));
        assertThat(response.totalCost())
                .isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.overBudget()).isFalse();

        verify(userRepository).existsByUserId(1L);

        verify(shoppingListRepository)
                .existsByUserIdAndListNameIgnoreCase(
                        1L,
                        "Weekly Shop"
                );

        //Ensure the save method was called on the repository with a ShoppingList object
        verify(shoppingListRepository)
                .save(any(ShoppingList.class));
    }

    /**
     * When the createShoppingList method is called with a valid CreateShoppingListRequestDTO is provided with leading and trailing whitespace in the list name.
     * Then ensure the list name is trimmed before saving to the database.
     */
    @Test
    void test02_createShoppingList_shouldTrimListNameBeforeSaving() {
        // Setup a valid CreateShoppingListRequestDTO with leading and trailing whitespace in the list name.
        CreateShoppingListRequestDTO request = new CreateShoppingListRequestDTO(
                1L,
                "  Weekly Shop  ",
                new BigDecimal("75.00"),
                LocalDate.of(2026, 7, 18)
        );

        // Mock the ShoppingList object that would be returned by the repository when saving a new shopping list.
        ShoppingList savedShoppingList = ShoppingList.builder()
                .listId(12L)
                .userId(1L)
                .listName("Weekly Shop")
                .spendingLimit(new BigDecimal("75.00"))
                .date(LocalDate.of(2026, 7, 18))
                .build();

        //Mock JPA repository methods
        when(userRepository.existsByUserId(1L))
                .thenReturn(true);

        when(
                shoppingListRepository.existsByUserIdAndListNameIgnoreCase(
                        1L,
                        "Weekly Shop"
                )
        ).thenReturn(false);

        when(shoppingListRepository.save(
                any(ShoppingList.class)
        )).thenReturn(savedShoppingList);

        // Invoke the service method to create a shopping list
        ShoppingListResponseDTO response = shoppingListService.createShoppingList(request, 1L);

        // Assert values are as expected
        assertThat(response.listId()).isEqualTo(12L);
        assertThat(response.userId()).isEqualTo(1L);
        assertThat(response.listName()).isEqualTo("Weekly Shop");
        assertThat(response.spendingLimit())
                .isEqualByComparingTo("75.00");
        assertThat(response.date())
                .isEqualTo(LocalDate.of(2026, 7, 18));
        assertThat(response.totalCost())
                .isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.overBudget()).isFalse();

        verify(userRepository).existsByUserId(1L);

        verify(shoppingListRepository)
                .existsByUserIdAndListNameIgnoreCase(
                        1L,
                        "Weekly Shop"
                );

        //Ensure the save method was called on the repository with a ShoppingList object
        verify(shoppingListRepository)
                .save(any(ShoppingList.class));
    }

    /**
     * When the createShoppingList method is called with a CreateShoppingListRequestDTO is provided with a user ID that does not exist in the database.
     * Then ensure a NotFoundException is thrown with the appropriate message.
     */
    @Test
    void test03_createShoppingList_whenUserDoesNotExist_thenEnsureNotFoundExceptionThrown() {
        // Setup a CreateShoppingListRequestDTO with a user ID that does not exist in the database.
        CreateShoppingListRequestDTO request = new CreateShoppingListRequestDTO(
                999L,
                "Weekly Shop",
                new BigDecimal("75.00"),
                LocalDate.of(2026, 7, 18)
        );

        // Mock the userRepository to return false when checking if the user exists.
        when(userRepository.existsByUserId(999L))
                .thenReturn(false);

        // Invoke the service method and assert that a NotFoundException is thrown with the expected message.
        assertThatThrownBy(
                () -> shoppingListService.createShoppingList(request, 999L)
        )
                .isInstanceOf(NotFoundException.class)
                .hasMessage("User ID [999] does not exist");

        // Verify that the userRepository was called to check if the user exists, and that no interactions occurred with the shoppingListRepository.
        verify(userRepository).existsByUserId(999L);
        verifyNoInteractions(shoppingListRepository);
    }

    /**
     * When the createShoppingList method is called with a CreateShoppingListRequestDTO is provided for a different user ID to the user ID that made the request.
     * Then ensure a ForbiddenException is thrown with the appropriate message.
     */
    @Test
    void test04_whenUserIdInRequestDoesNotMatchCallingUserId_thenEnsureForbiddenExceptionThrown() {
        // Setup a CreateShoppingListRequestDTO with a user ID that does not match the calling user ID.
        CreateShoppingListRequestDTO request = new CreateShoppingListRequestDTO(
                2L,
                "Weekly Shop",
                new BigDecimal("75.00"),
                LocalDate.of(2026, 7, 18)
        );

        // Invoke the service method and assert that a ForbiddenException is thrown with the expected message.
        assertThatThrownBy(
                () -> shoppingListService.createShoppingList(request, 1L)
        )
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("User ID [1] is not authorized to create a shopping list for user ID [2]");

        // Verify that no interactions occurred with the userRepository or shoppingListRepository since the user ID check failed before any database calls.
        verifyNoInteractions(userRepository);
        verifyNoInteractions(shoppingListRepository);
    }

    /**
     * When the createShoppingList method is called with a CreateShoppingListRequestDTO is provided with a list name that already exists for the user.
     * Then ensure a ConflictException is thrown with the appropriate message.
     */
    @Test
    void test05_createShoppingList_whenListNameAlreadyExists_thenEnsureConflictExceptionThrown() {
        // Setup a CreateShoppingListRequestDTO with a list name that already exists for the user.
        CreateShoppingListRequestDTO request = new CreateShoppingListRequestDTO(
                1L,
                "Weekly Shop",
                new BigDecimal("75.00"),
                LocalDate.of(2026, 7, 18)
        );

        // Mock JPA repository methods to simulate that the user exists and the list name already exists for that user.
        when(userRepository.existsByUserId(1L))
                .thenReturn(true);

        when(
                shoppingListRepository.existsByUserIdAndListNameIgnoreCase(
                        1L,
                        "Weekly Shop"
                )
        ).thenReturn(true);

        // Invoke the service method and assert that a ConflictException is thrown with the expected message.
        assertThatThrownBy(
                () -> shoppingListService.createShoppingList(request, 1L)
        )
                .isInstanceOf(ConflictException.class)
                .hasMessage(
                        "A shopping list with the name [Weekly Shop] already exists for user ID [1]"
                );

        // Verify that the userRepository was called to check if the user exists and that the shoppingListRepository was called to check if the list name already exists for that user.
        verify(userRepository).existsByUserId(1L);

        verify(shoppingListRepository)
                .existsByUserIdAndListNameIgnoreCase(
                        1L,
                        "Weekly Shop"
                );

        // Verify that the save method was never called on the shoppingListRepository since the list name already exists for that user.
        verify(shoppingListRepository, never())
                .save(any(ShoppingList.class));
    }

    /**
     * When the createShoppingList method is called with a null callingUserId.
     * Then ensure an UnauthorizedException is thrown with the appropriate message.
     */
    @Test
    void test06_createShoppingList_whenCallingUserIdIsNull_thenEnsureUnauthorizedExceptionThrown() {
        // Setup a CreateShoppingListRequestDTO with a valid user ID and list name.
        CreateShoppingListRequestDTO request = new CreateShoppingListRequestDTO(
                1L,
                "Weekly Shop",
                new BigDecimal("75.00"),
                LocalDate.of(2026, 7, 18)
        );

        // Invoke the service method and assert that an UnauthorizedException is thrown with the expected message.
        assertThatThrownBy(
                () -> shoppingListService.createShoppingList(request, null)
        )
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Authorization failed: calling user ID is invalid or not provided.");

        // Verify that no interactions occurred with the userRepository or shoppingListRepository since the callingUserId check failed before any database calls.
        verifyNoInteractions(userRepository);
        verifyNoInteractions(shoppingListRepository);

    }

    /**
     * When the createShoppingList method is called with a callingUserId with a value of 0.
     * Then ensure an UnauthorizedException is thrown with the appropriate message.
     */
    @Test
    void test07_createShoppingList_whenCallingUserIdIs0_thenEnsureUnauthorizedExceptionThrown() {
        // Setup a CreateShoppingListRequestDTO with a valid user ID and list name.
        CreateShoppingListRequestDTO request = new CreateShoppingListRequestDTO(
                1L,
                "Weekly Shop",
                new BigDecimal("75.00"),
                LocalDate.of(2026, 7, 18)
        );

        // Invoke the service method and assert that an UnauthorizedException is thrown with the expected message.
        assertThatThrownBy(
                () -> shoppingListService.createShoppingList(request, 0L)
        )
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Authorization failed: calling user ID is invalid or not provided.");

        // Verify that no interactions occurred with the userRepository or shoppingListRepository since the callingUserId check failed before any database calls.
        verifyNoInteractions(userRepository);
        verifyNoInteractions(shoppingListRepository);

    }

    /**
     * When a valid list-item request is made by the list owner
     * Then validate the list and delegate item creation to the list-item service.
     */
    @Test
    void test08_createListItem_whenValidOwnerRequestProvided_thenDelegatesToListItemService() {
        // Setup a valid CreateListItemRequestDTO with an item name, price, and quantity.
        CreateListItemRequestDTO request = new CreateListItemRequestDTO(
                "Milk",
                new BigDecimal("1.25"),
                2
        );

        // Mock the expected ListItemResponseDTO that would be returned by the list-item service when creating a new list item.
        ListItemResponseDTO expectedResponse = new ListItemResponseDTO(
                44L,
                12L,
                "Milk",
                new BigDecimal("1.25"),
                2,
                false,
                1
        );

        // Mock the shoppingListRepository to return a ShoppingList object when finding by list ID, and mock the userRepository to return true when checking if the user exists.
        when(shoppingListRepository.findByListId(12L))
                .thenReturn(ShoppingList.builder().listId(12L).userId(1L).build());
        when(userRepository.existsByUserId(1L)).thenReturn(true);
        when(listItemService.createListItem(12L, request)).thenReturn(expectedResponse);

        // Invoke the service method to create a list item and capture the response
        ListItemResponseDTO response = shoppingListService.createListItem(12L, request, 1L);

        // Assert that the response matches the expected ListItemResponseDTO
        assertThat(response).isEqualTo(expectedResponse);

        // Verify that the shoppingListRepository was called to find the shopping list by list ID, the userRepository was called to check if the user exists, and the listItemService was called to create the list item.
        verify(shoppingListRepository).findByListId(12L);
        verify(userRepository).existsByUserId(1L);
        verify(listItemService).createListItem(12L, request);
    }

    /**
     * When the requested shopping list does not exist
     * Then reject item creation before calling the list-item service.
     */
    @Test
    void test09_createListItem_whenListDoesNotExist_thenThrowsNotFoundException() {
        // Mock the shoppingListRepository to return null when finding by list ID, simulating that the shopping list does not exist.
        when(shoppingListRepository.findByListId(12L)).thenReturn(null);

        // Assert that calling the service method to create a list item throws a NotFoundException with the expected message.
        assertThatThrownBy(() -> shoppingListService.createListItem(
                12L,
                new CreateListItemRequestDTO(
                        "Milk", new BigDecimal("1.25"), 2), 1L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Shopping list ID [12] does not exist");

        // Verify that no interactions occurred with the userRepository or listItemService
        verifyNoInteractions(userRepository, listItemService);
    }

    /**
     * When the shopping list owner does not exist
     * Then reject item creation before calling the list-item service.
     */
    @Test
    void test10_createListItem_whenListOwnerDoesNotExist_thenThrowsNotFoundException() {
        // Mock the shoppingListRepository to return a ShoppingList object when finding by list ID, and mock the userRepository to return false when checking if the user exists.
        when(shoppingListRepository.findByListId(12L))
                .thenReturn(ShoppingList.builder().listId(12L).userId(1L).build());
        when(userRepository.existsByUserId(1L)).thenReturn(false);

        // Assert that calling the service method to create a list item throws a NotFoundException with the expected message.
        assertThatThrownBy(() -> shoppingListService.createListItem(
                12L, new CreateListItemRequestDTO("Milk", new BigDecimal("1.25"), 2), 1L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("User ID [1] does not exist");

        // Verify that no interactions occurred with the listItemService
        verifyNoInteractions(listItemService);
    }

    /**
     * When the authenticated user does not own the shopping list
     * Then reject item creation before checking the user or creating an item.
     */
    @Test
    void test11_createListItem_whenAuthenticatedUserDoesNotOwnList_thenThrowsForbiddenException() {
        // Mock the shoppingListRepository to return a ShoppingList object when finding by list ID, simulating that the shopping list is owned by user ID 1.
        when(shoppingListRepository.findByListId(12L))
                .thenReturn(ShoppingList.builder().listId(12L).userId(1L).build());

        // Assert that calling the service method to create a list item with a different authenticated user ID throws a ForbiddenException with the expected message.
        assertThatThrownBy(() -> shoppingListService.createListItem(
                12L, new CreateListItemRequestDTO("Milk", new BigDecimal("1.25"), 2), 2L))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("User ID [2] is not authorized to create a shopping list for user ID [1]");

        // Verify that no interactions occurred with the userRepository or listItemService
        verifyNoInteractions(userRepository, listItemService);
    }

    /**
     * When item creation has no authenticated user ID
     * Then reject it before checking the owner or creating an item.
     */
    @Test
    void test12_createListItem_whenCallingUserIdIsNull_thenThrowsUnauthorizedException() {
        // Mock the shoppingListRepository to return a ShoppingList object when finding by list ID, simulating that the shopping list is owned by user ID 1.
        when(shoppingListRepository.findByListId(12L))
                .thenReturn(ShoppingList.builder().listId(12L).userId(1L).build());

        // Assert that calling the service method to create a list item with a null authenticated user ID throws an UnauthorizedException with the expected message.
        assertThatThrownBy(() -> shoppingListService.createListItem(
                12L, new CreateListItemRequestDTO("Milk", new BigDecimal("1.25"), 2), null))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Authorization failed: calling user ID is invalid or not provided.");

        // Verify that no interactions occurred with the userRepository or listItemService since the callingUserId check failed before any database calls.
        verifyNoInteractions(userRepository, listItemService);
    }

    /**
     * When item creation has a non-positive authenticated user ID
     * Then reject it before checking the owner or creating an item.
     */
    @Test
    void test13_createListItem_whenCallingUserIdIsZero_thenThrowsUnauthorizedException() {
        // Mock the shoppingListRepository to return a ShoppingList object when finding by list ID, simulating that the shopping list is owned by user ID 1.
        when(shoppingListRepository.findByListId(12L))
                .thenReturn(ShoppingList.builder().listId(12L).userId(1L).build());

        // Assert that calling the service method to create a list item with a non-positive authenticated user ID throws an UnauthorizedException with the expected message.
        assertThatThrownBy(() -> shoppingListService.createListItem(
                12L, new CreateListItemRequestDTO("Milk", new BigDecimal("1.25"), 2), 0L))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Authorization failed: calling user ID is invalid or not provided.");

        // Verify that no interactions occurred with the userRepository or listItemService
        verifyNoInteractions(userRepository, listItemService);
    }

    /**
     * When a list owner deletes an item from an existing shopping list
     * Then validate list access and delegate deletion to the list-item service.
     */
    @Test
    void test14_deleteListItem_whenValidOwnerRequestProvided_thenDelegatesToListItemService() {
        // Mock an existing shopping list and its associated user
        when(shoppingListRepository.findByListId(12L))
                .thenReturn(ShoppingList.builder().listId(12L).userId(1L).build());
        when(userRepository.existsByUserId(1L)).thenReturn(true);

        // Invoke the service method to delete the list item
        shoppingListService.deleteListItem(12L, 44L, 1L);

        // Verify list lookup, user validation, and delegation to the list-item service
        verify(shoppingListRepository).findByListId(12L);
        verify(userRepository).existsByUserId(1L);
        verify(listItemService).deleteListItem(12L, 44L);
    }

    /**
     * When item deletion targets a shopping list that does not exist
     * Then reject deletion before checking the user or list item.
     */
    @Test
    void test15_deleteListItem_whenListDoesNotExist_thenThrowsNotFoundException() {
        // Mock the shopping-list lookup to return no list
        when(shoppingListRepository.findByListId(12L)).thenReturn(null);

        // Assert that deletion fails with the expected not-found response
        assertThatThrownBy(() -> shoppingListService.deleteListItem(12L, 44L, 1L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Shopping list ID [12] does not exist");

        // Verify that no user or list-item operations were attempted
        verifyNoInteractions(userRepository, listItemService);
    }

    /**
     * When the user associated with the shopping list does not exist
     * Then reject deletion before calling the list-item service.
     */
    @Test
    void test16_deleteListItem_whenListOwnerDoesNotExist_thenThrowsNotFoundException() {
        // Mock an existing list whose associated user cannot be found
        when(shoppingListRepository.findByListId(12L))
                .thenReturn(ShoppingList.builder().listId(12L).userId(1L).build());
        when(userRepository.existsByUserId(1L)).thenReturn(false);

        // Assert that deletion fails with the expected user not-found response
        assertThatThrownBy(() -> shoppingListService.deleteListItem(12L, 44L, 1L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("User ID [1] does not exist");

        // Verify that item deletion was not attempted
        verifyNoInteractions(listItemService);
    }

    /**
     * When the authenticated user does not own the shopping list
     * Then reject deletion before checking the user or list item.
     */
    @Test
    void test17_deleteListItem_whenAuthenticatedUserDoesNotOwnList_thenThrowsForbiddenException() {
        // Mock a shopping list owned by a different user
        when(shoppingListRepository.findByListId(12L))
                .thenReturn(ShoppingList.builder().listId(12L).userId(1L).build());

        // Assert that deletion fails with the expected forbidden response
        assertThatThrownBy(() -> shoppingListService.deleteListItem(12L, 44L, 2L))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("User ID [2] is not authorized to create a shopping list for user ID [1]");

        // Verify that no user or list-item operations were attempted
        verifyNoInteractions(userRepository, listItemService);
    }

    /**
     * When item deletion has no authenticated user ID
     * Then reject it before checking the owner or list item.
     */
    @Test
    void test18_deleteListItem_whenCallingUserIdIsNull_thenThrowsUnauthorizedException() {
        // Mock the shopping list targeted by the deletion request
        when(shoppingListRepository.findByListId(12L))
                .thenReturn(ShoppingList.builder().listId(12L).userId(1L).build());

        // Assert that deletion fails with the expected unauthorized response
        assertThatThrownBy(() -> shoppingListService.deleteListItem(12L, 44L, null))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Authorization failed: calling user ID is invalid or not provided.");

        // Verify that no user or list-item operations were attempted
        verifyNoInteractions(userRepository, listItemService);
    }

    /**
     * When item deletion has a non-positive authenticated user ID
     * Then reject it before checking the owner or list item.
     */
    @Test
    void test19_deleteListItem_whenCallingUserIdIsZero_thenThrowsUnauthorizedException() {
        // Mock the shopping list targeted by the deletion request
        when(shoppingListRepository.findByListId(12L))
                .thenReturn(ShoppingList.builder().listId(12L).userId(1L).build());

        // Assert that deletion fails with the expected unauthorized response
        assertThatThrownBy(() -> shoppingListService.deleteListItem(12L, 44L, 0L))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Authorization failed: calling user ID is invalid or not provided.");

        // Verify that no user or list-item operations were attempted
        verifyNoInteractions(userRepository, listItemService);
    }

    /**
     * When a list owner toggles an item's in-basket state
     * Then validate list access and delegate the item update to the list-item service.
     */
    @Test
    void test20_toggleListItemInBasket_whenValidOwnerRequestProvided_thenDelegatesToListItemService() {
        // Mock an existing shopping list and its associated user
        when(shoppingListRepository.findByListId(12L))
                .thenReturn(ShoppingList.builder().listId(12L).userId(1L).build());
        when(userRepository.existsByUserId(1L)).thenReturn(true);

        // Invoke the service method to toggle the item's in-basket state
        shoppingListService.toggleListItemInBasket(12L, 44L, 1L);

        // Verify list lookup, user validation, and delegation to the list-item service
        verify(shoppingListRepository).findByListId(12L);
        verify(userRepository).existsByUserId(1L);
        verify(listItemService).toggleListItemInBasket(12L, 44L);
    }

    /**
     * When the basket update targets a shopping list that does not exist
     * Then reject the update before checking the user or list item.
     */
    @Test
    void test21_toggleListItemInBasket_whenListDoesNotExist_thenThrowsNotFoundException() {
        // Mock the shopping-list lookup to return no list
        when(shoppingListRepository.findByListId(12L)).thenReturn(null);

        // Assert that the update fails with the expected not-found response
        assertThatThrownBy(() -> shoppingListService.toggleListItemInBasket(12L, 44L, 1L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Shopping list ID [12] does not exist");

        // Verify that no user or list-item operations were attempted
        verifyNoInteractions(userRepository, listItemService);
    }

    /**
     * When the user associated with the shopping list does not exist
     * Then reject the basket update before calling the list-item service.
     */
    @Test
    void test22_toggleListItemInBasket_whenListOwnerDoesNotExist_thenThrowsNotFoundException() {
        // Mock an existing list whose associated user cannot be found
        when(shoppingListRepository.findByListId(12L))
                .thenReturn(ShoppingList.builder().listId(12L).userId(1L).build());
        when(userRepository.existsByUserId(1L)).thenReturn(false);

        // Assert that the update fails with the expected user not-found response
        assertThatThrownBy(() -> shoppingListService.toggleListItemInBasket(12L, 44L, 1L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("User ID [1] does not exist");

        // Verify that the list-item service was not called
        verifyNoInteractions(listItemService);
    }

    /**
     * When the authenticated user does not own the shopping list
     * Then reject the basket update before checking the user or list item.
     */
    @Test
    void test23_toggleListItemInBasket_whenAuthenticatedUserDoesNotOwnList_thenThrowsForbiddenException() {
        // Mock a shopping list owned by a different user
        when(shoppingListRepository.findByListId(12L))
                .thenReturn(ShoppingList.builder().listId(12L).userId(1L).build());

        // Assert that the update fails with the expected forbidden response
        assertThatThrownBy(() -> shoppingListService.toggleListItemInBasket(12L, 44L, 2L))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("User ID [2] is not authorized to create a shopping list for user ID [1]");

        // Verify that no user or list-item operations were attempted
        verifyNoInteractions(userRepository, listItemService);
    }

    /**
     * When the basket update has no authenticated user ID
     * Then reject it before checking the owner or list item.
     */
    @Test
    void test24_toggleListItemInBasket_whenCallingUserIdIsNull_thenThrowsUnauthorizedException() {
        // Mock the shopping list targeted by the update request
        when(shoppingListRepository.findByListId(12L))
                .thenReturn(ShoppingList.builder().listId(12L).userId(1L).build());

        // Assert that the update fails with the expected unauthorized response
        assertThatThrownBy(() -> shoppingListService.toggleListItemInBasket(12L, 44L, null))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Authorization failed: calling user ID is invalid or not provided.");

        // Verify that no user or list-item operations were attempted
        verifyNoInteractions(userRepository, listItemService);
    }

    /**
     * When the basket update has a non-positive authenticated user ID
     * Then reject it before checking the owner or list item.
     */
    @Test
    void test25_toggleListItemInBasket_whenCallingUserIdIsZero_thenThrowsUnauthorizedException() {
        // Mock the shopping list targeted by the update request
        when(shoppingListRepository.findByListId(12L))
                .thenReturn(ShoppingList.builder().listId(12L).userId(1L).build());

        // Assert that the update fails with the expected unauthorized response
        assertThatThrownBy(() -> shoppingListService.toggleListItemInBasket(12L, 44L, 0L))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Authorization failed: calling user ID is invalid or not provided.");

        // Verify that no user or list-item operations were attempted
        verifyNoInteractions(userRepository, listItemService);
    }

    /**
     * When an owner retrieves a list whose total cost is below its spending limit
     * Then return its ordered items and calculated remaining budget with overBudget set to false.
     */
    @Test
    void test26_getShoppingList_whenListIsUnderBudget_thenReturnsCalculatedListDetails() {
        // Mock an existing list, owner and ordered list items with a combined cost of 9.00
        ShoppingList shoppingList = buildShoppingList(new BigDecimal("20.00"));
        List<ListItemResponseDTO> listItems = List.of(
                new ListItemResponseDTO(44L, 12L, "Milk", new BigDecimal("3.50"), 2, false, 1),
                new ListItemResponseDTO(45L, 12L, "Bread", new BigDecimal("2.00"), 1, true, 2)
        );
        when(shoppingListRepository.findByListId(12L)).thenReturn(shoppingList);
        when(userRepository.existsByUserId(1L)).thenReturn(true);
        when(listItemService.getListItems(12L)).thenReturn(listItems);

        // Retrieve the shopping list details
        ShoppingListDetailsResponseDTO response = shoppingListService.getShoppingList(12L, 1L);

        // Assert the list data, calculated budget values and nested item projection
        assertThat(response.listId()).isEqualTo(12L);
        assertThat(response.userId()).isEqualTo(1L);
        assertThat(response.listName()).isEqualTo("Weekly Shop");
        assertThat(response.spendingLimit()).isEqualByComparingTo("20.00");
        assertThat(response.date()).isEqualTo(LocalDate.of(2026, 7, 18));
        assertThat(response.totalCost()).isEqualByComparingTo("9.00");
        assertThat(response.remainingBudget()).isEqualByComparingTo("11.00");
        assertThat(response.overBudget()).isFalse();
        assertThat(response.items()).hasSize(2);
        assertThat(response.items().get(0).itemId()).isEqualTo(44L);
        assertThat(response.items().get(0).itemName()).isEqualTo("Milk");
        assertThat(response.items().get(0).listPosition()).isEqualTo(1);
        assertThat(response.items().get(1).itemId()).isEqualTo(45L);
        assertThat(response.items().get(1).listPosition()).isEqualTo(2);
        verify(listItemService).getListItems(12L);
    }

    /**
     * When an owner retrieves a list whose total cost exceeds its spending limit
     * Then return a negative remaining budget with overBudget set to true.
     */
    @Test
    void test27_getShoppingList_whenListIsOverBudget_thenReturnsNegativeRemainingBudget() {
        // Mock an existing list with a 10.00 limit and an item costing 12.00 in total
        when(shoppingListRepository.findByListId(12L)).thenReturn(buildShoppingList(new BigDecimal("10.00")));
        when(userRepository.existsByUserId(1L)).thenReturn(true);
        when(listItemService.getListItems(12L)).thenReturn(List.of(
                new ListItemResponseDTO(44L, 12L, "Milk", new BigDecimal("6.00"), 2, false, 1)));

        // Retrieve the shopping list details
        ShoppingListDetailsResponseDTO response = shoppingListService.getShoppingList(12L, 1L);

        // Assert that the calculated values identify the list as over budget
        assertThat(response.totalCost()).isEqualByComparingTo("12.00");
        assertThat(response.remainingBudget()).isEqualByComparingTo("-2.00");
        assertThat(response.overBudget()).isTrue();
    }

    /**
     * When an owner retrieves an empty shopping list
     * Then return zero total cost and the full spending limit as the remaining budget.
     */
    @Test
    void test28_getShoppingList_whenListIsEmpty_thenReturnsZeroTotalCost() {
        // Mock an existing empty list
        when(shoppingListRepository.findByListId(12L)).thenReturn(buildShoppingList(new BigDecimal("20.00")));
        when(userRepository.existsByUserId(1L)).thenReturn(true);
        when(listItemService.getListItems(12L)).thenReturn(List.of());

        // Retrieve the shopping list details
        ShoppingListDetailsResponseDTO response = shoppingListService.getShoppingList(12L, 1L);

        // Assert the empty list's default budget values
        assertThat(response.items()).isEmpty();
        assertThat(response.totalCost()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.remainingBudget()).isEqualByComparingTo("20.00");
        assertThat(response.overBudget()).isFalse();
    }

    /**
     * When retrieval targets a shopping list that does not exist
     * Then reject the request before checking the owner or retrieving items.
     */
    @Test
    void test29_getShoppingList_whenListDoesNotExist_thenThrowsNotFoundException() {
        // Mock the shopping-list lookup to return no list
        when(shoppingListRepository.findByListId(12L)).thenReturn(null);

        // Assert that retrieval fails with the expected not-found response
        assertThatThrownBy(() -> shoppingListService.getShoppingList(12L, 1L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Shopping list ID [12] does not exist");

        // Verify that no user or list-item operation was attempted
        verifyNoInteractions(userRepository, listItemService);
    }

    /**
     * When the user associated with a retrieved shopping list does not exist
     * Then reject the request before retrieving list items.
     */
    @Test
    void test30_getShoppingList_whenListOwnerDoesNotExist_thenThrowsNotFoundException() {
        // Mock an existing list whose associated user cannot be found
        when(shoppingListRepository.findByListId(12L)).thenReturn(buildShoppingList(new BigDecimal("20.00")));
        when(userRepository.existsByUserId(1L)).thenReturn(false);

        // Assert that retrieval fails with the expected user not-found response
        assertThatThrownBy(() -> shoppingListService.getShoppingList(12L, 1L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("User ID [1] does not exist");

        // Verify that no list items were retrieved
        verifyNoInteractions(listItemService);
    }

    /**
     * When the authenticated user does not own the requested shopping list
     * Then reject retrieval before checking the user or retrieving items.
     */
    @Test
    void test31_getShoppingList_whenAuthenticatedUserDoesNotOwnList_thenThrowsForbiddenException() {
        // Mock an existing shopping list owned by a different user
        when(shoppingListRepository.findByListId(12L)).thenReturn(buildShoppingList(new BigDecimal("20.00")));

        // Assert that retrieval fails with the expected forbidden response
        assertThatThrownBy(() -> shoppingListService.getShoppingList(12L, 2L))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("User ID [2] is not authorized to create a shopping list for user ID [1]");

        // Verify that no user or list-item operation was attempted
        verifyNoInteractions(userRepository, listItemService);
    }

    /**
     * When shopping-list retrieval has no authenticated user ID
     * Then reject it before checking the owner or retrieving items.
     */
    @Test
    void test32_getShoppingList_whenCallingUserIdIsNull_thenThrowsUnauthorizedException() {
        // Mock the shopping list targeted by the retrieval request
        when(shoppingListRepository.findByListId(12L)).thenReturn(buildShoppingList(new BigDecimal("20.00")));

        // Assert that retrieval fails with the expected unauthorized response
        assertThatThrownBy(() -> shoppingListService.getShoppingList(12L, null))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Authorization failed: calling user ID is invalid or not provided.");

        // Verify that no user or list-item operation was attempted
        verifyNoInteractions(userRepository, listItemService);
    }

    /**
     * When shopping-list retrieval has a non-positive authenticated user ID
     * Then reject it before checking the owner or retrieving items.
     */
    @Test
    void test33_getShoppingList_whenCallingUserIdIsZero_thenThrowsUnauthorizedException() {
        // Mock the shopping list targeted by the retrieval request
        when(shoppingListRepository.findByListId(12L)).thenReturn(buildShoppingList(new BigDecimal("20.00")));

        // Assert that retrieval fails with the expected unauthorized response
        assertThatThrownBy(() -> shoppingListService.getShoppingList(12L, 0L))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Authorization failed: calling user ID is invalid or not provided.");

        // Verify that no user or list-item operation was attempted
        verifyNoInteractions(userRepository, listItemService);
    }

    /**
     * When an owner retrieves a shopping list without a spending limit
     * Then calculate its total cost while returning no remaining budget and an overBudget value of false.
     */
    @Test
    void test34_getShoppingList_whenSpendingLimitIsNull_thenReturnsNoRemainingBudget() {
        // Mock an existing list without a spending limit and an item costing 7.50 in total
        when(shoppingListRepository.findByListId(12L)).thenReturn(buildShoppingList(null));
        when(userRepository.existsByUserId(1L)).thenReturn(true);
        when(listItemService.getListItems(12L)).thenReturn(List.of(
                new ListItemResponseDTO(44L, 12L, "Milk", new BigDecimal("2.50"), 3, false, 1)));

        // Retrieve the shopping list details
        ShoppingListDetailsResponseDTO response = shoppingListService.getShoppingList(12L, 1L);

        // Assert that cost is calculated independently of the optional spending limit
        assertThat(response.spendingLimit()).isNull();
        assertThat(response.totalCost()).isEqualByComparingTo("7.50");
        assertThat(response.remainingBudget()).isNull();
        assertThat(response.overBudget()).isFalse();
        assertThat(response.items()).hasSize(1);
        verify(listItemService).getListItems(12L);
    }

    /**
     * When a list owner submits a reorder request
     * Then validate list access and delegate the requested positions to the list-item service.
     */
    @Test
    void test35_reorderListItems_whenValidOwnerRequestProvided_thenDelegatesToListItemService() {
        // Mock an existing shopping list, its owner and a valid reorder request
        List<ReorderListItemRequestDTO> request = List.of(new ReorderListItemRequestDTO(44L, 1));
        when(shoppingListRepository.findByListId(12L))
                .thenReturn(ShoppingList.builder().listId(12L).userId(1L).build());
        when(userRepository.existsByUserId(1L)).thenReturn(true);

        // Invoke the service method to reorder the list items
        shoppingListService.reorderListItems(12L, request, 1L);

        // Verify list lookup, user validation and delegation to the list-item service
        verify(shoppingListRepository).findByListId(12L);
        verify(userRepository).existsByUserId(1L);
        verify(listItemService).reorderListItems(12L, request);
    }

    /**
     * When a reorder request targets a shopping list that does not exist
     * Then reject the request before checking the user or list items.
     */
    @Test
    void test36_reorderListItems_whenListDoesNotExist_thenThrowsNotFoundException() {
        // Mock the shopping-list lookup to return no list
        when(shoppingListRepository.findByListId(12L)).thenReturn(null);

        // Assert that the request fails with the expected not-found response
        assertThatThrownBy(() -> shoppingListService.reorderListItems(
                12L, List.of(new ReorderListItemRequestDTO(44L, 1)), 1L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Shopping list ID [12] does not exist");

        // Verify that no user or list-item operation was attempted
        verifyNoInteractions(userRepository, listItemService);
    }

    /**
     * When the owner associated with the shopping list does not exist
     * Then reject reordering before calling the list-item service.
     */
    @Test
    void test37_reorderListItems_whenListOwnerDoesNotExist_thenThrowsNotFoundException() {
        // Mock an existing list whose associated user cannot be found
        when(shoppingListRepository.findByListId(12L))
                .thenReturn(ShoppingList.builder().listId(12L).userId(1L).build());
        when(userRepository.existsByUserId(1L)).thenReturn(false);

        // Assert that the request fails with the expected user not-found response
        assertThatThrownBy(() -> shoppingListService.reorderListItems(
                12L, List.of(new ReorderListItemRequestDTO(44L, 1)), 1L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("User ID [1] does not exist");
        verifyNoInteractions(listItemService);
    }

    /**
     * When the authenticated user does not own the shopping list
     * Then reject reordering before checking the user or list items.
     */
    @Test
    void test38_reorderListItems_whenAuthenticatedUserDoesNotOwnList_thenThrowsForbiddenException() {
        // Mock a shopping list owned by a different user
        when(shoppingListRepository.findByListId(12L))
                .thenReturn(ShoppingList.builder().listId(12L).userId(1L).build());

        // Assert that the request fails with the expected forbidden response
        assertThatThrownBy(() -> shoppingListService.reorderListItems(
                12L, List.of(new ReorderListItemRequestDTO(44L, 1)), 2L))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("User ID [2] is not authorized to create a shopping list for user ID [1]");
        verifyNoInteractions(userRepository, listItemService);
    }

    /**
     * When a reorder request has no authenticated user ID
     * Then reject it before checking the owner or list items.
     */
    @Test
    void test39_reorderListItems_whenCallingUserIdIsNull_thenThrowsUnauthorizedException() {
        // Mock the shopping list targeted by the reorder request
        when(shoppingListRepository.findByListId(12L))
                .thenReturn(ShoppingList.builder().listId(12L).userId(1L).build());

        // Assert that the request fails with the expected unauthorized response
        assertThatThrownBy(() -> shoppingListService.reorderListItems(
                12L, List.of(new ReorderListItemRequestDTO(44L, 1)), null))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Authorization failed: calling user ID is invalid or not provided.");
        verifyNoInteractions(userRepository, listItemService);
    }

    /**
     * When a reorder request has a non-positive authenticated user ID
     * Then reject it before checking the owner or list items.
     */
    @Test
    void test40_reorderListItems_whenCallingUserIdIsZero_thenThrowsUnauthorizedException() {
        // Mock the shopping list targeted by the reorder request
        when(shoppingListRepository.findByListId(12L))
                .thenReturn(ShoppingList.builder().listId(12L).userId(1L).build());

        // Assert that the request fails with the expected unauthorized response
        assertThatThrownBy(() -> shoppingListService.reorderListItems(
                12L, List.of(new ReorderListItemRequestDTO(44L, 1)), 0L))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Authorization failed: calling user ID is invalid or not provided.");
        verifyNoInteractions(userRepository, listItemService);
    }

    private ShoppingList buildShoppingList(BigDecimal spendingLimit) {
        return ShoppingList.builder()
                .listId(12L)
                .userId(1L)
                .listName("Weekly Shop")
                .spendingLimit(spendingLimit)
                .date(LocalDate.of(2026, 7, 18))
                .build();
    }
}
