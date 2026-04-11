CREATE TABLE IF NOT EXISTS category (
    category_number INT AUTO_INCREMENT PRIMARY KEY,
    category_name VARCHAR(50) NOT NULL
);

CREATE TABLE IF NOT EXISTS product (
    product_id INT AUTO_INCREMENT PRIMARY KEY,
    category_number INT NOT NULL,
    product_name VARCHAR(50) NOT NULL,
    manufacturer VARCHAR(100) NOT NULL,
    characteristics VARCHAR(100) NOT NULL,
    CONSTRAINT fk_product_category
    FOREIGN KEY (category_number) REFERENCES category(category_number)
    ON DELETE NO ACTION
    ON UPDATE CASCADE
);

CREATE TABLE IF NOT EXISTS store_product (
    upc VARCHAR(12) PRIMARY KEY,
    upc_prom VARCHAR(12) NULL,
    product_id INT NOT NULL,
    selling_price DECIMAL(13,4) NOT NULL,
    products_number INT NOT NULL,
    promotional_product BOOLEAN NOT NULL,
    CONSTRAINT fk_store_product_upc_prom
    FOREIGN KEY (upc_prom) REFERENCES store_product(upc)
    ON DELETE SET NULL
    ON UPDATE CASCADE,
    CONSTRAINT fk_store_product_product
    FOREIGN KEY (product_id) REFERENCES product(product_id)
    ON DELETE NO ACTION
    ON UPDATE CASCADE
);


CREATE TABLE IF NOT EXISTS employee (
    id_employee VARCHAR(10) PRIMARY KEY,
    empl_surname VARCHAR(50) NOT NULL,
    empl_name VARCHAR(50) NOT NULL,
    empl_patronymic VARCHAR(50) NULL,
    empl_role VARCHAR(10) NOT NULL,
    salary DECIMAL(13,4) NOT NULL,
    date_of_birth DATE NOT NULL,
    date_of_start DATE NOT NULL,
    phone_number VARCHAR(13) NOT NULL,
    city VARCHAR(50) NOT NULL,
    street VARCHAR(50) NOT NULL,
    zip_code VARCHAR(9) NOT NULL
);

CREATE TABLE IF NOT EXISTS customer_card (
    card_number VARCHAR(13) PRIMARY KEY,
    cust_surname VARCHAR(50) NOT NULL,
    cust_name VARCHAR(50) NOT NULL,
    cust_patronymic VARCHAR(50) NULL,
    phone_number VARCHAR(13) NOT NULL,
    city VARCHAR(50) NULL,
    street VARCHAR(50) NULL,
    zip_code VARCHAR(9) NULL,
    percent INT NOT NULL
);

CREATE TABLE IF NOT EXISTS my_check (
    check_number VARCHAR(10) PRIMARY KEY,
    id_employee VARCHAR(10) NOT NULL,
    card_number VARCHAR(13) NULL,
    print_date DATETIME NOT NULL,
    sum_total DECIMAL(13,4) NOT NULL,
    vat DECIMAL(13,4) NOT NULL,
    CONSTRAINT fk_my_check_employee
    FOREIGN KEY (id_employee) REFERENCES employee(id_employee)
    ON DELETE NO ACTION
    ON UPDATE CASCADE,
    CONSTRAINT fk_my_check_customer_card
    FOREIGN KEY (card_number) REFERENCES customer_card(card_number)
    ON DELETE NO ACTION
    ON UPDATE CASCADE
);

CREATE TABLE IF NOT EXISTS sale (
    upc VARCHAR(12) NOT NULL,
    check_number VARCHAR(10) NOT NULL,
    product_number INT NOT NULL,
    selling_price DECIMAL(13,4) NOT NULL,
    PRIMARY KEY (upc, check_number),
    CONSTRAINT fk_sale_store_product
    FOREIGN KEY (upc) REFERENCES store_product(upc)
    ON DELETE NO ACTION
    ON UPDATE CASCADE,
    CONSTRAINT fk_sale_my_check
    FOREIGN KEY (check_number) REFERENCES my_check(check_number)
    ON DELETE CASCADE
    ON UPDATE CASCADE
);

CREATE TABLE IF NOT EXISTS user_account (
    id_account INT AUTO_INCREMENT PRIMARY KEY,
    id_employee VARCHAR(10) NOT NULL,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    CONSTRAINT fk_user_account_employee
    FOREIGN KEY (id_employee) REFERENCES employee(id_employee)
    ON DELETE CASCADE
    ON UPDATE CASCADE
);