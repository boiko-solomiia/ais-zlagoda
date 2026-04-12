package ua.kma.aiszlagoda.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import ua.kma.aiszlagoda.persistence.model.*;
import ua.kma.aiszlagoda.persistence.service.CheckService;
import ua.kma.aiszlagoda.persistence.service.CustomerCardService;
import ua.kma.aiszlagoda.persistence.service.EmployeeService;
import ua.kma.aiszlagoda.persistence.service.SaleService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/check")
public class CheckController {

    private final CheckService checkService;
    private final EmployeeService employeeService;
    private final CustomerCardService customerCardService;

    private final SaleService saleService;

    public CheckController(CheckService checkService, EmployeeService employeeService,
                           CustomerCardService customerCardService, SaleService saleService) {
        this.checkService = checkService;
        this.employeeService = employeeService;
        this.customerCardService = customerCardService;
        this.saleService = saleService;
    }

    @GetMapping
    public String getAllChecks(Model model) {
        Response<List<Check>> response = checkService.findAll();
        if (!response.getErrors().isEmpty()) {
            model.addAttribute("errors", response.getErrors());
            return "error-page";
        }

        model.addAttribute("checks", response.getObject());
        model.addAttribute("employees", employeeService.findAllCashiers().getObject());
        return "check-list";
    }

    @GetMapping("/filter")
    public String filterChecks(
            @RequestParam(required = false) String employeeId,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            Model model) {

        LocalDateTime from = (dateFrom != null && !dateFrom.isBlank())
                ? LocalDate.parse(dateFrom).atStartOfDay()
                : LocalDateTime.of(2000, 1, 1, 0, 0);
        LocalDateTime to = (dateTo != null && !dateTo.isBlank())
                ? LocalDate.parse(dateTo).atTime(23, 59, 59)
                : LocalDateTime.now();

        Response<List<Check>> response;
        double sumTotal;

        if (employeeId != null && !employeeId.isBlank()) {
            response = checkService.findChecksByEmployeeAndPeriod(employeeId, from, to);
            sumTotal = checkService.checkSumTotalByEmployeeAndPeriod(employeeId, from, to);
        } else {
            response = checkService.findChecksByPeriod(from, to);
            sumTotal = checkService.checkSumTotalByPeriod(from, to);
        }

        if (!response.getErrors().isEmpty()) {
            model.addAttribute("errors", response.getErrors());
            return "error-page";
        }

        model.addAttribute("checks", response.getObject());
        model.addAttribute("employees", employeeService.findAllCashiers().getObject());
        model.addAttribute("selectedEmployee", employeeId);
        model.addAttribute("dateFrom", dateFrom);
        model.addAttribute("dateTo", dateTo);
        model.addAttribute("sumTotal", sumTotal);
        return "check-list";
    }

    @GetMapping("/info/{checkNumber}")
    public String checkInfo(@PathVariable String checkNumber, Model model) {
        Response<Check> checkResponse = checkService.findCheckByNumber(checkNumber);

        if (!checkResponse.getErrors().isEmpty() || checkResponse.getObject() == null) {
            model.addAttribute("errors", checkResponse.getErrors());
            return "error-page";
        }

        Response<List<ua.kma.aiszlagoda.persistence.model.Sale>> salesResponse =
                saleService.findSalesForCheck(checkNumber);

        model.addAttribute("check", checkResponse.getObject());
        model.addAttribute("sales", salesResponse.getObject());
        return "check-info";
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

    @GetMapping("/search")
    public String searchByCheckNumber(@RequestParam String checkNumber, Model model) {
        Response<Check> checkResponse = checkService.findCheckByNumber(checkNumber);

        if (checkResponse.getObject() == null) {
            model.addAttribute("checks", List.of());
            model.addAttribute("employees", employeeService.findAllCashiers().getObject());
            model.addAttribute("searchedNumber", checkNumber);
            return "check-list";
        }

        Response<List<ua.kma.aiszlagoda.persistence.model.Sale>> salesResponse =
                saleService.findSalesForCheck(checkNumber);

        model.addAttribute("check", checkResponse.getObject());
        model.addAttribute("sales", salesResponse.getObject());
        model.addAttribute("searchedNumber", checkNumber);
        return "check-info";
    }
}