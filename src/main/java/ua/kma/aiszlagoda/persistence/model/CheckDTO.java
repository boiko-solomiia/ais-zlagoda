package ua.kma.aiszlagoda.persistence.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CheckDTO {
    private String checkNumber;
    private String idEmployee;
    private String employeeFullName;
    private String cardNumber;
    private String customerFullName;
    private LocalDateTime printDate;
    private Double sumTotal;
    private Double vat;
}
