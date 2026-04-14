package ua.kma.aiszlagoda.persistence.service;

import org.springframework.stereotype.Service;
import ua.kma.aiszlagoda.persistence.model.Check;
import ua.kma.aiszlagoda.persistence.model.CheckDTO;
import ua.kma.aiszlagoda.persistence.model.Response;
import ua.kma.aiszlagoda.persistence.model.SaleRequest;

import java.time.LocalDateTime;
import java.sql.*;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

@Service
public class CheckService {

    private final Connection connection;
    private final SaleService saleService;

    public CheckService(Connection connection, SaleService saleService) {
        this.connection = connection;
        this.saleService = saleService;
    }

    private Check getCheckFromResultSet(ResultSet rs) throws SQLException {
        Timestamp ts = rs.getTimestamp("print_date");
        return new Check(
                rs.getString("check_number"),
                rs.getString("id_employee"),
                rs.getString("card_number"),
                ts != null ? ts.toLocalDateTime() : null,
                rs.getDouble("sum_total"),
                rs.getDouble("vat")
        );
    }

    private CheckDTO getCheckDTOFromResultSet(ResultSet rs) throws SQLException {
        CheckDTO dto = new CheckDTO();
        dto.setCheckNumber(rs.getString("check_number"));
        dto.setIdEmployee(rs.getString("id_employee"));
        dto.setEmployeeFullName(rs.getString("employee_name"));
        dto.setCardNumber(rs.getString("card_number"));

        String custName = rs.getString("customer_name");
        dto.setCustomerFullName(custName == null || custName.isBlank() ? "—" : custName);

        Timestamp ts = rs.getTimestamp("print_date");
        dto.setPrintDate(ts != null ? ts.toLocalDateTime() : null);
        dto.setSumTotal(rs.getDouble("sum_total"));
        dto.setVat(rs.getDouble("vat"));
        return dto;
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

    public Response<Check> createCheck(Check myCheck) {
        List<String> errors = validateCheck(myCheck);
        if (!errors.isEmpty()) {
            return new Response<>(null, errors);
        }

        double sumTotal = saleService.calculateSumTotal(myCheck.getCheckNumber());
        double vat = sumTotal * 0.2;
        myCheck.setSumTotal(sumTotal);
        myCheck.setVat(vat);

        String query = "INSERT INTO my_check (check_number, id_employee, card_number, print_date, sum_total, vat) VALUES (?, ?, ?, ?, ?, ?)";

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

    public Response<Check> createCheckWithSales(Check myCheck, List<SaleRequest> items) {
        try {
            connection.setAutoCommit(false);
            Response<Void> stockCheck = saleService.checkStockForSaleItems(items);
            if (!stockCheck.getErrors().isEmpty()) {
                connection.rollback();
                return new Response<>(null, stockCheck.getErrors());
            }

            Response<Double> totalResponse = saleService.calculateTotalForSaleItems(items);
            if (!totalResponse.getErrors().isEmpty()) {
                connection.rollback();
                return new Response<>(null, totalResponse.getErrors());
            }

            double total = totalResponse.getObject();
            myCheck.setSumTotal(total);
            myCheck.setVat(total * 0.2);

            Response<Check> checkResponse = createCheck(myCheck);
            if (!checkResponse.getErrors().isEmpty()) {
                connection.rollback();
                return checkResponse;
            }

            Response<Void> salesResponse = saleService.createSalesFromRequests(items, myCheck.getCheckNumber());
            if (!salesResponse.getErrors().isEmpty()) {
                connection.rollback();
                return new Response<>(null, salesResponse.getErrors());
            }
            connection.commit();
            return new Response<>(myCheck, new LinkedList<>());
        } catch (SQLException e) {
            try { connection.rollback(); } catch (SQLException ex) { /* ignored */ }
            return new Response<>(null, Collections.singletonList(e.getMessage()));
        } finally {
            try { connection.setAutoCommit(true); } catch (SQLException e) { /* ignored */ }
        }
    }

    public Response<List<CheckDTO>> findAllDTO() {
        List<CheckDTO> checks = new LinkedList<>();
        String query = "SELECT c.*, " +
                "TRIM(CONCAT(e.empl_surname, ' ', e.empl_name, ' ', IFNULL(e.empl_patronymic, ''))) as employee_name, " +
                "TRIM(CONCAT(IFNULL(cc.cust_surname, ''), ' ', IFNULL(cc.cust_name, ''))) as customer_name " +
                "FROM my_check c " +
                "JOIN employee e ON c.id_employee = e.id_employee " +
                "LEFT JOIN customer_card cc ON c.card_number = cc.card_number " +
                "ORDER BY c.print_date DESC";

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                checks.add(getCheckDTOFromResultSet(rs));
            }
            return new Response<>(checks, new LinkedList<>());
        } catch (SQLException e) {
            return new Response<>(null, Collections.singletonList(e.getMessage()));
        }
    }

