CREATE TABLE shopping_lists (
    list_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    list_name VARCHAR(150) NOT NULL,
    spending_limit DECIMAL(10, 2) NULL,
    date DATE NULL,

    CONSTRAINT pk_shopping_lists
        PRIMARY KEY (list_id),

    CONSTRAINT fk_shopping_lists_user
        FOREIGN KEY (user_id)
        REFERENCES users (user_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,

    CONSTRAINT chk_shopping_lists_spending_limit
        CHECK (
            spending_limit IS NULL
            OR spending_limit >= 0
        )
);
