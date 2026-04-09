package ua.kma.aiszlagoda.persistence.service;

import org.springframework.stereotype.Service;
import ua.kma.aiszlagoda.persistence.model.Sale;
import ua.kma.aiszlagoda.persistence.model.Response;
import ua.kma.aiszlagoda.persistence.model.SaleRequest;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

@Service
public class SaleService {
    private final Connection connection;
    private final StoreProductService storeProductService;

    public SaleService(Connection connection, StoreProductService storeProductService) {
        this.connection = connection;
        this.storeProductService = storeProductService;
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

    private Sale saleFromResultSet(ResultSet resultSet) throws SQLException {
        return new Sale(
                resultSet.getString("upc"),
                resultSet.getString("check_number"),
                resultSet.getInt("product_number"),
                resultSet.getDouble("selling_price")
        );
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

    public Response<Sale> createSale(Sale sale) {
        List<String> errors = validateSale(sale);
        if (!errors.isEmpty()) {
            return new Response<>(null, errors);
        }

        if (!storeProductService.checkStockAvailability(sale.getUpc(), sale.getProductNumber()).getObject()) {
            return new Response<>(null, Collections.singletonList(
                    "Not enough stock for sale"
            ));
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

            Response<Void> stockUpdate = storeProductService.updateStockAfterSale(sale.getUpc(), sale.getProductNumber());
            if (!stockUpdate.getErrors().isEmpty()) {
                return new Response<>(null, stockUpdate.getErrors());
            }

            return new Response<>(sale, new LinkedList<>());
        } catch (SQLException e) {
            return new Response<>(null, Collections.singletonList(e.getMessage()));
        }
    }

    public Response<Void> createSalesFromRequests(List<SaleRequest> requests, String checkNumber) {
        for (SaleRequest item : requests) {
            Response<Double> priceResponse = storeProductService.getCurrentSellingPrice(item.getUpc());
            if (!priceResponse.getErrors().isEmpty()) {
                return new Response<>(null, priceResponse.getErrors());
            }

            Sale sale = new Sale(
                    item.getUpc(),
                    checkNumber,
                    item.getProductNumber(),
                    priceResponse.getObject()
            );
            Response<Sale> response = createSale(sale);
            if (!response.getErrors().isEmpty()) {
                return new Response<>(null, response.getErrors());
            }
        }
        return new Response<>(null, new LinkedList<>());
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
                return new Response<>(null, Collections.singletonList("Failed to delete sale"));
            }

            return new Response<>(null, new LinkedList<>());
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

    public double calculateSumTotal(String checkNumber) {
        String query = """
                SELECT SUM(product_number * selling_price) AS total_sum
                FROM sale
                WHERE check_number = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, checkNumber);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                double total = resultSet.getDouble("total_sum");
                if (resultSet.wasNull()) {
                    return 0.0;
                }
                return total;
            }
            return 0.0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Response<Void> checkStockForSaleItems(List<SaleRequest> items) {
        for (SaleRequest item : items) {
            Response<Boolean> stockCheck = storeProductService.checkStockAvailability(
                    item.getUpc(), item.getProductNumber()
            );
            if (!stockCheck.getErrors().isEmpty()) {
                return new Response<>(null, stockCheck.getErrors());
            }
            if (!stockCheck.getObject()) {
                return new Response<>(null, Collections.singletonList(
                        "Not enough stock for UPC: " + item.getUpc()
                ));
            }
        }
        return new Response<>(null, new LinkedList<>());
    }

    public Response<Double> calculateTotalForSaleItems(List<SaleRequest> items) {
        double total = 0.0;
        for (SaleRequest item : items) {
            Response<Double> priceResponse = storeProductService.getCurrentSellingPrice(item.getUpc());
            if (!priceResponse.getErrors().isEmpty()) {
                return new Response<>(null, priceResponse.getErrors());
            }
            total += item.getProductNumber() * priceResponse.getObject();
        }
        return new Response<>(total, new LinkedList<>());
    }
}
