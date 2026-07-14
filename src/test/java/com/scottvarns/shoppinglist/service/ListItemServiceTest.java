package com.scottvarns.shoppinglist.service;

import com.scottvarns.shoppinglist.dto.request.CreateListItemRequestDTO;
import com.scottvarns.shoppinglist.dto.request.ReorderListItemRequestDTO;
import com.scottvarns.shoppinglist.dto.response.ListItemResponseDTO;
import com.scottvarns.shoppinglist.entity.ListItem;
import com.scottvarns.shoppinglist.exception.ConflictException;
import com.scottvarns.shoppinglist.exception.BadRequestException;
import com.scottvarns.shoppinglist.exception.NotFoundException;
import com.scottvarns.shoppinglist.repository.ListItemRepository;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@TestMethodOrder(MethodOrderer.MethodName.class)
@ExtendWith(MockitoExtension.class)
class ListItemServiceTest {

    @Mock
    private ListItemRepository listItemRepository;

    @InjectMocks
    private ListItemServiceImpl listItemService;

    /**
     * When a valid item is appended to a populated list
     * Then trim its name, use the next list position and map the saved entity.
     */
    @Test
    void test01_createListItem_whenListHasItems_thenAppendsAndMapsItem() {
        // Create a request DTO with whitespace in the item name and a saved ListItem entity to simulate the repository's save operation
        CreateListItemRequestDTO request = new CreateListItemRequestDTO(
                "  Milk  ",
                new BigDecimal("1.25"),
                2
        );

        ListItem savedItem = buildSavedItem(44L, 4);

        // Mock the repository methods to simulate the behavior of checking for existing item names, finding the max list position, and saving the new item
        when(listItemRepository.existsByListIdAndItemNameIgnoreCase(12L, "Milk")).thenReturn(false);
        when(listItemRepository.findMaxListPositionByListId(12L)).thenReturn(3);
        when(listItemRepository.save(any(ListItem.class))).thenReturn(savedItem);

        // Call the service method to create the list item and capture the response
        ListItemResponseDTO response = listItemService.createListItem(12L, request);

        // Assert that the response matches the expected values, including the trimmed item name and the next list position
        assertThat(response).isEqualTo(new ListItemResponseDTO(
                44L, 12L, "Milk", new BigDecimal("1.25"), 2, false, 4));
        ArgumentCaptor<ListItem> itemCaptor = ArgumentCaptor.forClass(ListItem.class);

        // Verify that the repository's save method was called with the expected ListItem entity and assert its properties
        verify(listItemRepository).save(itemCaptor.capture());

        assertThat(itemCaptor.getValue().getListId()).isEqualTo(12L);
        assertThat(itemCaptor.getValue().getItemName()).isEqualTo("Milk");
        assertThat(itemCaptor.getValue().getUnitPrice()).isEqualByComparingTo("1.25");
        assertThat(itemCaptor.getValue().getQuantity()).isEqualTo(2);
        assertThat(itemCaptor.getValue().getInBasket()).isFalse();
        assertThat(itemCaptor.getValue().getListPosition()).isEqualTo(4);
    }

    /**
     * When an item name already exists on the target list
     * Then reject creation without calculating a position or saving an entity.
     */
    @Test
    void test02_createListItem_whenItemNameAlreadyExists_thenThrowsConflictException() {
        // Create a request DTO with an item name that already exists in the list
        CreateListItemRequestDTO request = new CreateListItemRequestDTO(
                "Milk",
                new BigDecimal("1.25"),
                2
        );

        // Mock the repository method to simulate that the item name already exists in the list
        when(listItemRepository.existsByListIdAndItemNameIgnoreCase(12L, "Milk")).thenReturn(true);

        // Assert that calling the service method to create the list item throws a ConflictException with the expected message
        assertThatThrownBy(() -> listItemService.createListItem(12L, request))
                .isInstanceOf(ConflictException.class)
                .hasMessage("A list item with the name [Milk] already exists on shopping list ID [12]");

        // Verify that the repository methods for finding the max list position and saving the new item were never called
        verify(listItemRepository, never()).findMaxListPositionByListId(any());
        verify(listItemRepository, never()).save(any());
    }

