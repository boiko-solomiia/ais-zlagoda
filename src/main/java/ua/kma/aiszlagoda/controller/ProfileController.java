package ua.kma.aiszlagoda.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import ua.kma.aiszlagoda.persistence.model.CheckDTO;
import ua.kma.aiszlagoda.persistence.model.Employee;
import ua.kma.aiszlagoda.persistence.model.Response;
import ua.kma.aiszlagoda.persistence.service.CheckService;
import ua.kma.aiszlagoda.persistence.service.EmployeeService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Controller
public class ProfileController {

    private final EmployeeService employeeService;
    private final CheckService checkService;

    public ProfileController(EmployeeService employeeService, CheckService checkService) {
        this.employeeService = employeeService;
        this.checkService = checkService;
    }

    @GetMapping("/profile")
    public String showProfile(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();

        Employee employee = employeeService.findEmployeeByUsername(username).getObject();
        if (employee == null) return "redirect:/login";

        model.addAttribute("employee", employee);

        if ("касир".equalsIgnoreCase(employee.getEmplRole())) {
            LocalDateTime start = LocalDate.now().atStartOfDay();
            LocalDateTime end = LocalDate.now().atTime(LocalTime.MAX);

            Response<List<CheckDTO>> checksResponse = checkService.findChecksDTOByEmployeeAndPeriod(
                    employee.getIdEmployee(), start, end);

            model.addAttribute("todayChecks", checksResponse.getObject());
        }
        return "profile";
    }
}