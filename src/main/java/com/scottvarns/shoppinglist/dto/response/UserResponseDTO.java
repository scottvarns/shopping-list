package com.scottvarns.shoppinglist.dto.response;

public record UserResponseDTO(
        Long userId,
        String email,
        String name
) {
}
