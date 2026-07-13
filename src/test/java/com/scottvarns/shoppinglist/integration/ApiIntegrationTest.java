package com.scottvarns.shoppinglist.integration;

import com.scottvarns.shoppinglist.dto.request.CreateShoppingListRequestDTO;
import com.scottvarns.shoppinglist.dto.request.LoginRequestDTO;
import com.scottvarns.shoppinglist.dto.request.SignupRequestDTO;
import com.scottvarns.shoppinglist.entity.User;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @BeforeEach
    void clearDatabase() {
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

    private User saveUser(String email, String password, String name) {
        return userRepository.save(User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .name(name)
                .build());
    }
}
