package com.project.exemples.jaxb;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Una comanda. Amb {@code XmlAccessType.FIELD}, JAXB fa servir els atributs de la classe
 * (no els getters) per decidir què es llegeix i s'escriu.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {"client", "linies"})
public class Comanda {

    @XmlAttribute(required = true)
    private String id;

    @XmlAttribute(required = true)
    @XmlJavaTypeAdapter(AdaptadorData.class)
    private LocalDate data;

    @XmlElement(required = true)
    private Client client;

    /** {@code @XmlElementWrapper} crea l'element contenidor {@code <linies>} al voltant de cada {@code <linia>}. */
    @XmlElementWrapper(name = "linies")
    @XmlElement(name = "linia")
    private List<Linia> linies = new ArrayList<>();

    /** Constructor buit, obligatori per a JAXB. */
    public Comanda() {
    }

    /**
     * @param id     identificador
     * @param data   data de la comanda
     * @param client client
     * @param linies línies de la comanda
     */
    public Comanda(String id, LocalDate data, Client client, List<Linia> linies) {
        this.id = id;
        this.data = data;
        this.client = client;
        this.linies = new ArrayList<>(linies);
    }

    /**
     * Import total de la comanda. És un valor calculat: com que no hi ha cap atribut de la classe
     * que el guardi, no apareix a l'XML.
     *
     * @return suma de les línies
     */
    public BigDecimal getTotal() {
        return linies.stream().map(Linia::getImport).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public LocalDate getData() {
        return data;
    }

    public void setData(LocalDate data) {
        this.data = data;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public List<Linia> getLinies() {
        return linies;
    }

    public void setLinies(List<Linia> linies) {
        this.linies = linies;
    }
}
