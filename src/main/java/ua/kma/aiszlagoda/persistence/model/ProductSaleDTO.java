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
public class ProductSaleDTO {
    private String checkNumber;
    private LocalDateTime printDate;
    private String upc;
    private Integer productNumber;
    private Double sellingPrice;
}
