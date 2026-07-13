package com.scottvarns.shoppinglist.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(
        name = "list_items",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_shopping_list_items_position",
                        columnNames = {"list_id", "list_position"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ListItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_id")
    private Long itemId;

    @Column(name = "list_id", nullable = false)
    private Long listId;

    @Column(name = "item_name", nullable = false, length = 255)
    private String itemName;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "is_purchased", nullable = false)
    private Boolean isPurchased;

    @Column(name = "list_position", nullable = false)
    private Integer listPosition;
}
