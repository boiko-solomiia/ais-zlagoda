package ua.kma.aiszlagoda.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import ua.kma.aiszlagoda.persistence.model.Product;
import ua.kma.aiszlagoda.persistence.model.Response;
import ua.kma.aiszlagoda.persistence.model.StoreProduct;
import ua.kma.aiszlagoda.persistence.model.StoreProductInfo;
import ua.kma.aiszlagoda.persistence.service.ProductService;
import ua.kma.aiszlagoda.persistence.service.StoreProductService;

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
    public String showAddForm(Model model) {
        Response<List<Product>> productsResponse = productService.findAll();

        model.addAttribute("storeProduct", new StoreProduct());
        model.addAttribute("products", productsResponse.getObject());

        return "store-product-add";
    }

    @PostMapping("/add")
    public String addStoreProduct(@ModelAttribute StoreProduct storeProduct, Model model) {
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
    public String deleteStoreProduct(@PathVariable String upc, Model model) {
        Response<StoreProduct> response = storeProductService.deleteStoreProduct(upc);

        if (!response.getErrors().isEmpty()) {
            model.addAttribute("errors", response.getErrors());
            return "error-page";
        }

        return "redirect:/store-product";
    }

    @GetMapping("/search")
    public String findByUpc(@RequestParam String upc, Model model) {
        Response<StoreProductInfo> response = storeProductService.findStoreProductInfoByUPC(upc);

        if (!response.getErrors().isEmpty()) {
            model.addAttribute("errors", response.getErrors());
            return "error-page";
        }

        model.addAttribute("storeProductInfo", response.getObject());
        return "store-product-info";
    }

    @GetMapping("/promotional")
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
    public String getNonPromotionalProducts(Model model) {
        Response<List<StoreProductInfo>> response = storeProductService.findNonPromotionalProductsSortedByName();

        if (!response.getErrors().isEmpty()) {
            model.addAttribute("errors", response.getErrors());
            return "error-page";
        }

        model.addAttribute("storeProducts", response.getObject());
        return "store-product-list";
    }
}