    public Response<List<CheckDTO>> findChecksDTOByEmployeeAndPeriod(String idEmployee, LocalDateTime start, LocalDateTime end) {
        List<CheckDTO> checks = new LinkedList<>();
        String query = "SELECT c.*, " +
                "TRIM(CONCAT(e.empl_surname, ' ', e.empl_name, ' ', IFNULL(e.empl_patronymic, ''))) as employee_name, " +
                "TRIM(CONCAT(IFNULL(cc.cust_surname, ''), ' ', IFNULL(cc.cust_name, ''))) as customer_name " +
                "FROM my_check c " +
                "JOIN employee e ON c.id_employee = e.id_employee " +
                "LEFT JOIN customer_card cc ON c.card_number = cc.card_number " +
                "WHERE c.id_employee = ? AND c.print_date BETWEEN ? AND ? " +
                "ORDER BY c.print_date DESC";

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, idEmployee);
            statement.setTimestamp(2, Timestamp.valueOf(start));
            statement.setTimestamp(3, Timestamp.valueOf(end));
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                checks.add(getCheckDTOFromResultSet(rs));
            }
            return new Response<>(checks, new LinkedList<>());
        } catch (SQLException e) {
            return new Response<>(null, Collections.singletonList(e.getMessage()));
        }
    }

    public Response<List<CheckDTO>> findChecksDTOByPeriod(LocalDateTime start, LocalDateTime end) {
        List<CheckDTO> checks = new LinkedList<>();
        String query = "SELECT c.*, " +
                "TRIM(CONCAT(e.empl_surname, ' ', e.empl_name, ' ', IFNULL(e.empl_patronymic, ''))) as employee_name, " +
                "TRIM(CONCAT(IFNULL(cc.cust_surname, ''), ' ', IFNULL(cc.cust_name, ''))) as customer_name " +
                "FROM my_check c " +
                "JOIN employee e ON c.id_employee = e.id_employee " +
                "LEFT JOIN customer_card cc ON c.card_number = cc.card_number " +
                "WHERE c.print_date BETWEEN ? AND ? " +
                "ORDER BY c.print_date DESC";

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setTimestamp(1, Timestamp.valueOf(start));
            statement.setTimestamp(2, Timestamp.valueOf(end));
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                checks.add(getCheckDTOFromResultSet(rs));
            }
            return new Response<>(checks, new LinkedList<>());
        } catch (SQLException e) {
            return new Response<>(null, Collections.singletonList(e.getMessage()));
        }
    }

    public Response<Check> findCheckByNumber(String num) {
        String query = "SELECT * FROM my_check WHERE check_number = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, num);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return new Response<>(getCheckFromResultSet(rs), new LinkedList<>());
            return new Response<>(null, Collections.singletonList("Check not found"));
        } catch (SQLException e) {
            return new Response<>(null, Collections.singletonList(e.getMessage()));
        }
    }

    public Response<Check> deleteCheck(String num) {
        try {
            connection.setAutoCommit(false);
            String query = "DELETE FROM my_check WHERE check_number = ?";
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setString(1, num);
                int rows = stmt.executeUpdate();
                if (rows == 0) {
                    connection.rollback();
                    return new Response<>(null, Collections.singletonList("Check not found"));
                }
            }
            connection.commit();
            return new Response<>(null, new LinkedList<>());
        } catch (SQLException e) {
            try { connection.rollback(); } catch (SQLException ex) { /* ignored */ }
            return new Response<>(null, Collections.singletonList(e.getMessage()));
        } finally {
            try { connection.setAutoCommit(true); } catch (SQLException e) { /* ignored */ }
        }
    }

    public double checkSumTotalByEmployeeAndPeriod(String employeeId, LocalDateTime start, LocalDateTime end) {
        String query = "SELECT COALESCE(SUM(sum_total), 0) AS total FROM my_check WHERE id_employee = ? AND print_date BETWEEN ? AND ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, employeeId);
            statement.setTimestamp(2, Timestamp.valueOf(start));
            statement.setTimestamp(3, Timestamp.valueOf(end));
            ResultSet rs = statement.executeQuery();
            return rs.next() ? rs.getDouble("total") : 0.0;
        } catch (SQLException e) {
            return 0.0;
        }
    }

    public double checkSumTotalByPeriod(LocalDateTime start, LocalDateTime end) {
        String query = "SELECT COALESCE(SUM(sum_total), 0) AS total FROM my_check WHERE print_date BETWEEN ? AND ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setTimestamp(1, Timestamp.valueOf(start));
            statement.setTimestamp(2, Timestamp.valueOf(end));
            ResultSet rs = statement.executeQuery();
            return rs.next() ? rs.getDouble("total") : 0.0;
        } catch (SQLException e) {
            return 0.0;
        }
    }
}