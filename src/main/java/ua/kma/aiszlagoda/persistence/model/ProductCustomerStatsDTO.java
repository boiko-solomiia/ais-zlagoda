package ua.kma.aiszlagoda.persistence.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

public class ProductCustomerStatsDTO {

    private Integer productId;
    private String productName;
    private String manufacturer;
    private Integer differentCustomersCount;
    private Integer totalUnitsSold;
    private Double totalSalesAmount;
}