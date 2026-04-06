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

    public Response<StoreProduct> createStoreProduct(StoreProduct storeProduct) {
        List<String> errors = validateStoreProduct(storeProduct);
        if (!errors.isEmpty()) {
            return new Response<>(null, errors);
        }

        String query = "INSERT INTO store_product (upc, product_id, selling_price, products_number, promotional_product) VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, storeProduct.getUpc());
            statement.setInt(2, storeProduct.getProductId());
            statement.setDouble(3, storeProduct.getSellingPrice());
            statement.setInt(4, storeProduct.getProductsNumber());
            statement.setBoolean(5, storeProduct.isPromotionalProduct());

            int rows = statement.executeUpdate();
            if (rows == 0) {
                return new Response<>(null, Collections.singletonList("Failed to save store product"));
            }

            return new Response<>(storeProduct, new LinkedList<>());
        } catch (SQLException e) {
            return new Response<>(null, Collections.singletonList(e.getMessage()));
        }
    }

    public Response<List<StoreProduct>> findAll() {
        String query = "SELECT * FROM store_product ORDER BY products_number ASC";

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

    public Response<StoreProduct> updateStoreProduct(StoreProduct storeProduct) {
        List<String> errors = validateStoreProduct(storeProduct);
        if (!errors.isEmpty()) {
            return new Response<>(null, errors);
        }

        if (findStoreProductByUPC(storeProduct.getUpc()).getObject() == null) {
            return new Response<>(null, Collections.singletonList("Can't update nonexistent store product"));
        }

        String query = "UPDATE store_product SET product_id = ?, selling_price = ?, products_number = ?, promotional_product = ? WHERE upc = ?";

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, storeProduct.getProductId());
            statement.setDouble(2, storeProduct.getSellingPrice());
            statement.setInt(3, storeProduct.getProductsNumber());
            statement.setBoolean(4, storeProduct.isPromotionalProduct());
            statement.setString(5, storeProduct.getUpc());

            int rows = statement.executeUpdate();
            if (rows == 0) {
                return new Response<>(null, Collections.singletonList("Failed to update store product"));
            }

            return new Response<>(storeProduct, new LinkedList<>());
        } catch (SQLException e) {
            return new Response<>(null, Collections.singletonList(e.getMessage()));
        }
    }

    public Response<StoreProduct> deleteStoreProduct(String upc) {
        if (findStoreProductByUPC(upc).getObject() == null) {
            return new Response<>(null, Collections.singletonList("Can't delete nonexistent store product"));
        }

        String query = "DELETE FROM store_product WHERE upc = ?";

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, upc);

            int rows = statement.executeUpdate();
            if (rows == 0) {
                return new Response<>(null, Collections.singletonList("Failed to delete store product"));
            }

            return new Response<>(null, new LinkedList<>());
        } catch (SQLException e) {
            return new Response<>(null, Collections.singletonList(e.getMessage()));
        }
    }

    public Response<List<StoreProduct>> findPromotionalProducts() {
        String query = "SELECT * FROM store_product WHERE promotional_product = true ORDER BY products_number ASC";

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

    public Response<List<StoreProduct>> findNonPromotionalProducts() {
        String query = "SELECT * FROM store_product WHERE promotional_product = false ORDER BY products_number ASC";

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

    private StoreProductInfo storeProductInfoFromResultSet(ResultSet resultSet) throws SQLException {
        return new StoreProductInfo(
                resultSet.getString("product_name"),
                resultSet.getString("characteristics"),
                resultSet.getDouble("selling_price"),
                resultSet.getInt("products_number")
        );
    }

    public Response<StoreProductInfo> findStoreProductInfoByUPC(String upc) {
        String query = """
            SELECT p.product_name,
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
                return new Response<>(null, Collections.singletonList("Store product info not found"));
            }
        } catch (SQLException e) {
            return new Response<>(null, Collections.singletonList(e.getMessage()));
        }
    }

    public Response<List<StoreProductInfo>> findAllStoreProductsSortedByName() {
        String query = """
            SELECT p.product_name,
                   p.characteristics,
                   sp.selling_price,
                   sp.products_number
            FROM store_product sp
            JOIN product p ON sp.product_id = p.product_id
            ORDER BY p.product_name ASC
            """;

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


    public Response<List<StoreProductInfo>> findPromotionalProductsSortedByName() {
        String query = """
            SELECT p.product_name,
                   p.characteristics,
                   sp.selling_price,
                   sp.products_number
            FROM store_product sp
            JOIN product p ON sp.product_id = p.product_id
            WHERE sp.promotional_product = true
            ORDER BY p.product_name ASC
            """;

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

    public Response<List<StoreProductInfo>> findNonPromotionalProductsSortedByName() {
        String query = """
            SELECT p.product_name,
                   p.characteristics,
                   sp.selling_price,
                   sp.products_number
            FROM store_product sp
            JOIN product p ON sp.product_id = p.product_id
            WHERE sp.promotional_product = false
            ORDER BY p.product_name ASC
            """;

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
}