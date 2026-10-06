package com.project.exemples.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Producte del catàleg de la botiga.
 *
 * @param id         identificador (per exemple, P001)
 * @param categoria  categoria del producte
 * @param nom        nom comercial
 * @param preu       preu unitari (BigDecimal: els imports no s'han de guardar mai amb double)
 * @param moneda     codi de la moneda (EUR)
 * @param estoc      unitats disponibles
 * @param etiquetes  etiquetes de cerca
 * @param descripcio descripció opcional (pot ser {@code null})
 */
public record Producte(String id, String categoria, String nom, BigDecimal preu, String moneda,
                       int estoc, List<String> etiquetes, String descripcio) {

    /**
     * Fa una còpia immutable de les etiquetes perquè el record no es pugui modificar des de fora.
     */
    public Producte {
        etiquetes = List.copyOf(etiquetes);
    }

    /**
     * @return la descripció, si n'hi ha
     */
    public Optional<String> descripcioOpcional() {
        return Optional.ofNullable(descripcio);
    }

    /**
     * @return {@code true} si queda alguna unitat
     */
    public boolean teEstoc() {
        return estoc > 0;
    }
}
