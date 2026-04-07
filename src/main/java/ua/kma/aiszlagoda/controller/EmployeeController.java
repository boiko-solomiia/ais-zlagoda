package ua.kma.aiszlagoda.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import ua.kma.aiszlagoda.persistence.model.Employee;
import ua.kma.aiszlagoda.persistence.model.EmployeeInfo;
import ua.kma.aiszlagoda.persistence.model.Response;
import ua.kma.aiszlagoda.persistence.service.EmployeeService;

import java.util.List;

@Controller
@RequestMapping("/employee")
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @GetMapping
    public String getAllEmployees(Model model) {
        Response<List<Employee>> response = employeeService.findAllEmployees();

        if (!response.getErrors().isEmpty()) {
            model.addAttribute("errors", response.getErrors());
            return "error-page";
        }

        model.addAttribute("employees", response.getObject());
        return "employee-list";
    }

    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("employee", new Employee());
        return "employee-add";
    }

    @PostMapping("/add")
    public String addEmployee(@ModelAttribute Employee employee, Model model) {
        Response<Employee> response = employeeService.createEmployee(employee);

        if (!response.getErrors().isEmpty()) {
            model.addAttribute("errors", response.getErrors());
            model.addAttribute("employee", employee);
            return "employee-add";
        }

        return "redirect:/employee";
    }

    @GetMapping("/edit/{idEmployee}")
    public String showEditForm(@PathVariable String idEmployee, Model model) {
        Response<Employee> response = employeeService.findEmployeeById(idEmployee);

        if (!response.getErrors().isEmpty() || response.getObject() == null) {
            model.addAttribute("errors", response.getErrors());
            return "error-page";
        }

        model.addAttribute("employee", response.getObject());
        return "employee-edit";
    }

    @PostMapping("/edit")
    public String editEmployee(@ModelAttribute Employee employee, Model model) {
        Response<Employee> response = employeeService.updateEmployee(employee);

        if (!response.getErrors().isEmpty()) {
            model.addAttribute("errors", response.getErrors());
            model.addAttribute("employee", employee);
            return "employee-edit";
        }

        return "redirect:/employee";
    }

    @PostMapping("/delete/{idEmployee}")
    public String deleteEmployee(@PathVariable String idEmployee, Model model) {
        Response<Employee> response = employeeService.deleteEmployee(idEmployee);

        if (!response.getErrors().isEmpty()) {
            model.addAttribute("errors", response.getErrors());
            return "error-page";
        }

        return "redirect:/employee";
    }

    @GetMapping("/search")
    public String searchEmployeesBySurname(@RequestParam String surname, Model model) {
        Response<List<Employee>> allEmployees = employeeService.findAllEmployees();

        Response<List<EmployeeInfo>> searchResponse = employeeService.findPhoneAndAddressBySurname(surname);

        if (!allEmployees.getErrors().isEmpty()) {
            model.addAttribute("errors", allEmployees.getErrors());
            return "error-page";
        }

        model.addAttribute("employees", allEmployees.getObject());
        model.addAttribute("searchResults", searchResponse.getObject());
        model.addAttribute("searchSurname", surname);

        return "employee-list";
    }
}