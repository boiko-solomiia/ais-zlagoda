package ua.kma.aiszlagoda.persistence.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Sale {
    private String upc;
    private String checkNumber;
    private Integer productNumber;
    private Double sellingPrice;
}
