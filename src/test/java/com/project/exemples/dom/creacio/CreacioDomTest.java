package com.project.exemples.dom.creacio;

import com.project.exemples.Fluxos;
import com.project.exemples.comu.Xml;
import com.project.exemples.model.Client;
import com.project.exemples.model.Comanda;
import com.project.exemples.model.LiniaComanda;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Exemple 2: creació d'un document amb DOM")
class CreacioDomTest {

    private static final List<LiniaComanda> LINIES = List.of(
            new LiniaComanda("P001", 2, new BigDecimal("49.90")),
            new LiniaComanda("P006", 1, new BigDecimal("12.00")));

    @TempDir
    Path temporal;

    private static Comanda comanda(String nomClient, String observacions) {
        return new Comanda("C2001", LocalDate.of(2026, 10, 5),
                new Client(nomClient, "client@example.com"), LINIES, observacions);
    }

    private static Element unic(Document document, String nom) {
        NodeList nodes = document.getElementsByTagName(nom);
        assertEquals(1, nodes.getLength(), "Hi hauria d'haver un sol <" + nom + ">");
        return (Element) nodes.item(0);
    }

    @Test
    @DisplayName("L'element arrel té l'identificador i la data en format ISO")
    void elementArrel() throws Exception {
        Element arrel = CreacioDom.crearComanda(comanda("Laia Ferrer", null)).getDocumentElement();
        assertAll(
                () -> assertEquals("comanda", arrel.getTagName()),
                () -> assertEquals("C2001", arrel.getAttribute("id")),
                () -> assertEquals("2026-10-05", arrel.getAttribute("data")));
    }

    @Test
    @DisplayName("El client té el nom i el correu com a elements")
    void dadesDelClient() throws Exception {
        Document document = CreacioDom.crearComanda(comanda("Laia Ferrer", null));
        Element client = unic(document, "client");
        assertEquals("Laia Ferrer", client.getElementsByTagName("nom").item(0).getTextContent());
        assertEquals("client@example.com", client.getElementsByTagName("email").item(0).getTextContent());
    }

    @Test
    @DisplayName("Hi ha una línia per a cada producte, amb les dades com a atributs")
    void liniesDeLaComanda() throws Exception {
        Document document = CreacioDom.crearComanda(comanda("Laia Ferrer", null));
        NodeList linies = unic(document, "linies").getElementsByTagName("linia");
        assertEquals(2, linies.getLength());
        Element primera = (Element) linies.item(0);
        assertAll(
                () -> assertEquals("P001", primera.getAttribute("producte")),
                () -> assertEquals("2", primera.getAttribute("quantitat")),
                () -> assertEquals("49.90", primera.getAttribute("preuUnitari")));
    }

    @Test
    @DisplayName("El total és la suma de les línies")
    void total() throws Exception {
        Element total = unic(CreacioDom.crearComanda(comanda("Laia Ferrer", null)), "total");
        assertEquals("111.80", total.getTextContent());
        assertEquals("EUR", total.getAttribute("moneda"));
    }

    @Test
    @DisplayName("Les observacions es desen en una secció CDATA")
    void observacionsEnCdata() throws Exception {
        Element observacions = unic(CreacioDom.crearComanda(comanda("Laia Ferrer", "Porta <A&B>")), "observacions");
        Node contingut = observacions.getFirstChild();
        assertEquals(Node.CDATA_SECTION_NODE, contingut.getNodeType());
        assertEquals("Porta <A&B>", contingut.getNodeValue());
    }

    @ParameterizedTest(name = "observacions = [{0}]")
    @NullSource
    @ValueSource(strings = {"", "   "})
    @DisplayName("Sense observacions no es crea l'element")
    void senseObservacions(String observacions) throws Exception {
        Document document = CreacioDom.crearComanda(comanda("Laia Ferrer", observacions));
        assertEquals(0, document.getElementsByTagName("observacions").getLength());
    }

    @Test
    @DisplayName("El primer fill de l'arrel és un comentari")
    void comentari() throws Exception {
        Node primer = CreacioDom.crearComanda(comanda("Laia Ferrer", null)).getDocumentElement().getFirstChild();
        assertEquals(Node.COMMENT_NODE, primer.getNodeType());
    }

    @Test
    @DisplayName("Els caràcters especials s'escapen en desar i es recuperen en llegir")
    void caractersEspecials() throws Exception {
        String text = CreacioDom.aText(CreacioDom.crearComanda(comanda("Ferrer & Fills <SL>", null)));
        assertTrue(text.contains("Ferrer &amp; Fills &lt;SL&gt;"), text);
        Document llegit = Xml.llegir(Fluxos.de(text));
        assertEquals("Ferrer & Fills <SL>", llegit.getElementsByTagName("nom").item(0).getTextContent());
    }

    @Test
    @DisplayName("El text resultant té declaració i està indentat")
    void textIndentat() throws Exception {
        String text = Fluxos.unificarSalts(CreacioDom.aText(CreacioDom.crearComanda(comanda("Laia Ferrer", null))));
        assertTrue(text.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\""), text);
        assertTrue(text.contains("\n    <client>"), text);
    }

    @Test
    @DisplayName("Desa el fitxer en UTF-8 i crea les carpetes que falten")
    void desarFitxer() throws Exception {
        Path fitxer = temporal.resolve("comandes/2026/C2001.xml");
        CreacioDom.desar(CreacioDom.crearComanda(comanda("Núria Sàez", null)), fitxer);
        String contingut = Files.readString(fitxer, StandardCharsets.UTF_8);
        assertTrue(contingut.contains("Núria Sàez"));
        assertEquals("comanda", Xml.llegir(Files.newInputStream(fitxer)).getDocumentElement().getTagName());
    }
}
