package ua.kma.aiszlagoda.persistence.service;

import org.springframework.stereotype.Service;
import ua.kma.aiszlagoda.persistence.model.Check;
import ua.kma.aiszlagoda.persistence.model.Response;

import java.time.LocalDateTime;
import java.sql.*;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

@Service
public class CheckService {

    private final Connection connection;

    public CheckService(Connection connection) {
        this.connection = connection;
    }

    private List<String> validateCheck(Check myCheck) {
        List<String> errors = new LinkedList<>();

        if (myCheck.getCheckNumber() == null || myCheck.getCheckNumber().isBlank()) {
            errors.add("Check number can't be empty");
        }

        if (myCheck.getIdEmployee() == null || myCheck.getIdEmployee().isBlank()) {
            errors.add("Employee id can't be empty");
        }

        if (myCheck.getPrintDate() == null) {
            errors.add("Print date can't be empty");
        } else if (myCheck.getPrintDate().isAfter(LocalDateTime.now())) {
            errors.add("Print date can't be in the future");
        }

        return errors;
    }

    private Check checkFromResultSet(ResultSet resultSet) throws SQLException {
        Timestamp timestamp = resultSet.getTimestamp("print_date");
        return new Check(
                resultSet.getString("check_number"),
                resultSet.getString("id_employee"),
                resultSet.getString("card_number"),
                timestamp != null ? timestamp.toLocalDateTime() : null,
                resultSet.getDouble("sum_total"),
                resultSet.getDouble("vat")
        );
    }

    private double calculateSumTotal(Check myCheck) {
        String query = """
                SELECT SUM(product_number * selling_price) AS total_sum
                FROM sale
                WHERE check_number = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, myCheck.getCheckNumber());
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

    public Response<Check> createCheck(Check myCheck) {
        List<String> errors = validateCheck(myCheck);
        if (!errors.isEmpty()) {
            return new Response<>(null, errors);
        }
        double sumTotal = calculateSumTotal(myCheck);
        double vat = sumTotal * 0.2;
        myCheck.setSumTotal(sumTotal);
        myCheck.setVat(vat);

        String query = """
                INSERT INTO my_check
                (check_number, id_employee, card_number, print_date, sum_total, vat)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, myCheck.getCheckNumber());
            statement.setString(2, myCheck.getIdEmployee());

            if (myCheck.getCardNumber() == null || myCheck.getCardNumber().isBlank()) {
                statement.setNull(3, Types.VARCHAR);
            } else {
                statement.setString(3, myCheck.getCardNumber());
            }

            statement.setTimestamp(4, Timestamp.valueOf(myCheck.getPrintDate()));
            statement.setDouble(5, myCheck.getSumTotal());
            statement.setDouble(6, myCheck.getVat());

            int rows = statement.executeUpdate();
            if (rows == 0) {
                return new Response<>(null, Collections.singletonList("Failed to save check"));
            }

