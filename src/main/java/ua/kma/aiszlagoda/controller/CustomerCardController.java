package ua.kma.aiszlagoda.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import ua.kma.aiszlagoda.persistence.model.CustomerCard;
import ua.kma.aiszlagoda.persistence.model.Response;
import ua.kma.aiszlagoda.persistence.service.CustomerCardService;

import java.util.List;

@Controller
@RequestMapping("/customer-card")
public class CustomerCardController {

    private final CustomerCardService customerCardService;

    public CustomerCardController(CustomerCardService customerCardService) {
        this.customerCardService = customerCardService;
    }

    @GetMapping
    public String getAllCustomerCards(Model model) {
        Response<List<CustomerCard>> response = customerCardService.findAll();

        if (!response.getErrors().isEmpty()) {
            model.addAttribute("errors", response.getErrors());
            return "error-page";
        }

        model.addAttribute("customerCards", response.getObject());
        return "customer-card-list";
    }

    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("customerCard", new CustomerCard());
        return "customer-card-add";
    }

    @PostMapping("/add")
    public String addCustomerCard(@ModelAttribute CustomerCard customerCard, Model model) {
        Response<CustomerCard> response = customerCardService.createCustomerCard(customerCard);

        if (!response.getErrors().isEmpty()) {
            model.addAttribute("errors", response.getErrors());
            model.addAttribute("customerCard", customerCard);
            return "customer-card-add";
        }

        return "redirect:/customer-card";
    }

    @GetMapping("/edit/{cardNumber}")
    public String showEditForm(@PathVariable String cardNumber, Model model) {
        Response<CustomerCard> response = customerCardService.findCustomerCardByNumber(cardNumber);

        if (!response.getErrors().isEmpty() || response.getObject() == null) {
            model.addAttribute("errors", response.getErrors());
            return "error-page";
        }

        model.addAttribute("customerCard", response.getObject());
        return "customer-card-edit";
    }

    @PostMapping("/edit")
    public String editCustomerCard(@ModelAttribute CustomerCard customerCard, Model model) {
        Response<CustomerCard> response = customerCardService.updateCustomerCard(customerCard);

        if (!response.getErrors().isEmpty()) {
            model.addAttribute("errors", response.getErrors());
            model.addAttribute("customerCard", customerCard);
            return "customer-card-edit";
        }

        return "redirect:/customer-card";
    }

    @PostMapping("/delete/{cardNumber}")
    public String deleteCustomerCard(@PathVariable String cardNumber, Model model) {
        Response<CustomerCard> response = customerCardService.deleteCustomerCard(cardNumber);

        if (!response.getErrors().isEmpty()) {
            model.addAttribute("errors", response.getErrors());
            return "error-page";
        }

        return "redirect:/customer-card";
    }

    @GetMapping("/search")
    public String searchCustomerCardsBySurname(@RequestParam String surname, Model model) {
        Response<List<CustomerCard>> response = customerCardService.findCustomerCardsBySurname(surname);

        if (!response.getErrors().isEmpty()) {
            model.addAttribute("errors", response.getErrors());
            return "error-page";
        }

        model.addAttribute("customerCards", response.getObject());
        model.addAttribute("searchSurname", surname);
        return "customer-card-list";
    }

    @GetMapping("/filter")
    public String filterByPercent(@RequestParam(required = false) Integer percent, Model model) {
        Response<List<CustomerCard>> response = percent != null
                ? customerCardService.findCustomerCardsByPercent(percent)
                : customerCardService.findAll();

        if (!response.getErrors().isEmpty()) {
            model.addAttribute("errors", response.getErrors());
            return "error-page";
        }

        model.addAttribute("customerCards", response.getObject());
        model.addAttribute("selectedPercent", percent);
        return "customer-card-list";
    }

    @GetMapping("/print")
    public String printCustomerCards(Model model) {
        Response<List<CustomerCard>> response = customerCardService.findAll();
        if (!response.getErrors().isEmpty()) {
            model.addAttribute("errors", response.getErrors());
            return "error-page";
        }
        model.addAttribute("customerCards", response.getObject());
        return "customer-card-print";
    }
}