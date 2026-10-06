package com.project.exemples.comu;

import com.project.exemples.Fluxos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXParseException;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Utilitats: lectura i escriptura d'XML")
class XmlTest {

    private static final String AMB_ESPAI_DE_NOMS = "<a:arrel xmlns:a=\"urn:prova\"><a:fill>text</a:fill></a:arrel>";

    @TempDir
    Path temporal;

    @Test
    @DisplayName("Per defecte no té en compte els espais de noms")
    void senseEspaisDeNoms() throws Exception {
        Document document = Xml.llegir(Fluxos.de(AMB_ESPAI_DE_NOMS));
        assertNull(document.getDocumentElement().getNamespaceURI());
        assertEquals("a:arrel", document.getDocumentElement().getTagName());
    }

    @Test
    @DisplayName("Pot llegir tenint en compte els espais de noms")
    void ambEspaisDeNoms() throws Exception {
        Document document = Xml.llegir(Fluxos.de(AMB_ESPAI_DE_NOMS), true);
        assertEquals("urn:prova", document.getDocumentElement().getNamespaceURI());
        assertEquals("arrel", document.getDocumentElement().getLocalName());
    }

    @Test
    @DisplayName("Converteix un document en text indentat amb declaració")
    void convertirEnText() {
        Document document = Xml.documentNou();
        Element arrel = document.createElement("arrel");
        arrel.appendChild(document.createElement("fill"));
        document.appendChild(arrel);
        String text = Fluxos.unificarSalts(Xml.aText(document));
        assertTrue(text.startsWith("<?xml"));
        assertTrue(text.contains("\n  <fill/>"), text);
    }

    @Test
    @DisplayName("Desa en UTF-8 i crea les carpetes que falten")
    void desarEnUtf8() throws Exception {
        Document document = Xml.documentNou();
        Element arrel = document.createElement("botiga");
        arrel.setTextContent("Botiga d'informàtica");
        document.appendChild(arrel);
        Path fitxer = temporal.resolve("carpeta/nova/botiga.xml");
        Xml.desar(document, fitxer);
        String contingut = Files.readString(fitxer, StandardCharsets.UTF_8);
        assertTrue(contingut.contains("Botiga d'informàtica"));
    }

    @Test
    @DisplayName("Rebutja els documents amb DOCTYPE (parser segur)")
    void rebutjaDoctype() {
        String xml = "<!DOCTYPE arrel [<!ENTITY e \"x\">]><arrel>&e;</arrel>";
        assertThrows(SAXParseException.class, () -> Xml.llegir(Fluxos.de(xml)));
    }

    @Test
    @DisplayName("Un XML mal format no escriu res per la sortida d'errors")
    void senseSortidaDErrors() throws Exception {
        String errors = Fluxos.capturarErrors(() ->
                assertThrows(SAXParseException.class, () -> Xml.llegir(Fluxos.de("<arrel><fill></arrel>"))));
        assertEquals("", errors);
    }
}
