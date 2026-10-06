package com.project.exemples.jaxb;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

/**
 * Client d'una comanda. Sense anotacions als atributs, JAXB els converteix en elements
 * amb el mateix nom ({@code <nom>} i {@code <email>}), en l'ordre indicat a propOrder.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {"nom", "email"})
public class Client {

    private String nom;
    private String email;

    /** Constructor buit, obligatori per a JAXB. */
    public Client() {
    }

    /**
     * @param nom   nom complet
     * @param email adreça de correu
     */
    public Client(String nom, String email) {
        this.nom = nom;
        this.email = email;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