    /**
     * When a valid item is appended to an empty list
     * Then assign the first list position.
     */
    @Test
    void test03_createListItem_whenListIsEmpty_thenUsesFirstPosition() {
        // Create a request DTO for a new item and mock the repository methods to simulate an empty list scenario
        CreateListItemRequestDTO request = new CreateListItemRequestDTO(
                "Milk",
                new BigDecimal("1.25"),
                2
        );

        // Mock the repository methods to simulate that the item name does not exist and MAX returns null for an empty list
        when(listItemRepository.existsByListIdAndItemNameIgnoreCase(12L, "Milk")).thenReturn(false);
        when(listItemRepository.findMaxListPositionByListId(12L)).thenReturn(null);
        when(listItemRepository.save(any(ListItem.class))).thenReturn(buildSavedItem(44L, 1));

        // Call the service method to create the list item and capture the response
        ListItemResponseDTO response = listItemService.createListItem(12L, request);

        // Assert that the response matches the expected values, including the first list position
        assertThat(response.listPosition()).isEqualTo(1);

        // Verify that the repository's save method was called with the expected ListItem entity and assert its list position
        ArgumentCaptor<ListItem> itemCaptor = ArgumentCaptor.forClass(ListItem.class);
        verify(listItemRepository).save(itemCaptor.capture());
        assertThat(itemCaptor.getValue().getListPosition()).isEqualTo(1);
    }

    /**
     * When an existing list item is deleted
     * Then delete it before decrementing every subsequent list position.
     */
    @Test
    void test04_deleteListItem_whenItemExists_thenDeletesAndReordersList() {
        // Create an existing list item at position two
        ListItem listItem = buildSavedItem(44L, 2);
        ListItem thirdItem = buildSavedItem(45L, 3);
        ListItem fourthItem = buildSavedItem(46L, 4);
        when(listItemRepository.findByItemIdAndListId(44L, 12L)).thenReturn(Optional.of(listItem));
        when(listItemRepository.findAllByListIdAndListPositionGreaterThanOrderByListPositionAsc(12L, 2))
                .thenReturn(List.of(thirdItem, fourthItem));

        // Invoke the service method to delete the list item
        listItemService.deleteListItem(12L, 44L);

        // Verify that deletion occurs before the remaining positions are recalculated and saved together
        InOrder repositoryOrder = inOrder(listItemRepository);
        repositoryOrder.verify(listItemRepository).findByItemIdAndListId(44L, 12L);
        repositoryOrder.verify(listItemRepository).delete(listItem);
        repositoryOrder.verify(listItemRepository)
                .findAllByListIdAndListPositionGreaterThanOrderByListPositionAsc(12L, 2);
        repositoryOrder.verify(listItemRepository).saveAll(List.of(thirdItem, fourthItem));
        assertThat(thirdItem.getListPosition()).isEqualTo(2);
        assertThat(fourthItem.getListPosition()).isEqualTo(3);
        verifyNoMoreInteractions(listItemRepository);
    }

