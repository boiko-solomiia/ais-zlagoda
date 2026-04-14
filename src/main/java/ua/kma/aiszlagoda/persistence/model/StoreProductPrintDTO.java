package ua.kma.aiszlagoda.persistence.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StoreProductPrintDTO {
    private String upc;
    private String productName;
    private String characteristics;
    private Double sellingPrice;
    private Integer productsNumber;
    private Boolean promotionalProduct;

    public String getPromotionalProductFormatted() {
        return promotionalProduct != null && promotionalProduct ? "Yes" : "No";
    }
}