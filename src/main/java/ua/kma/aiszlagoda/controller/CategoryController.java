package ua.kma.aiszlagoda.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import ua.kma.aiszlagoda.persistence.model.Category;
import ua.kma.aiszlagoda.persistence.model.Employee;
import ua.kma.aiszlagoda.persistence.model.Response;
import ua.kma.aiszlagoda.persistence.service.CategoryService;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/category")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping("/")
    public String home() {
        return "redirect:/category";
    }

    @GetMapping
    public String getAllCategories(Model model) {
        Response<List<Category>> response = categoryService.findAll();

        if (!response.getErrors().isEmpty()) {
            model.addAttribute("errors", response.getErrors());
            return "error-page";
        }

        model.addAttribute("categories", response.getObject());
        return "category-list";
    }

    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("category", new Category());
        return "category-add";
    }



    @PostMapping("/add")
    public String addCategory(@ModelAttribute Category category, Model model) {
        Response<Category> response = categoryService.createCategory(category);

        if (!response.getErrors().isEmpty()) {
            model.addAttribute("errors", response.getErrors());
            model.addAttribute("category", category);
            return "category-add";
        }

        return "redirect:/category";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable int id, Model model) {
        Response<Category> response = categoryService.findCategoryById(id);

        if (!response.getErrors().isEmpty() || response.getObject() == null) {
            model.addAttribute("errors", response.getErrors());
            return "error-page";
        }

        model.addAttribute("category", response.getObject());
        return "category-edit";
    }

    @PostMapping("/edit")
    public String editCategory(@ModelAttribute Category category, Model model) {
        Response<Category> response = categoryService.updateCategory(category);

        if (!response.getErrors().isEmpty()) {
            model.addAttribute("errors", response.getErrors());
            model.addAttribute("category", category);
            return "category-edit";
        }

        return "redirect:/category";
    }

    @PostMapping("/delete/{id}")
    public String deleteCategory(@PathVariable int id, RedirectAttributes redirectAttributes) {
        Response<Category> response = categoryService.deleteCategory(id);

        if (!response.getErrors().isEmpty()) {
            redirectAttributes.addFlashAttribute("errors", response.getErrors());
            return "redirect:/category";
        }

        redirectAttributes.addFlashAttribute("successMessage", "Category deleted successfully");
        return "redirect:/category";
    }

    @GetMapping("/search")
    public String searchProductsByName(@RequestParam String name, Model model) {
        Response<List<Category>> response = categoryService.findCategoriesByName(name);

        if (!response.getErrors().isEmpty()) {
            model.addAttribute("errors", response.getErrors());
            return "error-page";
        }

        model.addAttribute("categories", response.getObject());
        model.addAttribute("searchName", name);
        return "category-list";
    }

    @GetMapping("/print")
    public String printCategories(Model model) {
        Response<List<Category>> response = categoryService.findAll();
        if (!response.getErrors().isEmpty()) {
            model.addAttribute("errors", response.getErrors());
            return "error-page";
        }
        model.addAttribute("categories", response.getObject());
        return "category-print";
    }
}