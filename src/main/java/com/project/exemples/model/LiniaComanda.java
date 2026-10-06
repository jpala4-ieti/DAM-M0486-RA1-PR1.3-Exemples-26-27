package com.project.exemples.model;

import java.math.BigDecimal;

/**
 * Línia d'una comanda: quantes unitats d'un producte i a quin preu.
 *
 * @param producte    identificador del producte
 * @param quantitat   unitats demanades
 * @param preuUnitari preu de cada unitat
 */
public record LiniaComanda(String producte, int quantitat, BigDecimal preuUnitari) {

    /**
     * @return import de la línia (quantitat per preu unitari)
     */
    public BigDecimal importTotal() {
        return preuUnitari.multiply(BigDecimal.valueOf(quantitat));
    }
}
