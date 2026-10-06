package com.project.exemples.jaxb;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import java.util.ArrayList;
import java.util.List;

/**
 * Element arrel {@code <comandes>}. Cada {@code <comanda>} és un objecte {@link Comanda} de la llista.
 *
 * <p>JAXB necessita classes JavaBean: un constructor sense paràmetres i getters i setters.
 * Per això no pot fer servir els records del paquet {@code model}.</p>
 */
@XmlRootElement(name = "comandes")
@XmlAccessorType(XmlAccessType.FIELD)
public class Comandes {

    @XmlElement(name = "comanda")
    private List<Comanda> comandes = new ArrayList<>();

    /** Constructor buit, obligatori per a JAXB. */
    public Comandes() {
    }

    /**
     * @param comandes comandes que conté el document
     */
    public Comandes(List<Comanda> comandes) {
        this.comandes = new ArrayList<>(comandes);
    }

    public List<Comanda> getComandes() {
        return comandes;
    }

    public void setComandes(List<Comanda> comandes) {
        this.comandes = comandes;
    }
}
