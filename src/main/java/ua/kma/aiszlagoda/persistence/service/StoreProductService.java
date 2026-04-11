package ua.kma.aiszlagoda.persistence.service;

import org.springframework.stereotype.Service;
import ua.kma.aiszlagoda.persistence.model.Response;
import ua.kma.aiszlagoda.persistence.model.StoreProduct;
import ua.kma.aiszlagoda.persistence.model.StoreProductInfo;

import java.sql.*;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

@Service
public class StoreProductService {

    private final Connection connection;

    public StoreProductService(Connection connection) {
        this.connection = connection;
    }

    private List<String> validateStoreProduct(StoreProduct storeProduct) {
        List<String> errors = new LinkedList<>();

        if (storeProduct.getUpc() == null || storeProduct.getUpc().isBlank()) {
            errors.add("UPC can't be empty");
        }

        if (storeProduct.getUpcProm() != null && storeProduct.getUpcProm().equals(storeProduct.getUpc())) {
            errors.add("UPC prom can't be same as UPC");
        }

        if (storeProduct.getProductId() == null) {
            errors.add("Product must be selected");
        }

        if (storeProduct.getSellingPrice() == null) {
            errors.add("Selling price can't be empty");
        } else if (storeProduct.getSellingPrice() < 0) {
            errors.add("Selling price can't be negative");
        }

        if (storeProduct.getProductsNumber() == null) {
            errors.add("Products number can't be empty");
        } else if (storeProduct.getProductsNumber() < 0) {
            errors.add("Products number can't be negative");
        }

        return errors;
    }

    private StoreProduct storeProductFromResultSet(ResultSet resultSet) throws SQLException {
        return new StoreProduct(
                resultSet.getString("upc"),
                resultSet.getString("upc_prom"),
                resultSet.getInt("product_id"),
                resultSet.getDouble("selling_price"),
                resultSet.getInt("products_number"),
                resultSet.getBoolean("promotional_product")
        );
    }

    private StoreProductInfo storeProductInfoFromResultSet(ResultSet resultSet) throws SQLException {
        return new StoreProductInfo(
                resultSet.getString("upc"),
                resultSet.getString("product_name"),
                resultSet.getString("characteristics"),
                resultSet.getDouble("selling_price"),
                resultSet.getInt("products_number")
        );
    }

