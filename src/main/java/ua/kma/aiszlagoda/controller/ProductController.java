package ua.kma.aiszlagoda.controller;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import ua.kma.aiszlagoda.persistence.model.*;
import ua.kma.aiszlagoda.persistence.service.CategoryService;
import ua.kma.aiszlagoda.persistence.service.ProductService;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.access.prepost.PreAuthorize;

import java.time.LocalDateTime;
import java.util.List;


@Controller
@RequestMapping("/product")
public class ProductController {

    private final ProductService productService;
    private final CategoryService categoryService;

    public ProductController(ProductService productService, CategoryService categoryService) {
        this.productService = productService;
        this.categoryService = categoryService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'CASHIER')")
    public String getAllProducts(Model model) {
        Response<List<Product>> response = productService.findAll();

        if (!response.getErrors().isEmpty()) {
            model.addAttribute("errors", response.getErrors());
            return "error-page";
        }

        Response<List<Category>> categoriesResponse = categoryService.findAll();
        model.addAttribute("products", response.getObject());
        model.addAttribute("categories", categoriesResponse.getObject());
        return "product-list";
    }

    @GetMapping("/add")
    @PreAuthorize("hasRole('MANAGER')")
    public String showAddForm(Model model) {
        Response<List<Category>> categoriesResponse = categoryService.findAll();
        model.addAttribute("product", new Product());
        model.addAttribute("categories", categoriesResponse.getObject());
        return "product-add";
    }

    @PostMapping("/add")
    @PreAuthorize("hasRole('MANAGER')")
    public String addProduct(@ModelAttribute Product product, Model model) {
        Response<Product> response = productService.createProduct(product);

        if (!response.getErrors().isEmpty()) {
            Response<List<Category>> categoriesResponse = categoryService.findAll();
            model.addAttribute("errors", response.getErrors());
            model.addAttribute("product", product);
            model.addAttribute("categories", categoriesResponse.getObject());
            return "product-add";
        }

        return "redirect:/product";
    }

    @GetMapping("/edit/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public String showEditForm(@PathVariable int id, Model model) {
        Response<Product> response = productService.findProductById(id);
        Response<List<Category>> categoriesResponse = categoryService.findAll();

        if (!response.getErrors().isEmpty() || response.getObject() == null) {
            model.addAttribute("errors", response.getErrors());
            return "error-page";
        }

        model.addAttribute("product", response.getObject());
        model.addAttribute("categories", categoriesResponse.getObject());
        return "product-edit";
    }

    @PostMapping("/edit")
    @PreAuthorize("hasRole('MANAGER')")
    public String editProduct(@ModelAttribute Product product, Model model) {
        Response<Product> response = productService.updateProduct(product);

        if (!response.getErrors().isEmpty()) {
            Response<List<Category>> categoriesResponse = categoryService.findAll();
            model.addAttribute("errors", response.getErrors());
            model.addAttribute("product", product);
            model.addAttribute("categories", categoriesResponse.getObject());
            return "product-edit";
        }

        return "redirect:/product";
    }