    /**
     * When the requested item does not exist on the provided shopping list
     * Then reject deletion without changing list items or their positions.
     */
    @Test
    void test05_deleteListItem_whenItemDoesNotExistOnList_thenThrowsNotFoundException() {
        // Mock the repository to return no item for the item and list ID combination
        when(listItemRepository.findByItemIdAndListId(44L, 12L)).thenReturn(Optional.empty());

        // Assert that deletion fails with the expected not-found response
        assertThatThrownBy(() -> listItemService.deleteListItem(12L, 44L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("List item ID [44] does not exist on shopping list ID [12]");

        // Verify that no delete or position update was attempted
        verify(listItemRepository, never()).delete(any());
        verify(listItemRepository, never())
                .findAllByListIdAndListPositionGreaterThanOrderByListPositionAsc(any(), any());
        verify(listItemRepository, never()).saveAll(any());
    }

    /**
     * When an item outside the basket is toggled
     * Then set inBasket to true without deleting or reordering items.
     */
    @Test
    void test06_toggleListItemInBasket_whenItemIsNotInBasket_thenSetsInBasketTrue() {
        // Create an existing item and retain its original non-basket field values
        ListItem listItem = buildSavedItem(44L, 2);
        when(listItemRepository.findByItemIdAndListId(44L, 12L)).thenReturn(Optional.of(listItem));

        // Invoke the service method to toggle the item's in-basket state
        listItemService.toggleListItemInBasket(12L, 44L);

        // Assert that only inBasket changed and the same item was saved
        assertThat(listItem.getInBasket()).isTrue();
        assertThat(listItem.getItemId()).isEqualTo(44L);
        assertThat(listItem.getListId()).isEqualTo(12L);
        assertThat(listItem.getItemName()).isEqualTo("Milk");
        assertThat(listItem.getUnitPrice()).isEqualByComparingTo("1.25");
        assertThat(listItem.getQuantity()).isEqualTo(2);
        assertThat(listItem.getListPosition()).isEqualTo(2);
        verify(listItemRepository).findByItemIdAndListId(44L, 12L);
        verify(listItemRepository).save(listItem);
        verifyNoMoreInteractions(listItemRepository);
    }

    /**
     * When the item to toggle does not exist on the list
     * Then reject the update without saving, deleting, or reordering anything.
     */
    @Test
    void test07_toggleListItemInBasket_whenItemDoesNotExistOnList_thenThrowsNotFoundException() {
        // Mock the repository to return no matching item
        when(listItemRepository.findByItemIdAndListId(44L, 12L)).thenReturn(Optional.empty());

        // Assert that the update fails with the expected not-found response
        assertThatThrownBy(() -> listItemService.toggleListItemInBasket(12L, 44L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("List item ID [44] does not exist on shopping list ID [12]");

        // Verify that no data modification was attempted
        verify(listItemRepository, never()).save(any());
        verify(listItemRepository, never()).delete(any());
        verify(listItemRepository, never()).saveAll(any());
    }

    /**
     * When an item already in the basket is toggled
     * Then set inBasket to false without changing any other item fields.
     */
    @Test
    void test08_toggleListItemInBasket_whenItemIsAlreadyInBasket_thenSetsInBasketFalse() {
        // Create an existing item whose in-basket state is already true
        ListItem listItem = buildSavedItem(44L, 2);
        listItem.setInBasket(true);
        when(listItemRepository.findByItemIdAndListId(44L, 12L)).thenReturn(Optional.of(listItem));

        // Invoke the service method to toggle the item's in-basket state
        listItemService.toggleListItemInBasket(12L, 44L);

        // Assert that only inBasket changed from true to false
        assertThat(listItem.getInBasket()).isFalse();
        assertThat(listItem.getItemId()).isEqualTo(44L);
        assertThat(listItem.getListId()).isEqualTo(12L);
        assertThat(listItem.getItemName()).isEqualTo("Milk");
        assertThat(listItem.getUnitPrice()).isEqualByComparingTo("1.25");
        assertThat(listItem.getQuantity()).isEqualTo(2);
        assertThat(listItem.getListPosition()).isEqualTo(2);
        verify(listItemRepository).save(listItem);
    }

    /**
     * When a shopping list contains items
     * Then return every item mapped in list-position order.
     */
    @Test
    void test09_getListItems_whenListContainsItems_thenReturnsOrderedMappedItems() {
        // Create items in the order returned by the repository
        ListItem firstItem = buildSavedItem(44L, 1);
        ListItem secondItem = buildSavedItem(45L, 2);
        secondItem.setInBasket(true);
        when(listItemRepository.findAllByListIdOrderByListPositionAsc(12L))
                .thenReturn(List.of(firstItem, secondItem));

        // Retrieve and map every item on the shopping list
        List<ListItemResponseDTO> response = listItemService.getListItems(12L);

        // Assert that all entity fields are mapped and repository order is retained
        assertThat(response).containsExactly(
                new ListItemResponseDTO(44L, 12L, "Milk", new BigDecimal("1.25"), 2, false, 1),
                new ListItemResponseDTO(45L, 12L, "Milk", new BigDecimal("1.25"), 2, true, 2)
        );
        verify(listItemRepository).findAllByListIdOrderByListPositionAsc(12L);
        verifyNoMoreInteractions(listItemRepository);
    }

    /**
     * When a shopping list does not contain any items
     * Then return an empty list.
     */
    @Test
    void test10_getListItems_whenListIsEmpty_thenReturnsEmptyList() {
        // Mock the ordered repository operation to return no items
        when(listItemRepository.findAllByListIdOrderByListPositionAsc(12L)).thenReturn(List.of());

        // Retrieve the shopping list's items
        List<ListItemResponseDTO> response = listItemService.getListItems(12L);

        // Assert that the empty repository result is returned without modification
        assertThat(response).isEmpty();
        verify(listItemRepository).findAllByListIdOrderByListPositionAsc(12L);
        verifyNoMoreInteractions(listItemRepository);
    }

    /**
     * When every item is assigned a new valid position
     * Then persist the complete requested order without position conflicts.
     */
    @Test
    void test11_reorderListItems_whenCompleteUpdateProvided_thenAppliesRequestedOrder() {
        // Create three items in their current list-position order
        ListItem firstItem = buildSavedItem(44L, 1);
        ListItem secondItem = buildSavedItem(45L, 2);
        ListItem thirdItem = buildSavedItem(46L, 3);
        when(listItemRepository.findAllByListIdOrderByListPositionAsc(12L))
                .thenReturn(List.of(firstItem, secondItem, thirdItem));
        List<ReorderListItemRequestDTO> request = List.of(
                new ReorderListItemRequestDTO(45L, 1),
                new ReorderListItemRequestDTO(46L, 2),
                new ReorderListItemRequestDTO(44L, 3));

        // Apply the complete reorder request
        listItemService.reorderListItems(12L, request);

        // Assert that every item has its requested final position
        assertThat(firstItem.getListPosition()).isEqualTo(3);
        assertThat(secondItem.getListPosition()).isEqualTo(1);
        assertThat(thirdItem.getListPosition()).isEqualTo(2);
        verify(listItemRepository).findAllByListIdOrderByListPositionAsc(12L);
        verify(listItemRepository).saveAll(List.of(secondItem, thirdItem, firstItem));
    }

    /**
     * When a valid partial reorder moves one item to the first position
     * Then place it first and preserve the relative order of every unspecified item.
     */
    @Test
    void test12_reorderListItems_whenPartialUpdateProvided_thenPreservesUnspecifiedItemOrder() {
        // Create three ordered items and request only that the final item moves to position one
        ListItem firstItem = buildSavedItem(44L, 1);
        ListItem secondItem = buildSavedItem(45L, 2);
        ListItem thirdItem = buildSavedItem(46L, 3);
        when(listItemRepository.findAllByListIdOrderByListPositionAsc(12L))
                .thenReturn(List.of(firstItem, secondItem, thirdItem));

        // Apply the partial reorder request
        listItemService.reorderListItems(
                12L, List.of(new ReorderListItemRequestDTO(46L, 1)));

        // Assert that unspecified items shifted while retaining their relative order
        assertThat(thirdItem.getListPosition()).isEqualTo(1);
        assertThat(firstItem.getListPosition()).isEqualTo(2);
        assertThat(secondItem.getListPosition()).isEqualTo(3);
        verify(listItemRepository).saveAll(List.of(thirdItem, firstItem, secondItem));
    }

    /**
     * When a partial request specifies an item's existing position
     * Then complete validation without issuing unnecessary updates.
     */
    @Test
    void test13_reorderListItems_whenRequestedOrderIsUnchanged_thenDoesNotSaveItems() {
        // Create two ordered items and request an unchanged position
        ListItem firstItem = buildSavedItem(44L, 1);
        ListItem secondItem = buildSavedItem(45L, 2);
        when(listItemRepository.findAllByListIdOrderByListPositionAsc(12L))
                .thenReturn(List.of(firstItem, secondItem));

        // Apply the no-op reorder request
        listItemService.reorderListItems(
                12L, List.of(new ReorderListItemRequestDTO(45L, 2)));

        // Verify that valid unchanged positions do not cause database writes
        assertThat(firstItem.getListPosition()).isEqualTo(1);
        assertThat(secondItem.getListPosition()).isEqualTo(2);
        verify(listItemRepository, never()).saveAll(any());
    }

    /**
     * When a reorder request contains no item positions
     * Then reject it before retrieving or updating list items.
     */
    @Test
    void test14_reorderListItems_whenRequestIsEmpty_thenThrowsBadRequestException() {
        // Assert that an empty update is rejected with a clear validation message
        assertThatThrownBy(() -> listItemService.reorderListItems(12L, List.of()))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("At least one list item position must be provided");

        // Verify that no repository operation was attempted
        verifyNoMoreInteractions(listItemRepository);
    }

    /**
     * When the same item ID is included more than once
     * Then reject the reorder request without updating any items.
     */
    @Test
    void test15_reorderListItems_whenItemIdIsDuplicated_thenThrowsBadRequestException() {
        // Mock an existing two-item list and duplicate one requested item ID
        when(listItemRepository.findAllByListIdOrderByListPositionAsc(12L)).thenReturn(List.of(
                buildSavedItem(44L, 1), buildSavedItem(45L, 2)));
        List<ReorderListItemRequestDTO> request = List.of(
                new ReorderListItemRequestDTO(44L, 1),
                new ReorderListItemRequestDTO(44L, 2));

        // Assert that duplicate item IDs are rejected
        assertThatThrownBy(() -> listItemService.reorderListItems(12L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("List item ID [44] must only be provided once");
        verify(listItemRepository, never()).saveAll(any());
    }

    /**
     * When more than one item is assigned the same list position
     * Then reject the reorder request without updating any items.
     */
    @Test
    void test16_reorderListItems_whenListPositionIsDuplicated_thenThrowsBadRequestException() {
        // Mock an existing two-item list and duplicate the requested list position
        when(listItemRepository.findAllByListIdOrderByListPositionAsc(12L)).thenReturn(List.of(
                buildSavedItem(44L, 1), buildSavedItem(45L, 2)));
        List<ReorderListItemRequestDTO> request = List.of(
                new ReorderListItemRequestDTO(44L, 1),
                new ReorderListItemRequestDTO(45L, 1));

        // Assert that duplicate list positions are rejected
        assertThatThrownBy(() -> listItemService.reorderListItems(12L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("List position [1] must only be provided once");
        verify(listItemRepository, never()).saveAll(any());
    }

    /**
     * When a requested item does not exist on the shopping list
     * Then reject the reorder request without updating any items.
     */
    @Test
    void test17_reorderListItems_whenItemDoesNotExistOnList_thenThrowsNotFoundException() {
        // Mock a list that does not contain the requested item ID
        when(listItemRepository.findAllByListIdOrderByListPositionAsc(12L))
                .thenReturn(List.of(buildSavedItem(44L, 1)));

        // Assert that an item from outside the list is rejected
        assertThatThrownBy(() -> listItemService.reorderListItems(
                12L, List.of(new ReorderListItemRequestDTO(99L, 1))))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("List item ID [99] does not exist on shopping list ID [12]");
        verify(listItemRepository, never()).saveAll(any());
    }

    /**
     * When a requested position exceeds the number of items on the list
     * Then reject the reorder request without updating any items.
     */
    @Test
    void test18_reorderListItems_whenPositionExceedsListSize_thenThrowsBadRequestException() {
        // Mock a one-item list and request a position outside its valid range
        when(listItemRepository.findAllByListIdOrderByListPositionAsc(12L))
                .thenReturn(List.of(buildSavedItem(44L, 1)));

        // Assert that the out-of-range position is rejected
        assertThatThrownBy(() -> listItemService.reorderListItems(
                12L, List.of(new ReorderListItemRequestDTO(44L, 2))))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("List position [2] exceeds the number of items [1] on shopping list ID [12]");
        verify(listItemRepository, never()).saveAll(any());
    }

    /**
     * When a reorder request is null
     * Then reject it before retrieving or updating list items.
     */
    @Test
    void test19_reorderListItems_whenRequestIsNull_thenThrowsBadRequestException() {
        // Assert that a null update is rejected with the same clear validation message as an empty request
        assertThatThrownBy(() -> listItemService.reorderListItems(12L, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("At least one list item position must be provided");

        // Verify that validation rejected the request before any repository operation was attempted
        verifyNoMoreInteractions(listItemRepository);
    }

    private ListItem buildSavedItem(Long itemId, Integer listPosition) {
        return ListItem.builder()
                .itemId(itemId)
                .listId(12L)
                .itemName("Milk")
                .unitPrice(new BigDecimal("1.25"))
                .quantity(2)
                .inBasket(false)
                .listPosition(listPosition)
                .build();
    }
}
