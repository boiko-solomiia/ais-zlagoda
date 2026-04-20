package ua.kma.aiszlagoda.controller;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import ua.kma.aiszlagoda.persistence.model.Category;
import ua.kma.aiszlagoda.persistence.model.Product;
import ua.kma.aiszlagoda.persistence.model.ProductSaleDTO;
import ua.kma.aiszlagoda.persistence.model.Response;
import ua.kma.aiszlagoda.persistence.service.CategoryService;
import ua.kma.aiszlagoda.persistence.service.ProductService;

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
    public String showAddForm(Model model) {
        Response<List<Category>> categoriesResponse = categoryService.findAll();
        model.addAttribute("product", new Product());
        model.addAttribute("categories", categoriesResponse.getObject());
        return "product-add";
    }

    @PostMapping("/add")
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
    public String deleteProduct(@PathVariable int id, Model model) {
        Response<Product> response = productService.deleteProduct(id);

        if (!response.getErrors().isEmpty()) {
            model.addAttribute("errors", response.getErrors());
            return "error-page";
        }

        return "redirect:/product";
    }

    @GetMapping("/search")
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

}