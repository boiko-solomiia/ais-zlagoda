package ua.kma.aiszlagoda.persistence.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ua.kma.aiszlagoda.persistence.model.Employee;
import ua.kma.aiszlagoda.persistence.model.EmployeeInfo;
import ua.kma.aiszlagoda.persistence.model.Response;

import java.sql.*;
import java.time.LocalDate;
import java.time.Period;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

@Service
public class EmployeeService {
    private final Connection connection;

    @Autowired
    public EmployeeService(Connection connection) {
        this.connection = connection;
    }

    private List<String> validateEmployee(Employee employee) {
        List<String> errors = new LinkedList<>();

        if (employee.getIdEmployee() == null || employee.getIdEmployee().isBlank()) {
            errors.add("Employee id can't be empty");
        }

        if (employee.getEmplSurname() == null || employee.getEmplSurname().isBlank()) {
            errors.add("Employee surname can't be empty");
        }

        if (employee.getEmplName() == null || employee.getEmplName().isBlank()) {
            errors.add("Employee name can't be empty");
        }

        if (employee.getEmplRole() == null || employee.getEmplRole().isBlank()) {
            errors.add("Employee role can't be empty");
        }

        if (employee.getSalary() == null) {
            errors.add("Employee salary can't be empty");
        } else if (employee.getSalary() < 0) {
            errors.add("Salary can't be negative");
        }

        if (employee.getDateOfBirth() == null) {
            errors.add("Employee date of birth can't be empty");
        } else if (!isAtLeast18YearsOld(employee.getDateOfBirth())) {
            errors.add("Employee must be at least 18 years old");
        }

        if (employee.getDateOfStart() == null) {
            errors.add("Employee date of start can't be empty");
        } else if (employee.getDateOfStart().isAfter(LocalDate.now())) {
            errors.add("Date of start can't be in the future");
        }

        if (employee.getPhoneNumber() == null || employee.getPhoneNumber().isBlank()) {
            errors.add("Employee phone can't be empty");
        } else if (employee.getPhoneNumber().length() > 13) {
            errors.add("Phone number can't exceed 13 characters including '+'");
        }

        if (employee.getCity() == null || employee.getCity().isBlank()) {
            errors.add("Employee city can't be empty");
        }

        if (employee.getStreet() == null || employee.getStreet().isBlank()) {
            errors.add("Employee street can't be empty");
        }

        if (employee.getZipCode() == null || employee.getZipCode().isBlank()) {
            errors.add("Employee zip code can't be empty");
        }

        return errors;
    }

    private boolean isAtLeast18YearsOld(LocalDate birthDate) {
        LocalDate today = LocalDate.now();
        Period period = Period.between(birthDate, today);
        return period.getYears() >= 18;
    }

    private Response<List<Employee>> getListResponse(PreparedStatement statement) throws SQLException {
        ResultSet resultSet = statement.executeQuery();
        List<Employee> employees = new LinkedList<>();

        while (resultSet.next()) {
            employees.add(employeeFromResultSet(resultSet));
        }

        return new Response<>(employees, new LinkedList<>());
    }

    private Employee employeeFromResultSet(ResultSet resultSet) throws SQLException {
        return new Employee(
                resultSet.getString("id_employee"),
                resultSet.getString("empl_surname"),
                resultSet.getString("empl_name"),
                resultSet.getString("empl_patronymic"),
                resultSet.getString("empl_role"),
                resultSet.getDouble("salary"),
                resultSet.getDate("date_of_birth").toLocalDate(),
                resultSet.getDate("date_of_start").toLocalDate(),
                resultSet.getString("phone_number"),
                resultSet.getString("city"),
                resultSet.getString("street"),
                resultSet.getString("zip_code")
        );
    }

    private EmployeeInfo employeeInfoFromResultSet(ResultSet resultSet) throws SQLException {
        return new EmployeeInfo(
                resultSet.getString("id_employee"),
                resultSet.getString("empl_surname"),
                resultSet.getString("empl_name"),
                resultSet.getString("phone_number"),
                resultSet.getString("city"),
                resultSet.getString("street"),
                resultSet.getString("zip_code")
        );
    }

