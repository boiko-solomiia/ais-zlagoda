package ua.kma.aiszlagoda.controller;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
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
    private final UserAccountService userAccountService;

    public CheckController(CheckService checkService,
                           EmployeeService employeeService,
                           CustomerCardService customerCardService,
                           SaleService saleService,
                           StoreProductService storeProductService,
                           UserAccountService userAccountService) {
        this.checkService = checkService;
        this.employeeService = employeeService;
        this.customerCardService = customerCardService;
        this.saleService = saleService;
        this.storeProductService = storeProductService;
        this.userAccountService = userAccountService;
    }

    private boolean isManager(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_MANAGER"));
    }

    private String getCurrentEmployeeId(Authentication authentication) {
        Response<AuthUser> response = userAccountService.findAuthUserByUsername(authentication.getName());

        if (response.getObject() == null) {
            throw new RuntimeException("Current user not found");
        }

        return response.getObject().getIdEmployee();
    }

    @GetMapping
    public String getAllChecks(Authentication authentication, Model model) {
        checkService.deleteChecksOlderThanThreeYears();

        boolean manager = isManager(authentication);
        String currentEmployeeId = getCurrentEmployeeId(authentication);

        Response<List<CheckDTO>> response;
        double sumTotal;

        if (manager) {
            response = checkService.findAllDTO();
            sumTotal = checkService.checkSumTotalByPeriod(
                    LocalDateTime.of(1970, 1, 1, 0, 0),
                    LocalDateTime.now()
            );

            model.addAttribute("employees", employeeService.findAllCashiers().getObject());
        } else {
            LocalDateTime start = LocalDateTime.of(1970, 1, 1, 0, 0);
            LocalDateTime end = LocalDateTime.now();

            response = checkService.findChecksDTOByEmployeeAndPeriod(currentEmployeeId, start, end);
            sumTotal = checkService.checkSumTotalByEmployeeAndPeriod(currentEmployeeId, start, end);

            Response<Employee> currentCashier = employeeService.findEmployeeById(currentEmployeeId);
            model.addAttribute("employees", currentCashier.getObject() != null
                    ? List.of(currentCashier.getObject())
                    : List.of());
        }

        if (!response.getErrors().isEmpty()) {
            model.addAttribute("errors", response.getErrors());
            return "error-page";
        }

        model.addAttribute("checks", response.getObject());
        model.addAttribute("sumTotal", sumTotal);
        return "check-list";
    }

    @GetMapping("/filter")
    public String filterChecks(
            @RequestParam(required = false) String employeeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            Authentication authentication,
            Model model) {

        boolean manager = isManager(authentication);
        String currentEmployeeId = getCurrentEmployeeId(authentication);

        String effectiveEmployeeId = manager ? employeeId : currentEmployeeId;

        LocalDateTime start = (dateFrom != null)
                ? dateFrom.atStartOfDay()
                : LocalDateTime.of(1970, 1, 1, 0, 0);

        LocalDateTime end = (dateTo != null)
                ? dateTo.atTime(LocalTime.MAX)
                : LocalDateTime.now();

        Response<List<CheckDTO>> response;
        double sumTotal;

        if (effectiveEmployeeId != null && !effectiveEmployeeId.isBlank()) {
            response = checkService.findChecksDTOByEmployeeAndPeriod(effectiveEmployeeId, start, end);
            sumTotal = checkService.checkSumTotalByEmployeeAndPeriod(effectiveEmployeeId, start, end);
        } else {
            response = checkService.findChecksDTOByPeriod(start, end);
            sumTotal = checkService.checkSumTotalByPeriod(start, end);
        }

        if (!response.getErrors().isEmpty()) {
            model.addAttribute("errors", response.getErrors());
            return "error-page";
        }

        model.addAttribute("checks", response.getObject());
        model.addAttribute("sumTotal", sumTotal);
        model.addAttribute("selectedEmployee", effectiveEmployeeId);
        model.addAttribute("dateFrom", dateFrom);
        model.addAttribute("dateTo", dateTo);

        if (manager) {
            model.addAttribute("employees", employeeService.findAllCashiers().getObject());
        } else {
            Response<Employee> currentCashier = employeeService.findEmployeeById(currentEmployeeId);
            model.addAttribute("employees", currentCashier.getObject() != null
                    ? List.of(currentCashier.getObject())
                    : List.of());
        }

        return "check-list";
    }

    @GetMapping("/info/{checkNumber}")
    public String checkInfo(@PathVariable String checkNumber,
                            Authentication authentication,
                            Model model) {
        Response<CheckDTO> checkResponse = checkService.findCheckDTOByNumber(checkNumber);

        if (!checkResponse.getErrors().isEmpty() || checkResponse.getObject() == null) {
            model.addAttribute("errors", checkResponse.getErrors());
            return "error-page";
        }

        CheckDTO check = checkResponse.getObject();

        if (!isManager(authentication)) {
            String currentEmployeeId = getCurrentEmployeeId(authentication);
            if (!check.getIdEmployee().equals(currentEmployeeId)) {
                model.addAttribute("errors", List.of("You can view only your own checks"));
                return "error-page";
            }
        }

        Response<List<Sale>> salesResponse = saleService.findSalesForCheck(checkNumber);

        model.addAttribute("check", check);
        model.addAttribute("sales", salesResponse.getObject());
        return "check-info";
    }

    @GetMapping("/add")
    public String showAddForm(Authentication authentication, Model model) {
        String currentEmployeeId = getCurrentEmployeeId(authentication);

        Response<Employee> cashierResponse = employeeService.findEmployeeById(currentEmployeeId);
        Response<List<CustomerCard>> cardsResponse = customerCardService.findAll();
        Response<List<StoreProductInfo>> productsResponse = storeProductService.findAllStoreProductsSortedByName();

        if (cashierResponse.getObject() == null
            || !cardsResponse.getErrors().isEmpty()
            || !productsResponse.getErrors().isEmpty()) {
            model.addAttribute("errors", Stream.of(
                            cashierResponse.getErrors(),
                            cardsResponse.getErrors(),
                            productsResponse.getErrors()
                    )
                    .flatMap(List::stream)
                    .collect(Collectors.toList()));
            return "error-page";
        }

        model.addAttribute("check", new Check());
        model.addAttribute("employees", List.of(cashierResponse.getObject()));
        model.addAttribute("customerCards", cardsResponse.getObject());
        model.addAttribute("products", productsResponse.getObject());
        return "check-add";
    }

    @PostMapping("/add-with-sales")
    public String addCheckWithSales(@ModelAttribute Check check,
                                    @RequestParam(value = "upc", required = false) List<String> upcList,
                                    @RequestParam(value = "quantity", required = false) List<Integer> quantityList,
                                    Authentication authentication,
                                    Model model) {

        check.setIdEmployee(getCurrentEmployeeId(authentication));

        if (upcList == null || quantityList == null || upcList.isEmpty()) {
            model.addAttribute("errors", List.of("Додайте хоча б один товар"));
            return prepareAddFormWithErrors(model, check, authentication);
        }

        List<SaleRequest> sales = new ArrayList<>();
        for (int i = 0; i < upcList.size(); i++) {
            if (upcList.get(i) != null
                && !upcList.get(i).isBlank()
                && quantityList.get(i) != null
                && quantityList.get(i) > 0) {
                sales.add(new SaleRequest(upcList.get(i), quantityList.get(i)));
            }
        }

        if (sales.isEmpty()) {
            model.addAttribute("errors", List.of("Необхідно додати хоча б один товар з коректною кількістю"));
            return prepareAddFormWithErrors(model, check, authentication);
        }

        Response<Check> response = checkService.createCheckWithSales(check, sales);
        if (!response.getErrors().isEmpty()) {
            model.addAttribute("errors", response.getErrors());
            return prepareAddFormWithErrors(model, check, authentication);
        }

        return "redirect:/check";
    }

    private String prepareAddFormWithErrors(Model model, Check check, Authentication authentication) {
        String currentEmployeeId = getCurrentEmployeeId(authentication);

        model.addAttribute("check", check);

        Response<Employee> cashierResponse = employeeService.findEmployeeById(currentEmployeeId);
        model.addAttribute("employees", cashierResponse.getObject() != null
                ? List.of(cashierResponse.getObject())
                : List.of());

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
    public String searchByCheckNumber(@RequestParam String checkNumber,
                                      Authentication authentication,
                                      Model model) {
        Response<CheckDTO> checkResponse = checkService.findCheckDTOByNumber(checkNumber);

        if (checkResponse.getObject() == null) {
            model.addAttribute("checks", List.of());

            if (isManager(authentication)) {
                model.addAttribute("employees", employeeService.findAllCashiers().getObject());
            } else {
                String currentEmployeeId = getCurrentEmployeeId(authentication);
                Response<Employee> currentCashier = employeeService.findEmployeeById(currentEmployeeId);
                model.addAttribute("employees", currentCashier.getObject() != null
                        ? List.of(currentCashier.getObject())
                        : List.of());
            }

            model.addAttribute("searchedNumber", checkNumber);
            return "check-list";
        }

        CheckDTO check = checkResponse.getObject();

        if (!isManager(authentication)) {
            String currentEmployeeId = getCurrentEmployeeId(authentication);
            if (!check.getIdEmployee().equals(currentEmployeeId)) {
                model.addAttribute("errors", List.of("You can view only your own checks"));
                return "error-page";
            }
        }

        Response<List<Sale>> salesResponse = saleService.findSalesForCheck(checkNumber);

        model.addAttribute("check", check);
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