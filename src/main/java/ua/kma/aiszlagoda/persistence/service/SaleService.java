package ua.kma.aiszlagoda.persistence.service;

import org.springframework.stereotype.Service;
import ua.kma.aiszlagoda.persistence.model.Sale;
import ua.kma.aiszlagoda.persistence.model.Response;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

@Service
public class SaleService {
    private final Connection connection;


    public SaleService(Connection connection) {
        this.connection = connection;
    }

    private List<String> validateSale(Sale sale) {
        List<String> errors = new LinkedList<>();

        if (sale.getUpc() == null || sale.getUpc().isBlank()) {
            errors.add("Sale upc can't be empty");
        }

        if (sale.getCheckNumber() == null || sale.getCheckNumber().isBlank()) {
            errors.add("Check must be selected");
        }

        if (sale.getProductNumber() == null) {
            errors.add("Product number can't be empty");
        }

        if (sale.getSellingPrice() == null) {
            errors.add("Selling price can't be empty");
        }

        return errors;
    }

    public Response<Sale> createSale(Sale sale) {
        List<String> errors = validateSale(sale);
        if (!errors.isEmpty()) {
            return new Response<>(null, errors);
        }

        String query = """
                INSERT INTO sale
                (upc, check_number, product_number, selling_price)
                VALUES (?, ?, ?, ?)
                """;

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, sale.getUpc());
            statement.setString(2, sale.getCheckNumber());
            statement.setInt(3, sale.getProductNumber());
            statement.setDouble(4, sale.getSellingPrice());

            int rows = statement.executeUpdate();
            if (rows == 0) {
                return new Response<>(null, Collections.singletonList("Failed to save sale"));
            }

            return new Response<>(sale, new LinkedList<>());
        } catch (SQLException e) {
            return new Response<>(null, Collections.singletonList(e.getMessage()));
        }
    }

    public Response<Sale> deleteSale(String upc, String checkNumber) {
        if (findSaleByUpcAndCheck(upc, checkNumber).getObject() == null) {
            return new Response<>(null, Collections.singletonList("Can't delete nonexistent sale"));
        }

        String query = "DELETE FROM sale WHERE upc = ? AND check_number = ?";

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, upc);
            statement.setString(2, checkNumber);

            int rows = statement.executeUpdate();
            if (rows == 0) {
                return new Response<>(null, Collections.singletonList("Failed to delete check"));
            }

            return new Response<>(null, new LinkedList<>());
        } catch (SQLException e) {
            return new Response<>(null, Collections.singletonList(e.getMessage()));
        }
    }

    private Response<Sale> findSaleByUpcAndCheck(String upc, String checkNumber) {
        String query = "SELECT * FROM sale WHERE upc = ? AND check_number = ?";

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, upc);
            statement.setString(2, checkNumber);
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                return new Response<>(saleFromResultSet(resultSet), new LinkedList<>());
            } else {
                return new Response<>(null, Collections.singletonList("Sale not found"));
            }
        } catch (SQLException e) {
            return new Response<>(null, Collections.singletonList(e.getMessage()));
        }
    }

    public Response<List<Sale>> findSalesForCheck(String checkNumber) {
        String query = "SELECT * FROM sale WHERE check_number = ?";

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, checkNumber);

            ResultSet resultSet = statement.executeQuery();
            List<Sale> sales = new LinkedList<>();

            while (resultSet.next()) {
                sales.add(saleFromResultSet(resultSet));
            }

            return new Response<>(sales, new LinkedList<>());
        } catch (SQLException e) {
            return new Response<>(null, Collections.singletonList(e.getMessage()));
        }
    }

    public double productNumberTotalByPeriod(String upc, LocalDateTime start, LocalDateTime end) {
        String query = """
            SELECT COALESCE(SUM(product_number), 0) AS total
            FROM my_check
            INNER JOIN sale ON my_check.check_number = sale.check_number
            WHERE upc = ? AND print_date BETWEEN ? AND ?
            """;

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, upc);
            statement.setTimestamp(2, Timestamp.valueOf(start));
            statement.setTimestamp(3, Timestamp.valueOf(end));
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getDouble("total");
            }
            return 0.0;
        } catch (SQLException e) {
            return 0.0;
        }
    }

    public boolean checkStockAvailability(String upc, int requestedQuantity) {
        String query = """
        SELECT products_number 
        FROM store_product 
        WHERE upc = ? AND products_number >= ?
        """;

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, upc);
            statement.setInt(2, requestedQuantity);
            ResultSet resultSet = statement.executeQuery();
            return resultSet.next();
        } catch (SQLException e) {
            return false;
        }
    }

    private Sale saleFromResultSet(ResultSet resultSet) throws SQLException {
        return new Sale(
                resultSet.getString("upc"),
                resultSet.getString("check_number"),
                resultSet.getInt("product_number"),
                resultSet.getDouble("selling_price")
        );
    }
}