    public Response<Employee> createEmployee(Employee employee) {
        List<String> errors = validateEmployee(employee);
        if (!errors.isEmpty()) {
            return new Response<>(null, errors);
        }

        String query = "INSERT INTO employee (employee.id_employee, empl_surname, empl_name, empl_patronymic, empl_role, salary, date_of_birth, date_of_start, phone_number, city, street, zip_code) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try(PreparedStatement statement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, employee.getIdEmployee());
            statement.setString(2, employee.getEmplSurname());
            statement.setString(3, employee.getEmplName());
            statement.setString(4, employee.getEmplPatronymic());
            statement.setString(5, employee.getEmplRole());
            statement.setDouble(6, employee.getSalary());
            statement.setDate(7, java.sql.Date.valueOf(employee.getDateOfBirth()));
            statement.setDate(8, java.sql.Date.valueOf(employee.getDateOfStart()));
            statement.setString(9, employee.getPhoneNumber());
            statement.setString(10, employee.getCity());
            statement.setString(11, employee.getStreet());
            statement.setString(12, employee.getZipCode());

            int rows = statement.executeUpdate();
            if (rows == 0) {
                return new Response<>(null, Collections.singletonList("Failed to save employee"));
            }
            return new Response<>(employee, new LinkedList<>());
        } catch (SQLException e) {
            return new Response<>(null, Collections.singletonList(e.getMessage()));
        }
    }

    public Response<Employee> updateEmployee(Employee employee) {
        List<String> errors = validateEmployee(employee);
        if (!errors.isEmpty()) {
            return new Response<>(null, errors);
        }

        if (findEmployeeById(employee.getIdEmployee()).getObject() == null) {
            return new Response<>(null, Collections.singletonList("Can't update nonexistent employee"));
        }

        String query = "UPDATE employee SET empl_surname = ?, empl_name = ?, empl_patronymic = ?, empl_role = ?, salary = ?, date_of_birth = ?, date_of_start = ?, phone_number = ?, city = ?, street = ?, zip_code = ? WHERE id_employee = ?";

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, employee.getEmplSurname());
            statement.setString(2, employee.getEmplName());
            statement.setString(3, employee.getEmplPatronymic());
            statement.setString(4, employee.getEmplRole());
            statement.setDouble(5, employee.getSalary());
            statement.setDate(6, java.sql.Date.valueOf(employee.getDateOfBirth()));
            statement.setDate(7, java.sql.Date.valueOf(employee.getDateOfStart()));
            statement.setString(8, employee.getPhoneNumber());
            statement.setString(9, employee.getCity());
            statement.setString(10, employee.getStreet());
            statement.setString(11, employee.getZipCode());
            statement.setString(12, employee.getIdEmployee());

            int rows = statement.executeUpdate();
            if (rows == 0) {
                return new Response<>(null, Collections.singletonList("Failed to update employee"));
            }

            return new Response<>(employee, new LinkedList<>());
        } catch (SQLException e) {
            return new Response<>(null, Collections.singletonList(e.getMessage()));
        }
    }

    public Response<Employee> deleteEmployee(String employeeId) {
        if (findEmployeeById(employeeId).getObject() == null) {
            return new Response<>(null, Collections.singletonList("Can't delete nonexistent employee"));
        }

        if (hasChecks(employeeId)) {
            return new Response<>(null, Collections.singletonList(
                    "Cannot delete employee because this employee has created checks"
            ));
        }

        String query = "DELETE FROM employee WHERE id_employee = ?";

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, employeeId);

            int rows = statement.executeUpdate();
            if (rows == 0) {
                return new Response<>(null, Collections.singletonList("Failed to delete employee"));
            }

            return new Response<>(null, new LinkedList<>());
        } catch (SQLException e) {
            return new Response<>(null, Collections.singletonList(e.getMessage()));
        }
    }

    public Response<List<Employee>> findAllEmployees() {
        String query = "SELECT * FROM employee ORDER BY empl_surname";

        try(PreparedStatement statement = connection.prepareStatement(query)) {
            return getListResponse(statement);
        } catch (SQLException e) {
            return new Response<>(null, Collections.singletonList(e.getMessage()));
        }
    }

    public Response<List<Employee>> findAllCashiers() {
        String query = "SELECT * FROM employee WHERE empl_role = ? ORDER BY empl_surname";

        try(PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, "касир");
            return getListResponse(statement);
        } catch (SQLException e) {
            return new Response<>(null, Collections.singletonList(e.getMessage()));
        }
    }

    public Response<List<EmployeeInfo>> findPhoneAndAddressBySurname(String employeeSurname) {
        String query = "SELECT id_employee, empl_surname, empl_name, phone_number, city, street, zip_code FROM employee WHERE empl_surname = ?";

        try(PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, employeeSurname);
            ResultSet resultSet = statement.executeQuery();
            List<EmployeeInfo> employeesInfo = new LinkedList<>();

            while (resultSet.next()) {
                employeesInfo.add(employeeInfoFromResultSet(resultSet));
            }

            return new Response<>(employeesInfo, new LinkedList<>());
        } catch (SQLException e) {
            return new Response<>(null, Collections.singletonList(e.getMessage()));
        }
    }

    public Response<Employee> findEmployeeById(String employeeId) {
        String query = "SELECT * FROM employee WHERE id_employee = ?";

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, employeeId);
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                return new Response<>(employeeFromResultSet(resultSet), new LinkedList<>());
            } else {
                return new Response<>(null, Collections.singletonList("Employee not found"));
            }
        } catch (SQLException e) {
            return new Response<>(null, Collections.singletonList(e.getMessage()));
        }
    }

    public Response<Employee> findEmployeeByUsername(String username) {
        String query = """
        SELECT e.* FROM employee e
        JOIN user_account u ON e.id_employee = u.id_employee
        WHERE u.username = ?
    """;
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return new Response<>(employeeFromResultSet(resultSet), new java.util.LinkedList<>());
            }
            return new Response<>(null, java.util.Collections.singletonList("Employee not found"));
        } catch (SQLException e) {
            return new Response<>(null, java.util.Collections.singletonList(e.getMessage()));
        }
    }

    private boolean hasChecks(String employeeId) {
        String query = "SELECT 1 FROM my_check WHERE id_employee = ? LIMIT 1";

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, employeeId);
            ResultSet rs = statement.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            return true;
        }
    }
}