    private void insertStoreProduct(StoreProduct sp) throws SQLException {
        String query = """
                    INSERT INTO store_product
                    (upc, upc_prom, product_id, selling_price, products_number, promotional_product)
                    VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, sp.getUpc());
            if (sp.getUpcProm() == null || sp.getUpcProm().isBlank()) {
                statement.setNull(2, Types.VARCHAR);
            } else {
                statement.setString(2, sp.getUpcProm());
            }
            statement.setInt(3, sp.getProductId());
            statement.setDouble(4, sp.getSellingPrice());
            statement.setInt(5, sp.getProductsNumber());
            statement.setBoolean(6, sp.isPromotionalProduct());
            int rows = statement.executeUpdate();
            if (rows == 0) {
                throw new SQLException("Failed to insert store product");
            }
        }
    }

    private Response<List<StoreProductInfo>> getListResponse(String query) {
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            ResultSet resultSet = statement.executeQuery();
            List<StoreProductInfo> storeProducts = new LinkedList<>();

            while (resultSet.next()) {
                storeProducts.add(storeProductInfoFromResultSet(resultSet));
            }

            return new Response<>(storeProducts, new LinkedList<>());
        } catch (SQLException e) {
            return new Response<>(null, Collections.singletonList(e.getMessage()));
        }
    }

    public Response<StoreProduct> createStoreProduct(StoreProduct storeProduct) {
        List<String> errors = validateStoreProduct(storeProduct);
        if (!errors.isEmpty()) {
            return new Response<>(null, errors);
        }
        try {
            connection.setAutoCommit(false);
            if (!storeProduct.isPromotionalProduct() &&
                    storeProduct.getUpcProm() != null &&
                    !storeProduct.getUpcProm().isBlank()) {
                StoreProduct promo = new StoreProduct();
                promo.setUpc(storeProduct.getUpcProm());
                promo.setUpcProm(null);
                promo.setProductId(storeProduct.getProductId());
                promo.setSellingPrice(storeProduct.getSellingPrice() * 0.8);
                promo.setProductsNumber(storeProduct.getProductsNumber());
                promo.setPromotionalProduct(true);
                insertStoreProduct(promo);
            }
            insertStoreProduct(storeProduct);
            connection.commit();
            return new Response<>(storeProduct, new LinkedList<>());
        } catch (Exception e) {
            try {
                connection.rollback();
            } catch (SQLException ignored) {
            }
            return new Response<>(null, Collections.singletonList(e.getMessage()));
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException ignored) {
            }
        }
    }

    public Response<StoreProduct> updateStoreProduct(StoreProduct storeProduct) {
        List<String> errors = validateStoreProduct(storeProduct);
        if (!errors.isEmpty()) {
            return new Response<>(null, errors);
        }

        if (findStoreProductByUPC(storeProduct.getUpc()).getObject() == null) {
            return new Response<>(null, Collections.singletonList("Can't update nonexistent store product"));
        }
        try {
            connection.setAutoCommit(false);

            String query = """
                        UPDATE store_product
                        SET upc_prom = ?, product_id = ?, selling_price = ?, products_number = ?, promotional_product = ?
                        WHERE upc = ?
                    """;

            try (PreparedStatement statement = connection.prepareStatement(query)) {
                if (storeProduct.getUpcProm() == null || storeProduct.getUpcProm().isBlank()) {
                    statement.setNull(1, Types.VARCHAR);
                } else {
                    statement.setString(1, storeProduct.getUpcProm());
                }
                statement.setInt(2, storeProduct.getProductId());
                statement.setDouble(3, storeProduct.getSellingPrice());
                statement.setInt(4, storeProduct.getProductsNumber());
                statement.setBoolean(5, storeProduct.isPromotionalProduct());
                statement.setString(6, storeProduct.getUpc());

                int rows = statement.executeUpdate();
                if (rows == 0) {
                    throw new SQLException("Failed to update store product");
                }
                if (!storeProduct.isPromotionalProduct() &&
                        storeProduct.getUpcProm() != null &&
                        !storeProduct.getUpcProm().isBlank()) {

                    String promoQuery = """
                                UPDATE store_product
                                SET product_id = ?, selling_price = ?, products_number = ?
                                WHERE upc = ?
                            """;

                    try (PreparedStatement promStatement = connection.prepareStatement(promoQuery)) {
                        promStatement.setInt(1, storeProduct.getProductId());
                        promStatement.setDouble(2, storeProduct.getSellingPrice() * 0.8);
                        promStatement.setInt(3, storeProduct.getProductsNumber());
                        promStatement.setString(4, storeProduct.getUpcProm());
                        int promoRows = promStatement.executeUpdate();
                        if (promoRows == 0) {
                            throw new SQLException("Failed to update promotional store product");
                        }
                    }
                }

                connection.commit();
                return new Response<>(storeProduct, new LinkedList<>());

            }
        } catch (Exception e) {
            try {
                connection.rollback();
            } catch (SQLException ignored) {
            }
            return new Response<>(null, Collections.singletonList(e.getMessage()));
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException ignored) {
            }
        }
    }

    public Response<StoreProduct> deleteStoreProduct(String upc) {
        Response<StoreProduct> exist = findStoreProductByUPC(upc);
        if (exist.getObject() == null) {
            return new Response<>(null, Collections.singletonList("Can't delete nonexistent store product"));
        }
        StoreProduct product = exist.getObject();
        try {
            connection.setAutoCommit(false);
            if (!product.isPromotionalProduct() &&
                    product.getUpcProm() != null &&
                    !product.getUpcProm().isBlank()) {
                try (PreparedStatement statement = connection.prepareStatement(
                        "DELETE FROM store_product WHERE upc = ?")) {
                    statement.setString(1, product.getUpcProm());
                    statement.executeUpdate();
                }
            }

            if (product.isPromotionalProduct()) {
                try (PreparedStatement statement = connection.prepareStatement(
                        "UPDATE store_product SET upc_prom = NULL WHERE upc_prom = ?")) {
                    statement.setString(1, upc);
                    statement.executeUpdate();
                }
            }

            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM store_product WHERE upc = ?")) {
                statement.setString(1, upc);
                int rows = statement.executeUpdate();
                if (rows == 0) {
                    throw new SQLException("Failed to delete store product");
                }
            }

            connection.commit();
            return new Response<>(null, new LinkedList<>());

        } catch (Exception e) {
            try {
                connection.rollback();
            } catch (SQLException ignored) {
            }
            return new Response<>(null, Collections.singletonList(e.getMessage()));
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException ignored) {
            }
        }
    }

    public Response<List<StoreProduct>> findAll() {
        String query = "SELECT * FROM store_product ORDER BY products_number";

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            ResultSet resultSet = statement.executeQuery();
            List<StoreProduct> storeProducts = new LinkedList<>();

            while (resultSet.next()) {
                storeProducts.add(storeProductFromResultSet(resultSet));
            }

            return new Response<>(storeProducts, new LinkedList<>());
        } catch (SQLException e) {
            return new Response<>(null, Collections.singletonList(e.getMessage()));
        }
    }

    public Response<StoreProduct> findStoreProductByUPC(String upc) {
        String query = "SELECT * FROM store_product WHERE upc = ?";

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, upc);
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                return new Response<>(storeProductFromResultSet(resultSet), new LinkedList<>());
            } else {
                return new Response<>(null, Collections.singletonList("Store product not found"));
            }
        } catch (SQLException e) {
            return new Response<>(null, Collections.singletonList(e.getMessage()));
        }
    }

    public Response<StoreProductInfo> findStoreProductInfoByUPC(String upc) {
        String query = """
                SELECT sp.upc,
                       p.product_name,
                       p.characteristics,
                       sp.selling_price,
                       sp.products_number
                FROM store_product sp
                JOIN product p ON sp.product_id = p.product_id
                WHERE sp.upc = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, upc);
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                return new Response<>(storeProductInfoFromResultSet(resultSet), new LinkedList<>());
            } else {
                return new Response<>(null, new LinkedList<>());
            }
        } catch (SQLException e) {
            return new Response<>(null, Collections.singletonList(e.getMessage()));
        }
    }

    public Response<List<StoreProductInfo>> findAllStoreProductsSortedByName() {
        String query = """
                SELECT sp.upc,
                       p.product_name,
                       p.characteristics,
                       sp.selling_price,
                       sp.products_number,
                       sp.promotional_product
                FROM store_product sp
                JOIN product p ON sp.product_id = p.product_id
                ORDER BY p.product_name ASC
                """;

        return getListResponse(query);
    }

    public Response<List<StoreProductInfo>> findAllStoreProductsSortedByQuantity() {
        String query = """
        SELECT sp.upc,
               p.product_name,
               p.characteristics,
               sp.selling_price,
               sp.products_number
        FROM store_product sp
        JOIN product p ON sp.product_id = p.product_id
        ORDER BY sp.products_number ASC
        """;
        return getListResponse(query);
    }

    public Response<List<StoreProductInfo>> findPromotionalProductsSortedByName() {
        String query = """
                SELECT sp.upc,
                       p.product_name,
                       p.characteristics,
                       sp.selling_price,
                       sp.products_number
                FROM store_product sp
                JOIN product p ON sp.product_id = p.product_id
                WHERE sp.promotional_product = true
                ORDER BY p.product_name
                """;

        return getListResponse(query);
    }

    public Response<List<StoreProductInfo>> findNonPromotionalProductsSortedByName() {
        String query = """
                SELECT sp.upc,
                       p.product_name,
                       p.characteristics,
                       sp.selling_price,
                       sp.products_number
                FROM store_product sp
                JOIN product p ON sp.product_id = p.product_id
                WHERE sp.promotional_product = false
                ORDER BY p.product_name
                """;

        return getListResponse(query);
    }

    public Response<List<StoreProductInfo>> findPromotionalProductsSortedByQuantity() {
        String query = """
                SELECT sp.upc,
                       p.product_name,
                       p.characteristics,
                       sp.selling_price,
                       sp.products_number
                FROM store_product sp
                JOIN product p ON sp.product_id = p.product_id
                WHERE sp.promotional_product = true
                ORDER BY sp.products_number
                """;

        return getListResponse(query);
    }

    public Response<List<StoreProductInfo>> findNonPromotionalProductsSortedByQuantity() {
        String query = """
                SELECT sp.upc,
                       p.product_name,
                       p.characteristics,
                       sp.selling_price,
                       sp.products_number
                FROM store_product sp
                JOIN product p ON sp.product_id = p.product_id
                WHERE sp.promotional_product = false
                ORDER BY sp.products_number
                """;

        return getListResponse(query);
    }

    public Response<Double> getCurrentSellingPrice(String upc) {
        String query = """
                SELECT selling_price
                FROM store_product
                WHERE upc = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, upc);
            ResultSet resultSet = statement.executeQuery();

            if (!resultSet.next()) {
                return new Response<>(null, Collections.singletonList("Can't get current selling price for nonexistent store product"));
            }

            double price = resultSet.getDouble("selling_price");
            return new Response<>(price, new LinkedList<>());
        } catch (SQLException e) {
            return new Response<>(null, Collections.singletonList(e.getMessage()));
        }
    }

    public Response<Boolean> checkStockAvailability(String upc, int requestedQuantity) {
        String query = """
                SELECT products_number
                FROM store_product
                WHERE upc = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, upc);
            ResultSet resultSet = statement.executeQuery();

            if (!resultSet.next()) {
                return new Response<>(false, Collections.singletonList("Can't check stock for nonexistent store product"));
            }

            int availableQuantity = resultSet.getInt("products_number");
            boolean isAvailable = availableQuantity >= requestedQuantity;

            if (!isAvailable) {
                return new Response<>(false, Collections.singletonList(
                        "Not enough stock. Available: " + availableQuantity + ", requested: " + requestedQuantity
                ));
            }
            return new Response<>(true, new LinkedList<>());
        } catch (SQLException e) {
            return new Response<>(null, Collections.singletonList(e.getMessage()));
        }
    }

    public Response<Void> updateStockAfterSale(String upc, Integer soldQuantity) {
        if (soldQuantity == null || soldQuantity <= 0) {
            return new Response<>(null, Collections.singletonList("Quantity must be positive"));
        }

        String query = """
                UPDATE store_product
                SET products_number = products_number - ?
                WHERE upc = ? AND products_number >= ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, soldQuantity);
            statement.setString(2, upc);
            statement.setInt(3, soldQuantity);

            int rowsUpdated = statement.executeUpdate();
            if (rowsUpdated == 0) {
                Response<StoreProduct> productCheck = findStoreProductByUPC(upc);
                if (productCheck.getObject() == null) {
                    return new Response<>(null, Collections.singletonList("Can't update stock for nonexistent store product"));
                } else {
                    return new Response<>(null, Collections.singletonList("Not enough stock"));
                }
            }
            return new Response<>(null, new LinkedList<>());
        } catch (SQLException e) {
            return new Response<>(null, Collections.singletonList(e.getMessage()));
        }
    }
}