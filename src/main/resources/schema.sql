CREATE TABLE IF NOT EXISTS category (
    category_number INT AUTO_INCREMENT PRIMARY KEY,
    category_name VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS product (
    product_id INT AUTO_INCREMENT PRIMARY KEY,
    category_number INT NOT NULL,
    product_name VARCHAR(100) NOT NULL,
    manufacturer VARCHAR(100) NOT NULL,
    characteristics VARCHAR(255) NOT NULL,
    CONSTRAINT fk_product_category
    FOREIGN KEY (category_number) REFERENCES category(category_number)
    ON DELETE RESTRICT
    ON UPDATE CASCADE
);

CREATE TABLE IF NOT EXISTS store_product (
    upc VARCHAR(20) PRIMARY KEY,
    product_id INT NOT NULL,
    selling_price DECIMAL(10,2) NOT NULL,
    products_number INT NOT NULL,
    promotional_product BOOLEAN NOT NULL,
    CONSTRAINT fk_store_product_product
    FOREIGN KEY (product_id) REFERENCES product(product_id)
    ON DELETE RESTRICT
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