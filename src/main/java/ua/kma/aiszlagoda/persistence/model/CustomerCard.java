package ua.kma.aiszlagoda.persistence.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CustomerCard {
    private String cardNumber;
    private String custSurname;
    private String custName;
    private String custPatronymic;
    private String phoneNumber;
    private String city;
    private String street;
    private String zipCode;
    private Integer percent;
}