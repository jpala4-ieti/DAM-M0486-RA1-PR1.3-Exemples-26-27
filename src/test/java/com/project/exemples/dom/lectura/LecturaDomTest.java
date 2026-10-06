package com.project.exemples.dom.lectura;

import com.project.exemples.Fluxos;
import com.project.exemples.comu.Recursos;
import com.project.exemples.model.Producte;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Exemple 1: lectura amb DOM")
class LecturaDomTest {

    private static Document cataleg;
    private static List<Producte> productes;

    @BeforeAll
    static void carregarCataleg() throws Exception {
        try (InputStream entrada = Recursos.dades("cataleg.xml")) {
            cataleg = LecturaDom.carregar(entrada);
        }
        productes = LecturaDom.llegirProductes(cataleg);
    }

    private static Document documentDe(String xml) throws Exception {
        return LecturaDom.carregar(Fluxos.de(xml));
    }

    @Test
    @DisplayName("Llegeix tots els productes en l'ordre del document")
    void llegeixTotsElsProductes() {
        assertEquals(List.of("P001", "P002", "P003", "P004", "P005", "P006"),
                productes.stream().map(Producte::id).toList());
    }

    @Test
    @DisplayName("Converteix atributs, textos i tipus del primer producte")
    void convertiixElPrimerProducte() {
        Producte producte = productes.get(0);
        assertAll(
                () -> assertEquals("periferics", producte.categoria()),
                () -> assertEquals("Teclat mecànic", producte.nom()),
                () -> assertEquals(new BigDecimal("49.90"), producte.preu()),
                () -> assertEquals("EUR", producte.moneda()),
                () -> assertEquals(25, producte.estoc()),
                () -> assertEquals(List.of("gaming", "usb"), producte.etiquetes()));
    }

    @Test
    @DisplayName("El preu conserva els decimals del fitxer")
    void conservaElsDecimals() {
        assertEquals("229.00", productes.get(2).preu().toPlainString());
    }

    @Test
    @DisplayName("El text d'una secció CDATA es llegeix com qualsevol altre text")
    void llegeixCdata() {
        assertEquals("Teclat amb interruptors <blaus> i retroil·luminació RGB.", productes.get(0).descripcio());
    }

    @Test
    @DisplayName("Un element opcional que no hi és es converteix en null")
    void elementOpcionalAbsent() {
        assertNull(productes.get(1).descripcio());
        assertTrue(productes.get(1).descripcioOpcional().isEmpty());
    }

    @Test
    @DisplayName("Un contenidor buit es converteix en una llista buida")
    void contenidorBuit() {
        assertTrue(productes.get(3).etiquetes().isEmpty());
    }

    @Test
    @DisplayName("Si falta l'atribut moneda, es fa servir EUR")
    void monedaPerDefecte() throws Exception {
        Document document = documentDe("<cataleg><producte id=\"X1\"><nom>Prova</nom><preu>1.50</preu><estoc>3</estoc></producte></cataleg>");
        assertEquals("EUR", LecturaDom.llegirProductes(document).get(0).moneda());
    }

    @Test
    @DisplayName("Si falta un element obligatori, l'error diu quin producte i quin element")
    void elementObligatoriAbsent() throws Exception {
        Document document = documentDe("<cataleg><producte id=\"X1\"><nom>Sense preu</nom><estoc>1</estoc></producte></cataleg>");
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> LecturaDom.llegirProductes(document));
        assertTrue(error.getMessage().contains("X1"));
        assertTrue(error.getMessage().contains("<preu>"));
    }

    @Test
    @DisplayName("primerFill només mira els fills directes, getElementsByTagName busca a tot l'arbre")
    void primerFillNomesDirectes() throws Exception {
        Document document = documentDe("<producte><fabricant><nom>Fabricant SA</nom></fabricant><nom>Teclat</nom></producte>");
        Element producte = document.getDocumentElement();
        assertEquals("Teclat", LecturaDom.primerFill(producte, "nom").getTextContent());
        assertEquals("Fabricant SA", producte.getElementsByTagName("nom").item(0).getTextContent());
        assertNull(LecturaDom.primerFill(producte, "preu"));
    }

    @Test
    @DisplayName("Compta els nodes de cada tipus")
    void comptaElsNodes() {
        Map<String, Integer> recompte = LecturaDom.comptarTipusDeNodes(cataleg);
        assertAll(
                () -> assertEquals(cataleg.getElementsByTagName("*").getLength(), recompte.get("element")),
                () -> assertEquals(1, recompte.get("document")),
                () -> assertEquals(2, recompte.get("comentari")),
                () -> assertEquals(1, recompte.get("cdata")),
                () -> assertEquals(20, recompte.get("atribut")),
                // Els salts de línia i els espais entre etiquetes també són nodes de text
                () -> assertTrue(recompte.get("text") > productes.size()));
    }

    @Test
    @DisplayName("Un XML mal format llança SAXException")
    void xmlMalFormat() {
        assertThrows(SAXException.class, () -> documentDe("<cataleg><producte></cataleg>"));
    }

    @Test
    @DisplayName("Les etiquetes d'un producte no es poden modificar des de fora")
    void etiquetesImmutables() {
        assertThrows(UnsupportedOperationException.class, () -> productes.get(0).etiquetes().add("nova"));
    }
}
