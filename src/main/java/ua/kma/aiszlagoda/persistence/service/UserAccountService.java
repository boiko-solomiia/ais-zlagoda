package ua.kma.aiszlagoda.persistence.service;

import org.springframework.stereotype.Service;
import ua.kma.aiszlagoda.persistence.model.AuthUser;
import ua.kma.aiszlagoda.persistence.model.Response;
import ua.kma.aiszlagoda.persistence.model.UserAccount;

import java.sql.*;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

@Service
public class UserAccountService {

    private final Connection connection;
    private final PasswordService passwordService;

    public UserAccountService(Connection connection, PasswordService passwordService) {
        this.connection = connection;
        this.passwordService = passwordService;
    }

    private List<String> validateUserAccount(UserAccount userAccount) {
        List<String> errors = new LinkedList<>();

        if (userAccount.getIdEmployee() == null || userAccount.getIdEmployee().isBlank()) {
            errors.add("Employee id can't be empty");
        }

        if (userAccount.getUsername() == null || userAccount.getUsername().isBlank()) {
            errors.add("Username can't be empty");
        }

        if (userAccount.getPasswordHash() == null || userAccount.getPasswordHash().isBlank()) {
            errors.add("Password can't be empty");
        }

        return errors;
    }

    private UserAccount userAccountFromResultSet(ResultSet resultSet) throws SQLException {
        return new UserAccount(
                resultSet.getInt("id_account"),
                resultSet.getString("id_employee"),
                resultSet.getString("username"),
                resultSet.getString("password_hash")
        );
    }

    public Response<UserAccount> findByUsername(String username) {
        String query = "SELECT * FROM user_account WHERE username = ?";

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                return new Response<>(userAccountFromResultSet(resultSet), new LinkedList<>());
            } else {
                return new Response<>(null, Collections.singletonList("User account not found"));
            }
        } catch (SQLException e) {
            return new Response<>(null, Collections.singletonList(e.getMessage()));
        }
    }

    public Response<UserAccount> findByEmployeeId(String employeeId) {
        String query = "SELECT * FROM user_account WHERE id_employee = ?";

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, employeeId);
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                return new Response<>(userAccountFromResultSet(resultSet), new LinkedList<>());
            } else {
                return new Response<>(null, Collections.singletonList("User account not found"));
            }
        } catch (SQLException e) {
            return new Response<>(null, Collections.singletonList(e.getMessage()));
        }
    }

    public Response<UserAccount> createUserAccount(UserAccount userAccount) {
        List<String> errors = validateUserAccount(userAccount);
        if (!errors.isEmpty()) {
            return new Response<>(null, errors);
        }

        if (findByEmployeeId(userAccount.getIdEmployee()).getObject() != null) {
            return new Response<>(null, Collections.singletonList("This employee already has an account"));
        }

        if (findByUsername(userAccount.getUsername()).getObject() != null) {
            return new Response<>(null, Collections.singletonList("This username is already taken"));
        }

        String query = """
                INSERT INTO user_account (id_employee, username, password_hash)
                VALUES (?, ?, ?)
                """;

        try (PreparedStatement statement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, userAccount.getIdEmployee());
            statement.setString(2, userAccount.getUsername());

            String encodedPassword = passwordService.encodePassword(userAccount.getPasswordHash());
            statement.setString(3, encodedPassword);

            int rows = statement.executeUpdate();
            if (rows == 0) {
                return new Response<>(null, Collections.singletonList("Failed to create user account"));
            }

            ResultSet generatedKeys = statement.getGeneratedKeys();
            if (generatedKeys.next()) {
                userAccount.setIdAccount(generatedKeys.getInt(1));
            }

            userAccount.setPasswordHash(encodedPassword);
            return new Response<>(userAccount, new LinkedList<>());
        } catch (SQLException e) {
            return new Response<>(null, Collections.singletonList(e.getMessage()));
        }
    }

    public Response<AuthUser> findAuthUserByUsername(String username) {
        String query = """
            SELECT ua.username,
                   ua.password_hash,
                   ua.id_employee,
                   e.empl_role
            FROM user_account ua
            JOIN employee e ON ua.id_employee = e.id_employee
            WHERE ua.username = ?
            """;

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                AuthUser authUser = new AuthUser(
                        resultSet.getString("username"),
                        resultSet.getString("password_hash"),
                        resultSet.getString("id_employee"),
                        resultSet.getString("empl_role")
                );
                return new Response<>(authUser, new LinkedList<>());
            } else {
                return new Response<>(null, Collections.singletonList("User account not found"));
            }
        } catch (SQLException e) {
            return new Response<>(null, Collections.singletonList(e.getMessage()));
        }
    }
}