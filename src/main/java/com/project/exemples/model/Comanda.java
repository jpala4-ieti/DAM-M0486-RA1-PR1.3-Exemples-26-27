package com.project.exemples.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Comanda d'un client.
 *
 * @param id           identificador (per exemple, C1001)
 * @param data         data de la comanda
 * @param client       client que fa la comanda
 * @param linies       línies de la comanda
 * @param observacions text lliure opcional (pot ser {@code null})
 */
public record Comanda(String id, LocalDate data, Client client, List<LiniaComanda> linies, String observacions) {

    /**
     * Fa una còpia immutable de les línies.
     */
    public Comanda {
        linies = List.copyOf(linies);
    }

    /**
     * @return suma dels imports de totes les línies
     */
    public BigDecimal total() {
        return linies.stream().map(LiniaComanda::importTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
