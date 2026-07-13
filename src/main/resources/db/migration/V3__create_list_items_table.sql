CREATE TABLE list_items (
    item_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    list_id BIGINT UNSIGNED NOT NULL,
    item_name VARCHAR(255) NOT NULL,
    quantity INT UNSIGNED NOT NULL DEFAULT 1,
    unit_price DECIMAL(10, 2) NULL,
    is_purchased BOOLEAN NOT NULL DEFAULT FALSE,
    list_position INT UNSIGNED NOT NULL,

    CONSTRAINT pk_shopping_list_items
        PRIMARY KEY (item_id),

    CONSTRAINT fk_shopping_list_items_list
        FOREIGN KEY (list_id)
        REFERENCES shopping_lists (list_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,

    CONSTRAINT uq_shopping_list_items_position
        UNIQUE (list_id, list_position),

    CONSTRAINT chk_shopping_list_items_quantity
        CHECK (quantity > 0),

    CONSTRAINT chk_shopping_list_items_unit_price
        CHECK (
            unit_price IS NULL
            OR unit_price >= 0
        )
);
