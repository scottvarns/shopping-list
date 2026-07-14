package com.scottvarns.shoppinglist.integration;

import com.scottvarns.shoppinglist.dto.request.CreateShoppingListRequestDTO;
import com.scottvarns.shoppinglist.dto.request.CreateListItemRequestDTO;
import com.scottvarns.shoppinglist.dto.request.LoginRequestDTO;
import com.scottvarns.shoppinglist.dto.request.ReorderListItemRequestDTO;
import com.scottvarns.shoppinglist.dto.request.SignupRequestDTO;
import com.scottvarns.shoppinglist.entity.ListItem;
import com.scottvarns.shoppinglist.entity.ShoppingList;
import com.scottvarns.shoppinglist.entity.User;
import com.scottvarns.shoppinglist.repository.ListItemRepository;
import com.scottvarns.shoppinglist.repository.ShoppingListRepository;
import com.scottvarns.shoppinglist.repository.UserRepository;
import com.scottvarns.shoppinglist.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
@TestMethodOrder(MethodOrderer.MethodName.class)
class ApiIntegrationTest {

    private static final String JWT_SECRET =
            "c2hvcHBpbmdsaXN0LXRlc3Qtc2VjcmV0LWtleS1tdXN0LWJlLWF0LWxlYXN0LTMyLWJ5dGVz";

    @Container
    static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("shoppinglist")
            .withUsername("shoppinglist")
            .withPassword("shoppinglist");

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.datasource.driver-class-name", mysql::getDriverClassName);
        registry.add("spring.flyway.enabled", () -> true);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("security.jwt.secret", () -> JWT_SECRET);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ShoppingListRepository shoppingListRepository;

    @Autowired
    private ListItemRepository listItemRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @BeforeEach
    void clearDatabase() {
        listItemRepository.deleteAll();
        shoppingListRepository.deleteAll();
        userRepository.deleteAll();
    }

