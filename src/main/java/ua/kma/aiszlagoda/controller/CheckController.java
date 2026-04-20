package ua.kma.aiszlagoda.controller;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import ua.kma.aiszlagoda.persistence.model.*;
import ua.kma.aiszlagoda.persistence.service.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Controller
@RequestMapping("/check")
public class CheckController {

    private final CheckService checkService;
    private final EmployeeService employeeService;
    private final CustomerCardService customerCardService;
    private final StoreProductService storeProductService;

    private final SaleService saleService;

    public CheckController(CheckService checkService, EmployeeService employeeService,
                           CustomerCardService customerCardService, SaleService saleService, StoreProductService storeProductService) {
        this.checkService = checkService;
        this.employeeService = employeeService;
        this.customerCardService = customerCardService;
        this.saleService = saleService;
        this.storeProductService = storeProductService;
    }

    @GetMapping
    public String getAllChecks(Model model) {
        checkService.deleteChecksOlderThanThreeYears();
        Response<List<CheckDTO>> response = checkService.findAllDTO();
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
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            Model model) {

        LocalDateTime start = (dateFrom != null) ? dateFrom.atStartOfDay() : LocalDateTime.of(1970, 1, 1, 0, 0);
        LocalDateTime end = (dateTo != null) ? dateTo.atTime(LocalTime.MAX) : LocalDateTime.now();

        Response<List<CheckDTO>> response;
        double sumTotal;

        if (employeeId != null && !employeeId.isBlank()) {
            response = checkService.findChecksDTOByEmployeeAndPeriod(employeeId, start, end);
            sumTotal = checkService.checkSumTotalByEmployeeAndPeriod(employeeId, start, end);
        } else {
            response = checkService.findChecksDTOByPeriod(start, end);
            sumTotal = checkService.checkSumTotalByPeriod(start, end);
        }

        if (!response.getErrors().isEmpty()) {
            model.addAttribute("errors", response.getErrors());
            return "error-page";
        }

        model.addAttribute("checks", response.getObject());
        model.addAttribute("employees", employeeService.findAllCashiers().getObject());
        model.addAttribute("sumTotal", sumTotal);

        model.addAttribute("selectedEmployee", employeeId);
        model.addAttribute("dateFrom", dateFrom);
        model.addAttribute("dateTo", dateTo);

        return "check-list";
    }

    @GetMapping("/info/{checkNumber}")
    public String checkInfo(@PathVariable String checkNumber, Model model) {
        Response<CheckDTO> checkResponse = checkService.findCheckDTOByNumber(checkNumber);

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
        Response<List<Employee>> cashiersResponse = employeeService.findAllCashiers(); // тільки касири
        Response<List<CustomerCard>> cardsResponse = customerCardService.findAll();
        Response<List<StoreProductInfo>> productsResponse = storeProductService.findAllStoreProductsSortedByName();

        if (!cashiersResponse.getErrors().isEmpty() || !cardsResponse.getErrors().isEmpty() || !productsResponse.getErrors().isEmpty()) {
            model.addAttribute("errors", Stream.of(cashiersResponse.getErrors(), cardsResponse.getErrors(), productsResponse.getErrors())
                    .flatMap(List::stream).collect(Collectors.toList()));
            return "error-page";
        }

        model.addAttribute("check", new Check());
        model.addAttribute("employees", cashiersResponse.getObject()); // тільки касири
        model.addAttribute("customerCards", cardsResponse.getObject());
        model.addAttribute("products", productsResponse.getObject());
        return "check-add";
    }

    @PostMapping("/add-with-sales")
    public String addCheckWithSales(@ModelAttribute Check check,
                                    @RequestParam(value = "upc", required = false) List<String> upcList,
                                    @RequestParam(value = "quantity", required = false) List<Integer> quantityList,
                                    Model model) {
        if (upcList == null || quantityList == null || upcList.isEmpty()) {
            model.addAttribute("errors", List.of("Додайте хоча б один товар"));
            return prepareAddFormWithErrors(model, check);
        }

        List<SaleRequest> sales = new ArrayList<>();
        for (int i = 0; i < upcList.size(); i++) {
            if (upcList.get(i) != null && !upcList.get(i).isBlank() && quantityList.get(i) != null && quantityList.get(i) > 0) {
                sales.add(new SaleRequest(upcList.get(i), quantityList.get(i)));
            }
        }

        if (sales.isEmpty()) {
            model.addAttribute("errors", List.of("Необхідно додати хоча б один товар з коректною кількістю"));
            return prepareAddFormWithErrors(model, check);
        }

        Response<Check> response = checkService.createCheckWithSales(check, sales);
        if (!response.getErrors().isEmpty()) {
            model.addAttribute("errors", response.getErrors());
            return prepareAddFormWithErrors(model, check);
        }

        return "redirect:/check";
    }

    private String prepareAddFormWithErrors(Model model, Check check) {
        model.addAttribute("check", check);
        model.addAttribute("employees", employeeService.findAllCashiers().getObject());
        model.addAttribute("customerCards", customerCardService.findAll().getObject());
        model.addAttribute("products", storeProductService.findAllStoreProductsSortedByName().getObject());
        return "check-add";
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

    @GetMapping("/print")
    public String printChecks(Model model) {
        Response<List<CheckDTO>> response = checkService.findAllDTO();
        double sumTotal = checkService.checkSumTotalByPeriod(
                LocalDateTime.of(1970, 1, 1, 0, 0),
                LocalDateTime.now()
        );

        if (!response.getErrors().isEmpty()) {
            model.addAttribute("errors", response.getErrors());
            return "error-page";
        }

        model.addAttribute("checks", response.getObject());
        model.addAttribute("sumTotal", sumTotal);
        return "check-print";
    }
}