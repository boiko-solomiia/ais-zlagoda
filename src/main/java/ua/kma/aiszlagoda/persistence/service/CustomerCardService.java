package ua.kma.aiszlagoda.persistence.service;

import org.springframework.stereotype.Service;
import ua.kma.aiszlagoda.persistence.model.CustomerCard;
import ua.kma.aiszlagoda.persistence.model.Response;

import java.sql.*;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

@Service
public class CustomerCardService {

    private final Connection connection;

    public CustomerCardService(Connection connection) {
        this.connection = connection;
    }

    private List<String> validateCustomerCard(CustomerCard customerCard) {
        List<String> errors = new LinkedList<>();

        if (customerCard.getCardNumber() == null || customerCard.getCardNumber().isBlank()) {
            errors.add("Card number can't be empty");
        }

        if (customerCard.getCustSurname() == null || customerCard.getCustSurname().isBlank()) {
            errors.add("Customer surname can't be empty");
        }

        if (customerCard.getCustName() == null || customerCard.getCustName().isBlank()) {
            errors.add("Customer name can't be empty");
        }

        if (customerCard.getPhoneNumber() == null || customerCard.getPhoneNumber().isBlank()) {
            errors.add("Phone number can't be empty");
        } else if (customerCard.getPhoneNumber().length() > 13) {
            errors.add("Phone number can't exceed 13 characters including '+'");
        }

        if (customerCard.getPercent() == null) {
            errors.add("Percent can't be empty");
        } else if (customerCard.getPercent() < 0 || customerCard.getPercent() > 100) {
            errors.add("Percent must be between 0 and 100");
        }

        return errors;
    }

    private CustomerCard customerCardFromResultSet(ResultSet resultSet) throws SQLException {
        return new CustomerCard(
                resultSet.getString("card_number"),
                resultSet.getString("cust_surname"),
                resultSet.getString("cust_name"),
                resultSet.getString("cust_patronymic"),
                resultSet.getString("phone_number"),
                resultSet.getString("city"),
                resultSet.getString("street"),
                resultSet.getString("zip_code"),
                resultSet.getInt("percent")
        );
    }

    public Response<CustomerCard> createCustomerCard(CustomerCard customerCard) {
        List<String> errors = validateCustomerCard(customerCard);
        if (!errors.isEmpty()) {
            return new Response<>(null, errors);
        }

        String query = """
                INSERT INTO customer_card
                (card_number, cust_surname, cust_name, cust_patronymic, phone_number, city, street, zip_code, percent)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, customerCard.getCardNumber());
            statement.setString(2, customerCard.getCustSurname());
            statement.setString(3, customerCard.getCustName());
            statement.setString(4, customerCard.getCustPatronymic());
            statement.setString(5, customerCard.getPhoneNumber());
            statement.setString(6, customerCard.getCity());
            statement.setString(7, customerCard.getStreet());
            statement.setString(8, customerCard.getZipCode());
            statement.setInt(9, customerCard.getPercent());

            int rows = statement.executeUpdate();
            if (rows == 0) {
                return new Response<>(null, Collections.singletonList("Failed to save customer card"));
            }

            return new Response<>(customerCard, new LinkedList<>());
        } catch (SQLException e) {
            return new Response<>(null, Collections.singletonList(e.getMessage()));
        }
    }

    public Response<List<CustomerCard>> findAll() {
        String query = "SELECT * FROM customer_card ORDER BY cust_surname ASC";

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            ResultSet resultSet = statement.executeQuery();
            List<CustomerCard> customerCards = new LinkedList<>();

            while (resultSet.next()) {
                customerCards.add(customerCardFromResultSet(resultSet));
            }

            return new Response<>(customerCards, new LinkedList<>());
        } catch (SQLException e) {
            return new Response<>(null, Collections.singletonList(e.getMessage()));
        }
    }

    public Response<CustomerCard> findCustomerCardByNumber(String cardNumber) {
        String query = "SELECT * FROM customer_card WHERE card_number = ?";

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, cardNumber);
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                return new Response<>(customerCardFromResultSet(resultSet), new LinkedList<>());
            } else {
                return new Response<>(null, Collections.singletonList("Customer card not found"));
            }
        } catch (SQLException e) {
            return new Response<>(null, Collections.singletonList(e.getMessage()));
        }
    }

    public Response<CustomerCard> updateCustomerCard(CustomerCard customerCard) {
        List<String> errors = validateCustomerCard(customerCard);
        if (!errors.isEmpty()) {
            return new Response<>(null, errors);
        }

        if (findCustomerCardByNumber(customerCard.getCardNumber()).getObject() == null) {
            return new Response<>(null, Collections.singletonList("Can't update nonexistent customer card"));
        }

        String query = """
                UPDATE customer_card
                SET cust_surname = ?, cust_name = ?, cust_patronymic = ?, phone_number = ?,
                    city = ?, street = ?, zip_code = ?, percent = ?
                WHERE card_number = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, customerCard.getCustSurname());
            statement.setString(2, customerCard.getCustName());
            statement.setString(3, customerCard.getCustPatronymic());
            statement.setString(4, customerCard.getPhoneNumber());
            statement.setString(5, customerCard.getCity());
            statement.setString(6, customerCard.getStreet());
            statement.setString(7, customerCard.getZipCode());
            statement.setInt(8, customerCard.getPercent());
            statement.setString(9, customerCard.getCardNumber());

            int rows = statement.executeUpdate();
            if (rows == 0) {
                return new Response<>(null, Collections.singletonList("Failed to update customer card"));
            }

            return new Response<>(customerCard, new LinkedList<>());
        } catch (SQLException e) {
            return new Response<>(null, Collections.singletonList(e.getMessage()));
        }
    }

    public Response<CustomerCard> deleteCustomerCard(String cardNumber) {
        if (findCustomerCardByNumber(cardNumber).getObject() == null) {
            return new Response<>(null, Collections.singletonList("Can't delete nonexistent customer card"));
        }

        String query = "DELETE FROM customer_card WHERE card_number = ?";

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, cardNumber);

            int rows = statement.executeUpdate();
            if (rows == 0) {
                return new Response<>(null, Collections.singletonList("Failed to delete customer card"));
            }

            return new Response<>(null, new LinkedList<>());
        } catch (SQLException e) {
            return new Response<>(null, Collections.singletonList(e.getMessage()));
        }
    }

    public Response<List<CustomerCard>> findCustomerCardsByPercent(int percent) {
        String query = "SELECT * FROM customer_card WHERE percent = ? ORDER BY cust_surname ASC";

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, percent);
            ResultSet resultSet = statement.executeQuery();
            List<CustomerCard> customerCards = new LinkedList<>();

            while (resultSet.next()) {
                customerCards.add(customerCardFromResultSet(resultSet));
            }

            return new Response<>(customerCards, new LinkedList<>());
        } catch (SQLException e) {
            return new Response<>(null, Collections.singletonList(e.getMessage()));
        }
    }

    public Response<List<CustomerCard>> findCustomerCardsBySurname(String surname) {
        String query = "SELECT * FROM customer_card WHERE cust_surname LIKE ? ORDER BY cust_surname ASC";

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, "%" + surname + "%");
            ResultSet resultSet = statement.executeQuery();
            List<CustomerCard> customerCards = new LinkedList<>();

            while (resultSet.next()) {
                customerCards.add(customerCardFromResultSet(resultSet));
            }

            return new Response<>(customerCards, new LinkedList<>());
        } catch (SQLException e) {
            return new Response<>(null, Collections.singletonList(e.getMessage()));
        }
    }
}