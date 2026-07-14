package com.scottvarns.shoppinglist.bdd;

import com.scottvarns.shoppinglist.dto.request.CreateListItemRequestDTO;
import com.scottvarns.shoppinglist.dto.request.CreateShoppingListRequestDTO;
import com.scottvarns.shoppinglist.dto.request.LoginRequestDTO;
import com.scottvarns.shoppinglist.dto.request.ReorderListItemRequestDTO;
import com.scottvarns.shoppinglist.dto.request.SignupRequestDTO;
import com.scottvarns.shoppinglist.repository.ListItemRepository;
import com.scottvarns.shoppinglist.repository.ShoppingListRepository;
import com.scottvarns.shoppinglist.repository.UserRepository;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.cucumber.spring.ScenarioScope;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@ScenarioScope
public class CompleteShoppingJourneySteps {

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;
    private final ShoppingListRepository shoppingListRepository;
    private final ListItemRepository listItemRepository;

    private final Map<String, Long> itemIdsByName = new HashMap<>();

    private int lastStatus;
    private JsonNode lastResponse;
    private Long userId;
    private Long listId;
    private String accessToken;
    private boolean logoutSucceeded;

    public CompleteShoppingJourneySteps(
            MockMvc mockMvc,
            ObjectMapper objectMapper,
            UserRepository userRepository,
            ShoppingListRepository shoppingListRepository,
            ListItemRepository listItemRepository
    ) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
        this.userRepository = userRepository;
        this.shoppingListRepository = shoppingListRepository;
        this.listItemRepository = listItemRepository;
    }

    @Before
    public void clearDatabase() {
        listItemRepository.deleteAll();
        shoppingListRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Given("a new shopper registers with:")
    public void aNewShopperRegistersWith(DataTable table) throws Exception {
        Map<String, String> details = table.asMap(String.class, String.class);
        SignupRequestDTO request = new SignupRequestDTO(
                details.get("email"), details.get("password"), details.get("name"));

        perform(post("/api/auth/signup")
                .header("X-Correlation-ID", UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        if (lastResponse != null && lastResponse.hasNonNull("userId")) {
            userId = lastResponse.get("userId").asLong();
        }
    }

    @Then("the registration should succeed")
    public void theRegistrationShouldSucceed() {
        assertThat(lastStatus).isEqualTo(201);
        assertThat(userId).isNotNull();
    }

    @When("the shopper logs in with:")
    public void theShopperLogsInWith(DataTable table) throws Exception {
        Map<String, String> details = table.asMap(String.class, String.class);
        LoginRequestDTO request = new LoginRequestDTO(details.get("email"), details.get("password"));

        perform(post("/api/auth/login")
                .header("X-Correlation-ID", UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        if (lastResponse != null && lastResponse.hasNonNull("token")) {
            accessToken = lastResponse.get("token").asString();
        }
    }

    @Then("the login should succeed")
    public void theLoginShouldSucceed() {
        assertThat(lastStatus).isEqualTo(200);
    }

    @And("an access token should be returned")
    public void anAccessTokenShouldBeReturned() {
        assertThat(accessToken).isNotBlank();
    }

    @When("the shopper creates a shopping list with:")
    public void theShopperCreatesAShoppingListWith(DataTable table) throws Exception {
        Map<String, String> details = table.asMap(String.class, String.class);
        CreateShoppingListRequestDTO request = new CreateShoppingListRequestDTO(
                userId,
                details.get("listName"),
                new BigDecimal(details.get("spendingLimit")),
                LocalDate.parse(details.get("date")));

        perform(post("/api/shopping-list")
                .header("Authorization", bearerToken())
                .header("X-Correlation-ID", UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        if (lastResponse != null && lastResponse.hasNonNull("listId")) {
            listId = lastResponse.get("listId").asLong();
        }
    }

    @Then("the shopping list should be created successfully")
    public void theShoppingListShouldBeCreatedSuccessfully() {
        assertThat(lastStatus).isEqualTo(201);
        assertThat(listId).isNotNull();
    }

    @When("the shopper retrieves the shopping list")
    public void theShopperRetrievesTheShoppingList() throws Exception {
        perform(get("/api/shopping-list/{listId}", listId)
                .header("Authorization", bearerToken())
                .header("X-Correlation-ID", UUID.randomUUID()));
        assertThat(lastStatus).isEqualTo(200);
    }

    @Then("the shopping list should be named {string}")
    public void theShoppingListShouldBeNamed(String expectedName) {
        assertThat(lastResponse.get("listName").asString()).isEqualTo(expectedName);
    }

    @Then("the shopping list should contain {int} items")
    public void theShoppingListShouldContainItems(int expectedCount) {
        assertThat(lastResponse.get("items").size()).isEqualTo(expectedCount);
    }

    @Then("the shopping list total should be {bigdecimal}")
    public void theShoppingListTotalShouldBe(BigDecimal expectedTotal) {
        assertThat(lastResponse.get("totalCost").decimalValue()).isEqualByComparingTo(expectedTotal);
    }

    @Then("the shopping list should not be over budget")
    public void theShoppingListShouldNotBeOverBudget() {
        assertThat(lastResponse.get("overBudget").asBoolean()).isFalse();
    }

    @When("the shopper adds the following items:")
    public void theShopperAddsTheFollowingItems(DataTable table) throws Exception {
        for (Map<String, String> item : table.asMaps(String.class, String.class)) {
            CreateListItemRequestDTO request = new CreateListItemRequestDTO(
                    item.get("itemName"),
                    new BigDecimal(item.get("unitPrice")),
                    Integer.valueOf(item.get("quantity")));

            perform(post("/api/shopping-list/{listId}/item", listId)
                    .header("Authorization", bearerToken())
                    .header("X-Correlation-ID", UUID.randomUUID())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            assertThat(lastStatus).isEqualTo(201);
            itemIdsByName.put(item.get("itemName"), lastResponse.get("itemId").asLong());
        }
    }

    @Then("the shopping list should be over budget")
    public void theShoppingListShouldBeOverBudget() {
        assertThat(lastResponse.get("overBudget").asBoolean()).isTrue();
    }

    @Then("the remaining budget should be {bigdecimal}")
    public void theRemainingBudgetShouldBe(BigDecimal expectedBudget) {
        assertThat(lastResponse.get("remainingBudget").decimalValue()).isEqualByComparingTo(expectedBudget);
    }

    @Then("the shopping list should contain:")
    public void theShoppingListShouldContain(DataTable table) {
        for (Map<String, String> expectedItem : table.asMaps(String.class, String.class)) {
            JsonNode actualItem = itemByName(expectedItem.get("itemName"));
            assertThat(actualItem.get("inBasket").asBoolean())
                    .isEqualTo(Boolean.parseBoolean(expectedItem.get("purchased")));
        }
    }

    @When("the shopper removes {string}")
    public void theShopperRemoves(String itemName) throws Exception {
        perform(delete("/api/shopping-list/{listId}/item/{itemId}", listId, itemId(itemName))
                .header("Authorization", bearerToken())
                .header("X-Correlation-ID", UUID.randomUUID()));
        assertThat(lastStatus).isEqualTo(204);
        itemIdsByName.remove(itemName);
    }

    @When("the shopper reorders the shopping list to:")
    public void theShopperReordersTheShoppingListTo(DataTable table) throws Exception {
        List<ReorderListItemRequestDTO> request = new ArrayList<>();
        for (Map<String, String> item : table.asMaps(String.class, String.class)) {
            request.add(new ReorderListItemRequestDTO(
                    itemId(item.get("itemName")), Integer.valueOf(item.get("position"))));
        }

        perform(patch("/api/shopping-list/{listId}/item/reorder", listId)
                .header("Authorization", bearerToken())
                .header("X-Correlation-ID", UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
        assertThat(lastStatus).isEqualTo(204);
    }

    @Then("the shopping list items should be returned in this order:")
    public void theShoppingListItemsShouldBeReturnedInThisOrder(DataTable table) {
        List<Map<String, String>> expectedItems = table.asMaps(String.class, String.class);
        JsonNode actualItems = lastResponse.get("items");
        assertThat(actualItems.size()).isEqualTo(expectedItems.size());

        for (int index = 0; index < expectedItems.size(); index++) {
            Map<String, String> expectedItem = expectedItems.get(index);
            JsonNode actualItem = actualItems.get(index);
            assertThat(actualItem.get("itemName").asString()).isEqualTo(expectedItem.get("itemName"));
            assertThat(actualItem.get("listPosition").asInt())
                    .isEqualTo(Integer.parseInt(expectedItem.get("position")));
        }
    }

    @When("the shopper marks {string} as purchased")
    public void theShopperMarksAsPurchased(String itemName) throws Exception {
        perform(patch("/api/shopping-list/{listId}/item/{itemId}", listId, itemId(itemName))
                .header("Authorization", bearerToken())
                .header("X-Correlation-ID", UUID.randomUUID()));
        assertThat(lastStatus).isEqualTo(204);
    }

    @Then("{string} should be marked as purchased")
    public void shouldBeMarkedAsPurchased(String itemName) {
        assertThat(itemByName(itemName).get("inBasket").asBoolean()).isTrue();
    }

    @Then("{string} should not be marked as purchased")
    public void shouldNotBeMarkedAsPurchased(String itemName) {
        assertThat(itemByName(itemName).get("inBasket").asBoolean()).isFalse();
    }

    @Then("the shopping list should contain {int} purchased items")
    public void theShoppingListShouldContainPurchasedItems(int expectedCount) {
        assertThat(countItemsWithPurchasedState(true)).isEqualTo(expectedCount);
    }

    @Then("the shopping list should contain {int} unpurchased items")
    public void theShoppingListShouldContainUnpurchasedItems(int expectedCount) {
        assertThat(countItemsWithPurchasedState(false)).isEqualTo(expectedCount);
    }

    private void perform(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request)
            throws Exception {
        MvcResult result = mockMvc.perform(request).andReturn();
        lastStatus = result.getResponse().getStatus();
        String responseBody = result.getResponse().getContentAsString();
        lastResponse = responseBody.isBlank() ? null : objectMapper.readTree(responseBody);
    }

    private String bearerToken() {
        return "Bearer " + accessToken;
    }

    private Long itemId(String itemName) {
        assertThat(itemIdsByName).containsKey(itemName);
        return itemIdsByName.get(itemName);
    }

    private JsonNode itemByName(String itemName) {
        for (JsonNode item : lastResponse.get("items")) {
            if (itemName.equals(item.get("itemName").asString())) {
                return item;
            }
        }
        throw new AssertionError("Shopping list does not contain item: " + itemName);
    }

    private long countItemsWithPurchasedState(boolean purchased) {
        long count = 0;
        for (JsonNode item : lastResponse.get("items")) {
            if (item.get("inBasket").asBoolean() == purchased) {
                count++;
            }
        }
        return count;
    }
}
