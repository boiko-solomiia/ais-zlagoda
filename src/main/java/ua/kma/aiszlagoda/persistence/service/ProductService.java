package ua.kma.aiszlagoda.persistence.service;

import org.springframework.stereotype.Service;
import ua.kma.aiszlagoda.persistence.model.Product;
import ua.kma.aiszlagoda.persistence.model.Response;

import java.sql.*;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

@Service
public class ProductService {

    private final Connection connection;

    public ProductService(Connection connection) {
        this.connection = connection;
    }

    private List<String> validateProduct(Product product) {
        List<String> errors = new LinkedList<>();

        if (product.getCategoryNumber() == null) {
            errors.add("Category must be selected");
        }

        if (product.getProductName() == null || product.getProductName().isBlank()) {
            errors.add("Product name can't be empty");
        }

        if (product.getManufacturer() == null || product.getManufacturer().isBlank()) {
            errors.add("Manufacturer can't be empty");
        }

        if (product.getCharacteristics() == null || product.getCharacteristics().isBlank()) {
            errors.add("Characteristics can't be empty");
        }

        return errors;
    }

    private Product productFromResultSet(ResultSet resultSet) throws SQLException {
        return new Product(
                resultSet.getInt("product_id"),
                resultSet.getInt("category_number"),
                resultSet.getString("product_name"),
                resultSet.getString("manufacturer"),
                resultSet.getString("characteristics")
        );
    }

    private Response<List<Product>> getListResponse(String query) {
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            ResultSet resultSet = statement.executeQuery();
            List<Product> products = new LinkedList<>();

            while (resultSet.next()) {
                products.add(productFromResultSet(resultSet));
            }

            return new Response<>(products, new LinkedList<>());
        } catch (SQLException e) {
            return new Response<>(null, Collections.singletonList(e.getMessage()));
        }
    }

    private Response<List<Product>> getListResponse(String query, Object... params) {
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            for (int i = 0; i < params.length; i++) {
                statement.setObject(i + 1, params[i]);
            }

            ResultSet resultSet = statement.executeQuery();
            List<Product> products = new LinkedList<>();

            while (resultSet.next()) {
                products.add(productFromResultSet(resultSet));
            }

            return new Response<>(products, new LinkedList<>());
        } catch (SQLException e) {
            return new Response<>(null, Collections.singletonList(e.getMessage()));
        }
    }

    public Response<Product> createProduct(Product product) {
        List<String> errors = validateProduct(product);
        if (!errors.isEmpty()) {
            return new Response<>(null, errors);
        }

        String query = "INSERT INTO product (category_number, product_name, manufacturer, characteristics) VALUES (?, ?, ?, ?)";

        try (PreparedStatement statement =
                     connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {

            statement.setInt(1, product.getCategoryNumber());
            statement.setString(2, product.getProductName());
            statement.setString(3, product.getManufacturer());
            statement.setString(4, product.getCharacteristics());

            int rows = statement.executeUpdate();
            if (rows == 0) {
                return new Response<>(null, Collections.singletonList("Failed to save product"));
            }

            ResultSet generatedKeys = statement.getGeneratedKeys();
            if (generatedKeys.next()) {
                product.setProductId(generatedKeys.getInt(1));
            }

            return new Response<>(product, new LinkedList<>());
        } catch (SQLException e) {
            return new Response<>(null, Collections.singletonList(e.getMessage()));
        }
    }

    public Response<Product> updateProduct(Product product) {
        List<String> errors = validateProduct(product);
        if (!errors.isEmpty()) {
            return new Response<>(null, errors);
        }

        if (findProductById(product.getProductId()).getObject() == null) {
            return new Response<>(null, Collections.singletonList("Can't update nonexistent product"));
        }

        String query = "UPDATE product SET category_number = ?, product_name = ?, manufacturer = ?, characteristics = ? WHERE product_id = ?";

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, product.getCategoryNumber());
            statement.setString(2, product.getProductName());
            statement.setString(3, product.getManufacturer());
            statement.setString(4, product.getCharacteristics());
            statement.setInt(5, product.getProductId());

            int rows = statement.executeUpdate();
            if (rows == 0) {
                return new Response<>(null, Collections.singletonList("Failed to update product"));
            }

            return new Response<>(product, new LinkedList<>());
        } catch (SQLException e) {
            return new Response<>(null, Collections.singletonList(e.getMessage()));
        }
    }

    public Response<Product> deleteProduct(int productId) {
        if (findProductById(productId).getObject() == null) {
            return new Response<>(null, Collections.singletonList("Can't delete nonexistent product"));
        }

        String query = "DELETE FROM product WHERE product_id = ?";

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, productId);

            int rows = statement.executeUpdate();
            if (rows == 0) {
                return new Response<>(null, Collections.singletonList("Failed to delete product"));
            }

            return new Response<>(null, new LinkedList<>());
        } catch (SQLException e) {
            return new Response<>(null, Collections.singletonList(e.getMessage()));
        }
    }

    public Response<List<Product>> findAll() {
        String query = "SELECT * FROM product ORDER BY product_name";
        return getListResponse(query);
    }

    public Response<Product> findProductById(int productId) {
        String query = "SELECT * FROM product WHERE product_id = ?";

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, productId);
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                return new Response<>(productFromResultSet(resultSet), new LinkedList<>());
            } else {
                return new Response<>(null, Collections.singletonList("Product not found"));
            }
        } catch (SQLException e) {
            return new Response<>(null, Collections.singletonList(e.getMessage()));
        }
    }

    public Response<List<Product>> findProductsByCategory(int categoryNumber) {
        String query = "SELECT * FROM product WHERE category_number = ? ORDER BY product_name";
        return getListResponse(query, categoryNumber);
    }

    public Response<List<Product>> findProductsByName(String productName) {
        String query = "SELECT * FROM product WHERE product_name = ? ORDER BY product_name";
        return getListResponse(query, productName);
    }
}