    /**
     * When valid signup details are submitted
     * Then create a user in MySQL with a BCrypt password hash.
     */
    @Test
    void test01_signup_whenValidRequestProvided_thenCreatesUser() throws Exception {
        SignupRequestDTO request = new SignupRequestDTO("signup@example.com", "your-password", "Signup User");

        mockMvc.perform(post("/api/auth/signup")
                        .header("X-Correlation-ID", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("signup@example.com"))
                .andExpect(jsonPath("$.name").value("Signup User"));

        User user = userRepository.findByEmail("signup@example.com").orElseThrow();
        assertThat(passwordEncoder.matches("your-password", user.getPasswordHash())).isTrue();
    }

    /**
     * When valid credentials are submitted
     * Then return a JWT for the existing user.
     */
    @Test
    void test02_login_whenValidCredentialsProvided_thenReturnsJwt() throws Exception {
        User user = saveUser("login@example.com", "your-password", "Login User");
        LoginRequestDTO request = new LoginRequestDTO("login@example.com", "your-password");

        String responseBody = mockMvc.perform(post("/api/auth/login")
                        .header("X-Correlation-ID", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String token = objectMapper.readTree(responseBody).get("token").asString();
        assertThat(jwtService.extractUserId(token)).isEqualTo(user.getUserId());
    }

    /**
     * When an authenticated user creates a shopping list for themselves
     * Then persist the list and return a 201 response.
     */
    @Test
    void test03_createShoppingList_whenAuthenticatedUserMatchesRequestUser_thenCreatesList() throws Exception {
        User user = saveUser("list@example.com", "your-password", "List User");
        CreateShoppingListRequestDTO request = new CreateShoppingListRequestDTO(
                user.getUserId(), "Weekly Shop", new BigDecimal("75.00"), LocalDate.of(2026, 7, 18));
        String token = jwtService.generateToken(user.getUserId(), user.getEmail());

        mockMvc.perform(post("/api/shopping-list")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Correlation-ID", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(user.getUserId()))
                .andExpect(jsonPath("$.listName").value("Weekly Shop"));

        assertThat(shoppingListRepository.existsByUserIdAndListNameIgnoreCase(user.getUserId(), "Weekly Shop"))
                .isTrue();
    }

    /**
     * When an authenticated list owner adds an item to an existing shopping list
     * Then persist it with the next position and an unpurchased status.
     */
    @Test
    void test04_createListItem_whenAuthenticatedUserOwnsList_thenAppendsItem() throws Exception {
        User user = saveUser("item@example.com", "your-password", "Item User");
        ShoppingList shoppingList = shoppingListRepository.save(ShoppingList.builder()
                .userId(user.getUserId())
                .listName("Weekly Shop")
                .build());
        ShoppingList otherShoppingList = shoppingListRepository.save(ShoppingList.builder()
                .userId(user.getUserId())
                .listName("Other Shop")
                .build());
        listItemRepository.save(ListItem.builder()
                .listId(shoppingList.getListId())
                .itemName("Bread")
                .unitPrice(new BigDecimal("1.00"))
                .quantity(1)
                .inBasket(false)
                .listPosition(3)
                .build());
        listItemRepository.save(ListItem.builder()
                .listId(otherShoppingList.getListId())
                .itemName("Milk")
                .unitPrice(new BigDecimal("1.25"))
                .quantity(1)
                .inBasket(false)
                .listPosition(1)
                .build());
        CreateListItemRequestDTO request = new CreateListItemRequestDTO("Milk", new BigDecimal("1.25"), 2);
        String token = jwtService.generateToken(user.getUserId(), user.getEmail());

        mockMvc.perform(post("/api/shopping-list/{listId}/item", shoppingList.getListId())
                        .header("Authorization", "Bearer " + token)
                        .header("X-Correlation-ID", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.listId").value(shoppingList.getListId()))
                .andExpect(jsonPath("$.itemName").value("Milk"))
                .andExpect(jsonPath("$.unitCost").value(1.25))
                .andExpect(jsonPath("$.quantity").value(2))
                .andExpect(jsonPath("$.inBasket").value(false))
                .andExpect(jsonPath("$.listPosition").value(4));

        ListItem createdItem = listItemRepository.findAll().stream()
                .filter(item -> item.getListId().equals(shoppingList.getListId()))
                .filter(item -> item.getItemName().equals("Milk"))
                .findFirst()
                .orElseThrow();
        assertThat(createdItem.getListId()).isEqualTo(shoppingList.getListId());
        assertThat(createdItem.getListPosition()).isEqualTo(4);
        assertThat(createdItem.getInBasket()).isFalse();
    }

    /**
     * When an authenticated list owner deletes an existing item
     * Then remove the item and close the gap in the remaining list positions.
     */
    @Test
    void test05_deleteListItem_whenAuthenticatedUserOwnsList_thenDeletesAndReordersItems() throws Exception {
        // Create an authenticated user, shopping list, and three ordered list items
        User user = saveUser("delete-item@example.com", "your-password", "Delete Item User");
        ShoppingList shoppingList = shoppingListRepository.save(ShoppingList.builder()
                .userId(user.getUserId())
                .listName("Weekly Shop")
                .build());
        ListItem firstItem = saveListItem(shoppingList.getListId(), "Bread", 1);
        ListItem deletedItem = saveListItem(shoppingList.getListId(), "Milk", 2);
        ListItem lastItem = saveListItem(shoppingList.getListId(), "Eggs", 3);
        String token = jwtService.generateToken(user.getUserId(), user.getEmail());

        // Invoke the endpoint without a request body and assert the empty 204 response
        mockMvc.perform(delete("/api/shopping-list/{listId}/item/{itemId}",
                        shoppingList.getListId(), deletedItem.getItemId())
                        .header("Authorization", "Bearer " + token)
                        .header("X-Correlation-ID", UUID.randomUUID()))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        // Verify that the selected item was deleted and subsequent positions were decremented
        assertThat(listItemRepository.findById(deletedItem.getItemId())).isEmpty();
        List<ListItem> remainingItems = listItemRepository.findAll().stream()
                .filter(item -> item.getListId().equals(shoppingList.getListId()))
                .sorted(Comparator.comparing(ListItem::getListPosition))
                .toList();
        assertThat(remainingItems).extracting(ListItem::getItemId)
                .containsExactly(firstItem.getItemId(), lastItem.getItemId());
        assertThat(remainingItems).extracting(ListItem::getListPosition)
                .containsExactly(1, 2);
    }

    /**
     * When an authenticated list owner toggles an item outside the basket
     * Then update only its in-basket state without removing or reordering any items.
     */
    @Test
    void test06_toggleListItemInBasket_whenItemIsNotInBasket_thenSetsInBasketTrue() throws Exception {
        // Create an authenticated user, shopping list, target item, and subsequent item
        User user = saveUser("basket-item@example.com", "your-password", "Basket Item User");
        ShoppingList shoppingList = shoppingListRepository.save(ShoppingList.builder()
                .userId(user.getUserId())
                .listName("Weekly Shop")
                .build());
        ListItem targetItem = saveListItem(shoppingList.getListId(), "Milk", 2);
        ListItem subsequentItem = saveListItem(shoppingList.getListId(), "Eggs", 3);
        String token = jwtService.generateToken(user.getUserId(), user.getEmail());

        // Invoke the PATCH endpoint without a request body and assert the empty 204 response
        mockMvc.perform(patch("/api/shopping-list/{listId}/item/{itemId}",
                        shoppingList.getListId(), targetItem.getItemId())
                        .header("Authorization", "Bearer " + token)
                        .header("X-Correlation-ID", UUID.randomUUID()))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        // Verify that only the target item's inBasket field changed
        ListItem updatedItem = listItemRepository.findById(targetItem.getItemId()).orElseThrow();
        assertThat(updatedItem.getListId()).isEqualTo(shoppingList.getListId());
        assertThat(updatedItem.getItemName()).isEqualTo("Milk");
        assertThat(updatedItem.getUnitPrice()).isEqualByComparingTo("1.00");
        assertThat(updatedItem.getQuantity()).isEqualTo(1);
        assertThat(updatedItem.getListPosition()).isEqualTo(2);
        assertThat(updatedItem.getInBasket()).isTrue();

        // Verify that the item remains on the list and the queue was not reordered
        ListItem unchangedSubsequentItem = listItemRepository.findById(subsequentItem.getItemId()).orElseThrow();
        assertThat(listItemRepository.count()).isEqualTo(2);
        assertThat(unchangedSubsequentItem.getListPosition()).isEqualTo(3);
        assertThat(unchangedSubsequentItem.getInBasket()).isFalse();
    }

    /**
     * When an authenticated list owner toggles an item already in the basket
     * Then set inBasket to false without removing or reordering any items.
     */
    @Test
    void test07_toggleListItemInBasket_whenItemIsAlreadyInBasket_thenSetsInBasketFalse() throws Exception {
        // Create an authenticated user, shopping list, and item already in the basket
        User user = saveUser("toggle-basket@example.com", "your-password", "Toggle Basket User");
        ShoppingList shoppingList = shoppingListRepository.save(ShoppingList.builder()
                .userId(user.getUserId())
                .listName("Weekly Shop")
                .build());
        ListItem targetItem = saveListItem(shoppingList.getListId(), "Milk", 2);
        targetItem.setInBasket(true);
        listItemRepository.save(targetItem);
        String token = jwtService.generateToken(user.getUserId(), user.getEmail());

        // Invoke the PATCH endpoint once and assert the empty 204 response
        mockMvc.perform(patch("/api/shopping-list/{listId}/item/{itemId}",
                        shoppingList.getListId(), targetItem.getItemId())
                        .header("Authorization", "Bearer " + token)
                        .header("X-Correlation-ID", UUID.randomUUID()))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        // Verify that the item remains in place and its inBasket state is now false
        ListItem updatedItem = listItemRepository.findById(targetItem.getItemId()).orElseThrow();
        assertThat(updatedItem.getInBasket()).isFalse();
        assertThat(updatedItem.getListPosition()).isEqualTo(2);
        assertThat(updatedItem.getItemName()).isEqualTo("Milk");
        assertThat(listItemRepository.count()).isEqualTo(1);
    }

    /**
     * When an authenticated list owner retrieves an existing shopping list
     * Then return its ordered items and budget calculations without repeating listId on each item.
     */
    @Test
    void test08_getShoppingList_whenAuthenticatedUserOwnsList_thenReturnsListDetails() throws Exception {
        // Create an authenticated user, a budgeted shopping list and two ordered items costing 11.00 in total
        User user = saveUser("get-list@example.com", "your-password", "Get List User");
        ShoppingList shoppingList = shoppingListRepository.save(ShoppingList.builder()
                .userId(user.getUserId())
                .listName("Weekly Shop")
                .spendingLimit(new BigDecimal("10.00"))
                .date(LocalDate.of(2026, 7, 18))
                .build());
        ListItem firstItem = listItemRepository.save(ListItem.builder()
                .listId(shoppingList.getListId())
                .itemName("Bread")
                .unitPrice(new BigDecimal("3.00"))
                .quantity(2)
                .inBasket(false)
                .listPosition(1)
                .build());
        ListItem secondItem = listItemRepository.save(ListItem.builder()
                .listId(shoppingList.getListId())
                .itemName("Milk")
                .unitPrice(new BigDecimal("5.00"))
                .quantity(1)
                .inBasket(true)
                .listPosition(2)
                .build());
        String token = jwtService.generateToken(user.getUserId(), user.getEmail());

        // Invoke the GET endpoint and assert the list fields, calculations and ordered item projection
        mockMvc.perform(get("/api/shopping-list/{listId}", shoppingList.getListId())
                        .header("Authorization", "Bearer " + token)
                        .header("X-Correlation-ID", UUID.randomUUID()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.listId").value(shoppingList.getListId()))
                .andExpect(jsonPath("$.userId").value(user.getUserId()))
                .andExpect(jsonPath("$.listName").value("Weekly Shop"))
                .andExpect(jsonPath("$.spendingLimit").value(10.00))
                .andExpect(jsonPath("$.date").value("2026-07-18"))
                .andExpect(jsonPath("$.totalCost").value(11.00))
                .andExpect(jsonPath("$.remainingBudget").value(-1.00))
                .andExpect(jsonPath("$.overBudget").value(true))
                .andExpect(jsonPath("$.items[0].itemId").value(firstItem.getItemId()))
                .andExpect(jsonPath("$.items[0].itemName").value("Bread"))
                .andExpect(jsonPath("$.items[0].listPosition").value(1))
                .andExpect(jsonPath("$.items[0].listId").doesNotExist())
                .andExpect(jsonPath("$.items[1].itemId").value(secondItem.getItemId()))
                .andExpect(jsonPath("$.items[1].itemName").value("Milk"))
                .andExpect(jsonPath("$.items[1].inBasket").value(true))
                .andExpect(jsonPath("$.items[1].listPosition").value(2))
                .andExpect(jsonPath("$.items[1].listId").doesNotExist());

        // Verify that retrieval did not modify the shopping list or its items
        assertThat(shoppingListRepository.findByListId(shoppingList.getListId()).getListName())
                .isEqualTo("Weekly Shop");
        assertThat(listItemRepository.count()).isEqualTo(2);
    }

    /**
     * When an authenticated owner retrieves a shopping list without a spending limit
     * Then return its total cost with null budget values and overBudget set to false.
     */
    @Test
    void test09_getShoppingList_whenSpendingLimitIsNull_thenReturnsNoRemainingBudget() throws Exception {
        // Create an authenticated user, a list without a spending limit and one item costing 7.50 in total
        User user = saveUser("no-budget@example.com", "your-password", "No Budget User");
        ShoppingList shoppingList = shoppingListRepository.save(ShoppingList.builder()
                .userId(user.getUserId())
                .listName("No Budget Shop")
                .spendingLimit(null)
                .date(LocalDate.of(2026, 7, 18))
                .build());
        listItemRepository.save(ListItem.builder()
                .listId(shoppingList.getListId())
                .itemName("Milk")
                .unitPrice(new BigDecimal("2.50"))
                .quantity(3)
                .inBasket(false)
                .listPosition(1)
                .build());
        String token = jwtService.generateToken(user.getUserId(), user.getEmail());

        // Invoke the GET endpoint and assert the optional budget values
        mockMvc.perform(get("/api/shopping-list/{listId}", shoppingList.getListId())
                        .header("Authorization", "Bearer " + token)
                        .header("X-Correlation-ID", UUID.randomUUID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.spendingLimit").value(nullValue()))
                .andExpect(jsonPath("$.totalCost").value(7.50))
                .andExpect(jsonPath("$.remainingBudget").value(nullValue()))
                .andExpect(jsonPath("$.overBudget").value(false))
                .andExpect(jsonPath("$.items[0].itemName").value("Milk"));
    }

    /**
     * When an authenticated owner submits a partial reorder request
     * Then place the specified item correctly and preserve the relative order of unspecified items.
     */
    @Test
    void test10_reorderListItems_whenPartialUpdateProvided_thenReordersPersistedItems() throws Exception {
        // Create an authenticated user, shopping list and four ordered items
        User user = saveUser("reorder@example.com", "your-password", "Reorder User");
        ShoppingList shoppingList = shoppingListRepository.save(ShoppingList.builder()
                .userId(user.getUserId())
                .listName("Weekly Shop")
                .build());
        ListItem firstItem = saveListItem(shoppingList.getListId(), "Bread", 1);
        ListItem secondItem = saveListItem(shoppingList.getListId(), "Milk", 2);
        ListItem thirdItem = saveListItem(shoppingList.getListId(), "Eggs", 3);
        ListItem fourthItem = saveListItem(shoppingList.getListId(), "Cheese", 4);
        String token = jwtService.generateToken(user.getUserId(), user.getEmail());
        List<ReorderListItemRequestDTO> request = List.of(new ReorderListItemRequestDTO(fourthItem.getItemId(), 1));

        // Invoke the reorder endpoint and assert the empty 204 response
        mockMvc.perform(patch("/api/shopping-list/{listId}/item/reorder", shoppingList.getListId())
                        .header("Authorization", "Bearer " + token)
                        .header("X-Correlation-ID", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        // Verify the specified item moved first and unspecified items retained their relative order
        List<ListItem> reorderedItems = listItemRepository.findAllByListIdOrderByListPositionAsc(
                shoppingList.getListId());
        assertThat(reorderedItems).extracting(ListItem::getItemId).containsExactly(
                fourthItem.getItemId(), firstItem.getItemId(), secondItem.getItemId(), thirdItem.getItemId());
        assertThat(reorderedItems).extracting(ListItem::getListPosition).containsExactly(1, 2, 3, 4);

        // Verify that no item data was removed or otherwise changed
        assertThat(reorderedItems).extracting(ListItem::getItemName)
                .containsExactly("Cheese", "Bread", "Milk", "Eggs");
        assertThat(listItemRepository.count()).isEqualTo(4);
    }

    /**
     * When a protected shopping-list endpoint is requested without a bearer token
     * Then the configured Spring Security entry point returns HTTP 401 Unauthorized.
     */
    @Test
    void test11_getShoppingList_whenBearerTokenIsMissing_thenReturns401() throws Exception {
        // Invoke a protected endpoint without an Authorization header
        mockMvc.perform(get("/api/shopping-list/12")
                        .header("X-Correlation-ID", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    /**
     * When a protected shopping-list endpoint is requested with an invalid bearer token
     * Then the configured Spring Security entry point returns HTTP 401 Unauthorized.
     */
    @Test
    void test12_getShoppingList_whenBearerTokenIsInvalid_thenReturns401() throws Exception {
        // Invoke a protected endpoint with a bearer token that cannot be validated
        mockMvc.perform(get("/api/shopping-list/12")
                        .header("Authorization", "Bearer invalid.token.here")
                        .header("X-Correlation-ID", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    private User saveUser(String email, String password, String name) {
        return userRepository.save(User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .name(name)
                .build());
    }

    private ListItem saveListItem(Long listId, String itemName, Integer listPosition) {
        return listItemRepository.save(ListItem.builder()
                .listId(listId)
                .itemName(itemName)
                .unitPrice(new BigDecimal("1.00"))
                .quantity(1)
                .inBasket(false)
                .listPosition(listPosition)
                .build());
    }
}
