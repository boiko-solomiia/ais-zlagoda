package ua.kma.aiszlagoda.persistence.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ua.kma.aiszlagoda.persistence.model.Category;
import ua.kma.aiszlagoda.persistence.model.Response;

import java.sql.*;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

@Service
public class CategoryService {

    private final Connection connection;

    @Autowired
    public CategoryService(Connection connection) {
        this.connection = connection;
    }

    private List<String> validateCategory(Category category) {
        List<String> errors = new LinkedList<>();
        if (category.getCategoryName() == null || category.getCategoryName().isBlank()) {
            errors.add("Category name can't be empty");
        }
        return errors;
    }

    private Category categoryFromResultSet(ResultSet resultSet) throws SQLException {
        return new Category(
                resultSet.getInt("category_number"),
                resultSet.getString("category_name")
        );
    }

    public Response<Category> createCategory(Category category) {
        List<String> errors = validateCategory(category);
        if (!errors.isEmpty()) {
            return new Response<>(null, errors);
        }

        String query = "INSERT INTO category (category_name) VALUES (?)";
        try (PreparedStatement statement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, category.getCategoryName());

            int rows = statement.executeUpdate();
            if (rows == 0) {
                return new Response<>(null, Collections.singletonList("Failed to save category"));
            }

            ResultSet generatedKeys = statement.getGeneratedKeys();
            if (generatedKeys.next()) {
                category.setCategoryNumber(generatedKeys.getInt(1));
            }

            return new Response<>(category, new LinkedList<>());
        } catch (SQLException e) {
            return new Response<>(null, Collections.singletonList(e.getMessage()));
        }
    }

    public Response<Category> updateCategory(Category category) {
        List<String> errors = validateCategory(category);
        if (!errors.isEmpty()) {
            return new Response<>(null, errors);
        }
        if (findCategoryById(category.getCategoryNumber()).getObject() == null) {
            return new Response<>(null, Collections.singletonList("Can't update nonexistent category"));
        }
        String query = "UPDATE category SET category_name = ? WHERE category_number = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, category.getCategoryName());
            statement.setInt(2, category.getCategoryNumber());
            int rows = statement.executeUpdate();
            if (rows == 0) {
                return new Response<>(null, Collections.singletonList("Failed to update category"));
            }
            return new Response<>(category, new LinkedList<>());
        } catch (SQLException e) {
            return new Response<>(null, Collections.singletonList(e.getMessage()));
        }
    }

    public Response<Category> deleteCategory(int categoryNumber) {
        if (findCategoryById(categoryNumber).getObject() == null) {
            return new Response<>(null, Collections.singletonList("Can't delete nonexistent category"));
        }
        String query = "DELETE FROM category WHERE category_number = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, categoryNumber);
            int rows = statement.executeUpdate();
            if (rows == 0) {
                return new Response<>(null, Collections.singletonList("Failed to delete category"));
            }
            return new Response<>(null, new LinkedList<>());
        } catch (SQLException e) {
            return new Response<>(null, Collections.singletonList(e.getMessage()));
        }
    }

    public Response<List<Category>> findAll() {
        String query = "SELECT * FROM category ORDER BY category_name";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            ResultSet resultSet = statement.executeQuery();
            List<Category> categories = new LinkedList<>();
            while (resultSet.next()) {
                categories.add(categoryFromResultSet(resultSet));
            }
            return new Response<>(categories, new LinkedList<>());
        } catch (SQLException e) {
            return new Response<>(null, Collections.singletonList(e.getMessage()));
        }
    }

    public Response<Category> findCategoryById(int categoryNumber) {
        String query = "SELECT * FROM category WHERE category_number = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, categoryNumber);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return new Response<>(categoryFromResultSet(resultSet), new LinkedList<>());
            } else {
                return new Response<>(null, Collections.singletonList("Category not found"));
            }
        } catch (SQLException e) {
            return new Response<>(null, Collections.singletonList(e.getMessage()));
        }
    }
}