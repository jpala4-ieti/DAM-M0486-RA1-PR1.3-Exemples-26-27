package com.project.exemples.jaxb;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;

import java.math.BigDecimal;

/**
 * Línia d'una comanda. Totes les dades són atributs de l'element {@code <linia>}.
 * JAXB converteix automàticament els tipus bàsics (int, BigDecimal...).
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class Linia {

    @XmlAttribute(required = true)
    private String producte;

    @XmlAttribute(required = true)
    private int quantitat;

    @XmlAttribute(required = true)
    private BigDecimal preuUnitari;

    /** Constructor buit, obligatori per a JAXB. */
    public Linia() {
    }

    /**
     * @param producte    identificador del producte
     * @param quantitat   unitats
     * @param preuUnitari preu de cada unitat
     */
    public Linia(String producte, int quantitat, BigDecimal preuUnitari) {
        this.producte = producte;
        this.quantitat = quantitat;
        this.preuUnitari = preuUnitari;
    }

    /**
     * @return import de la línia (valor calculat, no apareix a l'XML)
     */
    public BigDecimal getImport() {
        return preuUnitari.multiply(BigDecimal.valueOf(quantitat));
    }

    public String getProducte() {
        return producte;
    }

    public void setProducte(String producte) {
        this.producte = producte;
    }

    public int getQuantitat() {
        return quantitat;
    }

    public void setQuantitat(int quantitat) {
        this.quantitat = quantitat;
    }

    public BigDecimal getPreuUnitari() {
        return preuUnitari;
    }

    public void setPreuUnitari(BigDecimal preuUnitari) {
        this.preuUnitari = preuUnitari;
    }
}
