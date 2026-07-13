package com.scottvarns.shoppinglist.controller;

import com.scottvarns.shoppinglist.dto.request.CreateShoppingListRequestDTO;
import com.scottvarns.shoppinglist.dto.response.ShoppingListResponseDTO;
import com.scottvarns.shoppinglist.service.ShoppingListService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/shopping-list")
@RequiredArgsConstructor
public class ShoppingListController {

    private static final Logger log = LoggerFactory.getLogger(ShoppingListController.class);

    private final ShoppingListService shoppingListService;


    /**
     * Endpoint to create a new shopping list.
     *
     * @param correlationId Correlation ID for end-to-end traceability.
     * @param request       The request body containing shopping list details.
     * @param principal     The authenticated user established from the JWT.
     *
     * @return ResponseEntity containing the created shopping list response DTO.
     */
    @PostMapping
    public ResponseEntity<ShoppingListResponseDTO> createShoppingList(
            @RequestHeader(value = "X-Correlation-ID") UUID correlationId,
            @Valid @RequestBody CreateShoppingListRequestDTO request,
            Principal principal
    ) {
        log.info("Received shopping-list creation request for userId [{}]", request.userId());
        ShoppingListResponseDTO response =
                shoppingListService.createShoppingList(request, Long.valueOf(principal.getName()));

        return ResponseEntity
                .status(201)
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }



}