    @PostMapping("/delete/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public String deleteProduct(@PathVariable int id, RedirectAttributes redirectAttributes) {
        Response<Product> response = productService.deleteProduct(id);

        if (!response.getErrors().isEmpty()) {
            redirectAttributes.addFlashAttribute("errors", response.getErrors());
            return "redirect:/product";
        }

        redirectAttributes.addFlashAttribute("successMessage", "Product deleted successfully");
        return "redirect:/product";
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('MANAGER', 'CASHIER')")
    public String searchProductsByName(@RequestParam String name, Model model) {
        Response<List<Product>> response = productService.findProductsByName(name);

        if (!response.getErrors().isEmpty()) {
            model.addAttribute("errors", response.getErrors());
            return "error-page";
        }

        Response<List<Category>> categoriesResponse = categoryService.findAll();
        model.addAttribute("products", response.getObject());
        model.addAttribute("categories", categoriesResponse.getObject());
        model.addAttribute("searchName", name);
        return "product-list";
    }

    @GetMapping("/filter")
    @PreAuthorize("hasAnyRole('MANAGER', 'CASHIER')")
    public String getProductsByCategory(@RequestParam(required = false) Integer categoryNumber, Model model) {
        Response<List<Product>> response = categoryNumber != null
                ? productService.findProductsByCategory(categoryNumber)
                : productService.findAll();

        if (!response.getErrors().isEmpty()) {
            model.addAttribute("errors", response.getErrors());
            return "error-page";
        }

        Response<List<Category>> categoriesResponse = categoryService.findAll();
        model.addAttribute("products", response.getObject());
        model.addAttribute("categories", categoriesResponse.getObject());
        model.addAttribute("selectedCategory", categoryNumber);
        return "product-list";
    }

    @GetMapping("/print")
    @PreAuthorize("hasRole('MANAGER')")
    public String printProducts(Model model) {
        Response<List<Product>> response = productService.findAll();
        if (!response.getErrors().isEmpty()) {
            model.addAttribute("errors", response.getErrors());
            return "error-page";
        }
        model.addAttribute("products", response.getObject());
        return "product-print";
    }


    @GetMapping("/sold-quantity")
    @PreAuthorize("hasRole('MANAGER')")
    public String showProductSalesPage(@RequestParam Integer productId, Model model) {
        Response<Product> productResponse = productService.findProductById(productId);
        if (!productResponse.getErrors().isEmpty()) {
            model.addAttribute("errors", productResponse.getErrors());
            return "error-page";
        }

        Response<List<ProductSaleDTO>> salesResponse = productService.findAllSalesByProduct(productId);
        if (!salesResponse.getErrors().isEmpty()) {
            model.addAttribute("errors", salesResponse.getErrors());
            return "error-page";
        }

        int soldQuantity = 0;
        for (ProductSaleDTO sale : salesResponse.getObject()) {
            soldQuantity += sale.getProductNumber();
        }

        model.addAttribute("product", productResponse.getObject());
        model.addAttribute("sales", salesResponse.getObject());
        model.addAttribute("soldQuantity", soldQuantity);

        return "product-sold-quantity";
    }

    @PostMapping("/sold-quantity")
    @PreAuthorize("hasRole('MANAGER')")
    public String filterProductSalesByPeriod(
            @RequestParam Integer productId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime start,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime end,
            Model model) {

        Response<Product> productResponse = productService.findProductById(productId);
        if (!productResponse.getErrors().isEmpty()) {
            model.addAttribute("errors", productResponse.getErrors());
            return "error-page";
        }

        Response<List<ProductSaleDTO>> salesResponse =
                productService.findSalesByProductAndPeriod(productId, start, end);

        if (!salesResponse.getErrors().isEmpty()) {
            model.addAttribute("errors", salesResponse.getErrors());
            return "error-page";
        }

        Response<Integer> quantityResponse =
                productService.getSoldQuantityByProductAndPeriod(productId, start, end);

        if (!quantityResponse.getErrors().isEmpty()) {
            model.addAttribute("errors", quantityResponse.getErrors());
            return "error-page";
        }

        model.addAttribute("product", productResponse.getObject());
        model.addAttribute("sales", salesResponse.getObject());
        model.addAttribute("soldQuantity", quantityResponse.getObject());
        model.addAttribute("start", start);
        model.addAttribute("end", end);

        return "product-sold-quantity";
    }

    @GetMapping("/customer-stats")
    @PreAuthorize("hasRole('MANAGER')")
    public String showProductsCustomerStatsPage(Model model) {
        Response<List<ProductCustomerStatsDTO>> response = productService.findProductsBoughtByMostDifferentCustomers();

        if (!response.getErrors().isEmpty()) {
            model.addAttribute("errors", response.getErrors());
            return "product-customer-stats";
        }

        model.addAttribute("productsStats", response.getObject());
        return "product-customer-stats";
    }
}