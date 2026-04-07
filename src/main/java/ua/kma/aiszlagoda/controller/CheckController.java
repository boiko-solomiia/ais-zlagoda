package ua.kma.aiszlagoda.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import ua.kma.aiszlagoda.persistence.model.Check;
import ua.kma.aiszlagoda.persistence.model.CustomerCard;
import ua.kma.aiszlagoda.persistence.model.Employee;
import ua.kma.aiszlagoda.persistence.model.Response;
import ua.kma.aiszlagoda.persistence.service.CheckService;
import ua.kma.aiszlagoda.persistence.service.CustomerCardService;
import ua.kma.aiszlagoda.persistence.service.EmployeeService;

import java.util.List;

@Controller
@RequestMapping("/check")
public class CheckController {

    private final CheckService checkService;
    private final EmployeeService employeeService;
    private final CustomerCardService customerCardService;

    public CheckController(CheckService checkService,  EmployeeService employeeService, CustomerCardService customerCardService) {
        this.checkService = checkService;
        this.employeeService = employeeService;
        this.customerCardService = customerCardService;
    }

    @GetMapping
    public String getAllChecks(Model model) {
        Response<List<Check>> response = checkService.findAll();
        if (!response.getErrors().isEmpty()) {
            model.addAttribute("errors", response.getErrors());
            return "error-page";
        }

        model.addAttribute("checks", response.getObject());
        return "check-list";
    }

    @GetMapping("/add")
    public String showAddForm(Model model) {
        Response<List<Employee>> employeesResponse = employeeService.findAllEmployees();
        Response<List<CustomerCard>> cardsResponse = customerCardService.findAll();

        if (!employeesResponse.getErrors().isEmpty() || !cardsResponse.getErrors().isEmpty()) {
            model.addAttribute("errors",
                    !employeesResponse.getErrors().isEmpty()
                            ? employeesResponse.getErrors()
                            : cardsResponse.getErrors());
            return "error-page";
        }

        model.addAttribute("check", new Check());
        model.addAttribute("employees", employeesResponse.getObject());
        model.addAttribute("customerCards", cardsResponse.getObject());

        return "check-add";
    }

    @PostMapping("/add")
    public String addCheck(@ModelAttribute Check myCheck, Model model) {
        Response<Check> response = checkService.createCheck(myCheck);

        if (!response.getErrors().isEmpty()) {
            model.addAttribute("errors", response.getErrors());
            model.addAttribute("check", myCheck);
            model.addAttribute("employees", employeeService.findAllEmployees().getObject());
            model.addAttribute("customerCards", customerCardService.findAll().getObject());
            return "check-add";
        }

        return "redirect:/check";
    }

    @GetMapping("/edit/{checkNumber}")
    public String showEditForm(@PathVariable String checkNumber, Model model) {
        Response<Check> response = checkService.findCheckByNumber(checkNumber);
        Response<List<Employee>> employeesResponse = employeeService.findAllEmployees();
        Response<List<CustomerCard>> cardsResponse = customerCardService.findAll();

        if (!response.getErrors().isEmpty() || response.getObject() == null) {
            model.addAttribute("errors", response.getErrors());
            return "error-page";
        }

        if (!employeesResponse.getErrors().isEmpty() || !cardsResponse.getErrors().isEmpty()) {
            model.addAttribute("errors",
                    !employeesResponse.getErrors().isEmpty()
                            ? employeesResponse.getErrors()
                            : cardsResponse.getErrors());
            return "error-page";
        }

        model.addAttribute("check", response.getObject());
        model.addAttribute("employees", employeesResponse.getObject());
        model.addAttribute("customerCards", cardsResponse.getObject());
        return "check-edit";
    }

    @PostMapping("/edit")
    public String editCheck(@ModelAttribute Check myCheck, Model model) {
        Response<Check> response = checkService.updateCheck(myCheck);

        if (!response.getErrors().isEmpty()) {
            model.addAttribute("errors", response.getErrors());
            model.addAttribute("check", myCheck);
            model.addAttribute("employees", employeeService.findAllEmployees().getObject());
            model.addAttribute("customerCards", customerCardService.findAll().getObject());
            return "check-edit";
        }

        return "redirect:/check";
    }

    @PostMapping("/delete/{checkNumber}")
    public String deleteCheck(@PathVariable String checkNumber, Model model) {
        Response<Check> response = checkService.deleteCheck(checkNumber);

        if (!response.getErrors().isEmpty()) {
            model.addAttribute("errors", response.getErrors());
            return "error-page";
        }

        return "redirect:/check";
    }
}