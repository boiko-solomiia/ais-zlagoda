package ua.kma.aiszlagoda.persistence.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Check {
    private Integer check_number;
    private Integer id_employee;
    private Integer card_number;
    private Date print_date;
    private Double sum_total;
    private Double vat;

}