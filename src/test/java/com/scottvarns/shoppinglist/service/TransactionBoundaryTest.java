package com.scottvarns.shoppinglist.service;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

@TestMethodOrder(MethodOrderer.MethodName.class)
class TransactionBoundaryTest {

    /**
     * When authentication or shopping-list application logic is entered from an API request
     * Then the API-facing service implementation owns a required transaction boundary.
     */
    @Test
    void test01_apiFacingServices_whenInspected_thenDefineRequiredTransactionBoundary() {
        // Retrieve the class-level transaction configuration for both API-facing services
        Transactional authenticationTransaction = AnnotatedElementUtils.findMergedAnnotation(
                AuthenticationServiceImpl.class, Transactional.class);
        Transactional shoppingListTransaction = AnnotatedElementUtils.findMergedAnnotation(
                ShoppingListServiceImpl.class, Transactional.class);

        // Assert that every public application operation joins or creates exactly one request transaction
        assertThat(authenticationTransaction).isNotNull();
        assertThat(authenticationTransaction.propagation()).isEqualTo(Propagation.REQUIRED);
        assertThat(shoppingListTransaction).isNotNull();
        assertThat(shoppingListTransaction.propagation()).isEqualTo(Propagation.REQUIRED);
    }

    /**
     * When read-only API operations are inspected
     * Then their shared request transaction is marked as read-only.
     */
    @Test
    void test02_readOnlyApiOperations_whenInspected_thenUseReadOnlyTransactions() throws Exception {
        // Retrieve the method-level transaction configuration for login and shopping-list retrieval
        Method loginMethod = AuthenticationServiceImpl.class.getMethod(
                "login", com.scottvarns.shoppinglist.dto.request.LoginRequestDTO.class);
        Method getShoppingListMethod = ShoppingListServiceImpl.class.getMethod(
                "getShoppingList", Long.class, Long.class);
        Transactional loginTransaction = AnnotatedElementUtils.findMergedAnnotation(
                loginMethod, Transactional.class);
        Transactional getShoppingListTransaction = AnnotatedElementUtils.findMergedAnnotation(
                getShoppingListMethod, Transactional.class);

        // Assert that neither read endpoint requests a write-capable transaction
        assertThat(loginTransaction).isNotNull();
        assertThat(loginTransaction.readOnly()).isTrue();
        assertThat(getShoppingListTransaction).isNotNull();
        assertThat(getShoppingListTransaction.readOnly()).isTrue();
    }

    /**
     * When list-item operations are orchestrated by the shopping-list service
     * Then the nested service does not declare an additional transaction boundary.
     */
    @Test
    void test03_listItemService_whenInspected_thenUsesCallingRequestTransaction() {
        // Assert that transaction ownership remains at the API-facing shopping-list service
        assertThat(AnnotatedElementUtils.findMergedAnnotation(
                ListItemServiceImpl.class, Transactional.class)).isNull();
        assertThat(ListItemServiceImpl.class.getDeclaredMethods())
                .filteredOn(method -> java.lang.reflect.Modifier.isPublic(method.getModifiers()))
                .allSatisfy(method -> assertThat(AnnotatedElementUtils.findMergedAnnotation(
                        method, Transactional.class)).isNull());
    }
}
