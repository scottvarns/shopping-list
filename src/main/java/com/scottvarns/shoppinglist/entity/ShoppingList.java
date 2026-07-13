package com.scottvarns.shoppinglist.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "shopping_lists")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShoppingList {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "list_id")
    private Long listId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "list_name", nullable = false, length = 150)
    private String listName;

    @Column(name = "spending_limit", precision = 10, scale = 2)
    private BigDecimal spendingLimit;

    @Column(name = "date")
    private LocalDate date;
}
