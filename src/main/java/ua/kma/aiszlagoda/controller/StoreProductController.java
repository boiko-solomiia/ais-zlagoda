package ua.kma.aiszlagoda.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import ua.kma.aiszlagoda.persistence.model.*;
import ua.kma.aiszlagoda.persistence.service.ProductService;
import ua.kma.aiszlagoda.persistence.service.StoreProductService;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.access.prepost.PreAuthorize;

import java.sql.SQLException;
import java.util.List;

@Controller
@RequestMapping("/store-product")
public class StoreProductController {

    private final StoreProductService storeProductService;
    private final ProductService productService;

    public StoreProductController(StoreProductService storeProductService, ProductService productService) {
        this.storeProductService = storeProductService;
        this.productService = productService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'CASHIER')")
    public String getAllStoreProducts(Model model) {
        Response<List<StoreProductInfo>> response = storeProductService.findAllStoreProductsSortedByName();

        if (!response.getErrors().isEmpty()) {
            model.addAttribute("errors", response.getErrors());
            return "error-page";
        }

        model.addAttribute("storeProducts", response.getObject());
        return "store-product-list";
    }

    @GetMapping("/add")
    @PreAuthorize("hasRole('MANAGER')")
    public String showAddForm(Model model) {
        Response<List<Product>> productsResponse = productService.findAll();

        model.addAttribute("storeProduct", new StoreProduct());
        model.addAttribute("products", productsResponse.getObject());

        return "store-product-add";
    }

    @PostMapping("/add")
    @PreAuthorize("hasRole('MANAGER')")
    public String addStoreProduct(@ModelAttribute StoreProduct storeProduct, Model model) throws SQLException {
        Response<StoreProduct> response = storeProductService.createStoreProduct(storeProduct);

        if (!response.getErrors().isEmpty()) {
            Response<List<Product>> productsResponse = productService.findAll();
            model.addAttribute("errors", response.getErrors());
            model.addAttribute("storeProduct", storeProduct);
            model.addAttribute("products", productsResponse.getObject());
            return "store-product-add";
        }

        return "redirect:/store-product";
    }

    @GetMapping("/edit/{upc}")
    @PreAuthorize("hasRole('MANAGER')")
    public String showEditForm(@PathVariable String upc, Model model) {
        Response<StoreProduct> response = storeProductService.findStoreProductByUPC(upc);
        Response<List<Product>> productsResponse = productService.findAll();

        if (!response.getErrors().isEmpty() || response.getObject() == null) {
            model.addAttribute("errors", response.getErrors());
            return "error-page";
        }

        model.addAttribute("storeProduct", response.getObject());
        model.addAttribute("products", productsResponse.getObject());
        return "store-product-edit";
    }

    @PostMapping("/edit")
    @PreAuthorize("hasRole('MANAGER')")
    public String editStoreProduct(@ModelAttribute StoreProduct storeProduct, Model model) {
        Response<StoreProduct> response = storeProductService.updateStoreProduct(storeProduct);

        if (!response.getErrors().isEmpty()) {
            Response<List<Product>> productsResponse = productService.findAll();
            model.addAttribute("errors", response.getErrors());
            model.addAttribute("storeProduct", storeProduct);
            model.addAttribute("products", productsResponse.getObject());
            return "store-product-edit";
        }

        return "redirect:/store-product";
    }

    @PostMapping("/delete/{upc}")
    @PreAuthorize("hasRole('MANAGER')")
    public String deleteStoreProduct(@PathVariable String upc, RedirectAttributes redirectAttributes) {
        Response<StoreProduct> response = storeProductService.deleteStoreProduct(upc);

        if (!response.getErrors().isEmpty()) {
            redirectAttributes.addFlashAttribute("errors", response.getErrors());
            return "redirect:/store-product";
        }

        redirectAttributes.addFlashAttribute("successMessage", "Store product deleted successfully");
        return "redirect:/store-product";
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('MANAGER', 'CASHIER')")
    public String findByUpc(@RequestParam String upc, Model model) {
        Response<StoreProductInfo> response = storeProductService.findStoreProductInfoByUPC(upc);

        if (!response.getErrors().isEmpty()) {
            model.addAttribute("errors", response.getErrors());
            return "error-page";
        }

        model.addAttribute("storeProductInfo", response.getObject());
        model.addAttribute("searchUpc", upc);
        return "store-product-info";
    }

    @GetMapping("/promotional")
    @PreAuthorize("hasAnyRole('MANAGER', 'CASHIER')")
    public String getPromotionalProducts(Model model) {
        Response<List<StoreProductInfo>> response = storeProductService.findPromotionalProductsSortedByName();

        if (!response.getErrors().isEmpty()) {
            model.addAttribute("errors", response.getErrors());
            return "error-page";
        }

        model.addAttribute("storeProducts", response.getObject());
        return "store-product-list";
    }

    @GetMapping("/non-promotional")
    @PreAuthorize("hasAnyRole('MANAGER', 'CASHIER')")
    public String getNonPromotionalProducts(Model model) {
        Response<List<StoreProductInfo>> response = storeProductService.findNonPromotionalProductsSortedByName();

        if (!response.getErrors().isEmpty()) {
            model.addAttribute("errors", response.getErrors());
            return "error-page";
        }

        model.addAttribute("storeProducts", response.getObject());
        return "store-product-list";
    }

    @GetMapping("/sort")
    @PreAuthorize("hasAnyRole('MANAGER', 'CASHIER')")
    public String getStoreProductsSorted(
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(required = false) String filter,
            Model model) {

        Response<List<StoreProductInfo>> response;
        String filterType;

        if ("promotional".equals(filter)) {
            if ("quantity".equals(sortBy)) {
                response = storeProductService.findPromotionalProductsSortedByQuantity();
            } else {
                response = storeProductService.findPromotionalProductsSortedByName();
            }
            filterType = "promotional";
        } else if ("non-promotional".equals(filter)) {
            if ("quantity".equals(sortBy)) {
                response = storeProductService.findNonPromotionalProductsSortedByQuantity();
            } else {
                response = storeProductService.findNonPromotionalProductsSortedByName();
            }
            filterType = "non-promotional";
        } else {
            if ("quantity".equals(sortBy)) {
                response = storeProductService.findAllStoreProductsSortedByQuantity();
            } else {
                response = storeProductService.findAllStoreProductsSortedByName();
            }
            filterType = null;
        }

        if (!response.getErrors().isEmpty()) {
            model.addAttribute("errors", response.getErrors());
            return "error-page";
        }

        model.addAttribute("storeProducts", response.getObject());
        model.addAttribute("currentSort", sortBy);
        model.addAttribute("filterType", filterType);

        return "store-product-list";
    }

    @GetMapping("/print")
    @PreAuthorize("hasRole('MANAGER')")
    public String printStoreProducts(Model model) {
        Response<List<StoreProductPrintDTO>> response = storeProductService.findAllForPrint();
        if (!response.getErrors().isEmpty()) {
            model.addAttribute("errors", response.getErrors());
            return "error-page";
        }
        model.addAttribute("storeProducts", response.getObject());
        return "store-product-print";
    }
}