            return new Response<>(myCheck, new LinkedList<>());
        } catch (SQLException e) {
            return new Response<>(null, Collections.singletonList(e.getMessage()));
        }
    }

    public Response<List<Check>> findAll() {
        String query = "SELECT * FROM my_check ORDER BY print_date DESC";

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            ResultSet resultSet = statement.executeQuery();
            List<Check> checks = new LinkedList<>();

            while (resultSet.next()) {
                checks.add(checkFromResultSet(resultSet));
            }

            return new Response<>(checks, new LinkedList<>());
        } catch (SQLException e) {
            return new Response<>(null, Collections.singletonList(e.getMessage()));
        }
    }

    public Response<Check> findCheckByNumber(String checkNumber) {
        String query = "SELECT * FROM my_check WHERE check_number = ?";

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, checkNumber);
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                return new Response<>(checkFromResultSet(resultSet), new LinkedList<>());
            } else {
                return new Response<>(null, Collections.singletonList("Check not found"));
            }
        } catch (SQLException e) {
            return new Response<>(null, Collections.singletonList(e.getMessage()));
        }
    }

    public Response<Check> updateCheck(Check myCheck) {
        List<String> errors = validateCheck(myCheck);
        if (!errors.isEmpty()) {
            return new Response<>(null, errors);
        }

        if (findCheckByNumber(myCheck.getCheckNumber()).getObject() == null) {
            return new Response<>(null, Collections.singletonList("Can't update nonexistent check"));
        }

        double sumTotal = calculateSumTotal(myCheck);
        double vat = sumTotal * 0.2;
        myCheck.setSumTotal(sumTotal);
        myCheck.setVat(vat);
        String query = """
                UPDATE my_check
                SET id_employee = ?, card_number = ?, print_date = ?, sum_total = ?, vat = ?
                WHERE check_number = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, myCheck.getIdEmployee());

            if (myCheck.getCardNumber() == null || myCheck.getCardNumber().isBlank()) {
                statement.setNull(2, Types.VARCHAR);
            } else {
                statement.setString(2, myCheck.getCardNumber());
            }

            statement.setTimestamp(3, Timestamp.valueOf(myCheck.getPrintDate()));
            statement.setDouble(4, myCheck.getSumTotal());
            statement.setDouble(5, myCheck.getVat());
            statement.setString(6, myCheck.getCheckNumber());

            int rows = statement.executeUpdate();
            if (rows == 0) {
                return new Response<>(null, Collections.singletonList("Failed to update check"));
            }

            return new Response<>(myCheck, new LinkedList<>());
        } catch (SQLException e) {
            return new Response<>(null, Collections.singletonList(e.getMessage()));
        }
    }

    public Response<Check> deleteCheck(String checkNumber) {
        if (findCheckByNumber(checkNumber).getObject() == null) {
            return new Response<>(null, Collections.singletonList("Can't delete nonexistent check"));
        }

        String query = "DELETE FROM my_check WHERE check_number = ?";

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, checkNumber);

            int rows = statement.executeUpdate();
            if (rows == 0) {
                return new Response<>(null, Collections.singletonList("Failed to delete check"));
            }

            return new Response<>(null, new LinkedList<>());
        } catch (SQLException e) {
            return new Response<>(null, Collections.singletonList(e.getMessage()));
        }
    }

    public Response<List<Check>> findChecksByEmployeeAndPeriod(String employeeId, LocalDateTime start, LocalDateTime end) {
        String query = """
                SELECT * FROM my_check
                WHERE id_employee = ? AND print_date BETWEEN ? AND ?
                ORDER BY print_date DESC
                """;

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, employeeId);
            statement.setTimestamp(2, Timestamp.valueOf(start));
            statement.setTimestamp(3, Timestamp.valueOf(end));

            ResultSet resultSet = statement.executeQuery();
            List<Check> checks = new LinkedList<>();

            while (resultSet.next()) {
                checks.add(checkFromResultSet(resultSet));
            }

            return new Response<>(checks, new LinkedList<>());
        } catch (SQLException e) {
            return new Response<>(null, Collections.singletonList(e.getMessage()));
        }
    }

    public Response<List<Check>> findChecksByPeriod(LocalDateTime start, LocalDateTime end) {
        String query = """
                SELECT * FROM my_check
                WHERE print_date BETWEEN ? AND ?
                ORDER BY print_date DESC
                """;

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setTimestamp(1, Timestamp.valueOf(start));
            statement.setTimestamp(2, Timestamp.valueOf(end));

            ResultSet resultSet = statement.executeQuery();
            List<Check> checks = new LinkedList<>();

            while (resultSet.next()) {
                checks.add(checkFromResultSet(resultSet));
            }

            return new Response<>(checks, new LinkedList<>());
        } catch (SQLException e) {
            return new Response<>(null, Collections.singletonList(e.getMessage()));
        }
    }

    public double checkSumTotalByEmployeeAndPeriod(String employeeId, LocalDateTime start, LocalDateTime end) {
        String query = """
            SELECT COALESCE(SUM(sum_total), 0) AS total
            FROM my_check
            WHERE id_employee = ? AND print_date BETWEEN ? AND ?
            """;

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, employeeId);
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

    public double checkSumTotalByPeriod(LocalDateTime start, LocalDateTime end) {
        String query = """
            SELECT COALESCE(SUM(sum_total), 0) AS total
            FROM my_check
            WHERE print_date BETWEEN ? AND ?
            """;

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setTimestamp(1, Timestamp.valueOf(start));
            statement.setTimestamp(2, Timestamp.valueOf(end));
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getDouble("total");
            }
            return 0.0;
        } catch (SQLException e) {
            return 0.0;
        }
    }
}