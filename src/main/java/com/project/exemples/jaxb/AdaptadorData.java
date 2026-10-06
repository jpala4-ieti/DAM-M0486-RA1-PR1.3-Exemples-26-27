package com.project.exemples.jaxb;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;

import java.time.LocalDate;

/**
 * JAXB no sap convertir {@link LocalDate} per si sol. Un adaptador indica com passar del text de
 * l'XML (format ISO, aaaa-mm-dd) a l'objecte Java i a l'inrevés.
 */
public class AdaptadorData extends XmlAdapter<String, LocalDate> {

    @Override
    public LocalDate unmarshal(String text) {
        return text == null ? null : LocalDate.parse(text.trim());
    }

    @Override
    public String marshal(LocalDate data) {
        return data == null ? null : data.toString();
    }
}
