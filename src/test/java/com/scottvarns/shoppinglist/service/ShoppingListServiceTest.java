package com.scottvarns.shoppinglist.service;

import com.scottvarns.shoppinglist.dto.request.CreateShoppingListRequestDTO;
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
}