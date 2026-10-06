package com.project.exemples.dom.modificacio;

import com.project.exemples.comu.Recursos;
import com.project.exemples.dom.lectura.LecturaDom;
import com.project.exemples.model.Producte;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Exemple 3: modificació d'un document amb DOM")
class ModificacioDomTest {

    @TempDir
    Path temporal;

    private Document document;

    @BeforeEach
    void carregarCataleg() throws Exception {
        try (InputStream entrada = Recursos.dades("cataleg.xml")) {
            document = LecturaDom.carregar(entrada);
        }
    }

    private Element producte(String id) {
        NodeList productes = document.getElementsByTagName("producte");
        for (int i = 0; i < productes.getLength(); i++) {
            Element producte = (Element) productes.item(i);
            if (producte.getAttribute("id").equals(id)) {
                return producte;
            }
        }
        throw new AssertionError("No s'ha trobat el producte " + id);
    }

    private Element preu(String id) {
        return (Element) producte(id).getElementsByTagName("preu").item(0);
    }

    @Test
    @DisplayName("El descompte canvia el preu i hi afegeix un atribut")
    void descompte() {
        assertEquals(2, ModificacioDom.aplicarDescompte(document, "monitors", new BigDecimal("10")));
        assertEquals("206.10", preu("P003").getTextContent());
        assertEquals("125.99", preu("P005").getTextContent());
        assertEquals("10%", preu("P003").getAttribute("descompte"));
    }

    @Test
    @DisplayName("El preu rebaixat s'arrodoneix a dos decimals (mig cap amunt)")
    void arrodoniment() {
        ModificacioDom.aplicarDescompte(document, "periferics", new BigDecimal("10"));
        assertEquals("17.96", preu("P002").getTextContent());
        assertEquals("44.91", preu("P001").getTextContent());
    }

    @Test
    @DisplayName("Un percentatge amb decimals es mostra sense zeros sobrants")
    void percentatgeAmbDecimals() {
        ModificacioDom.aplicarDescompte(document, "monitors", new BigDecimal("12.50"));
        assertEquals("12.5%", preu("P003").getAttribute("descompte"));
        assertEquals("200.38", preu("P003").getTextContent());
    }

    @Test
    @DisplayName("Els productes d'altres categories no canvien")
    void altresCategories() {
        ModificacioDom.aplicarDescompte(document, "monitors", new BigDecimal("10"));
        assertEquals("49.90", preu("P001").getTextContent());
        assertFalse(preu("P001").hasAttribute("descompte"));
    }

    @ParameterizedTest(name = "percentatge {0}")
    @ValueSource(strings = {"-5", "100.01", "150"})
    @DisplayName("Un percentatge fora de l'interval 0-100 no s'accepta")
    void percentatgeInvalid(String percentatge) {
        assertThrows(IllegalArgumentException.class,
                () -> ModificacioDom.aplicarDescompte(document, "monitors", new BigDecimal(percentatge)));
    }

    @Test
    @DisplayName("Marca com a no disponibles els productes sense estoc")
    void marcarSenseEstoc() {
        assertEquals(2, ModificacioDom.marcarDisponibilitat(document));
        assertEquals("false", producte("P002").getAttribute("disponible"));
        assertEquals("false", producte("P005").getAttribute("disponible"));
        assertFalse(producte("P001").hasAttribute("disponible"));
    }

    @Test
    @DisplayName("Treu la marca quan el producte torna a tenir estoc")
    void treureLaMarca() {
        ModificacioDom.marcarDisponibilitat(document);
        producte("P002").getElementsByTagName("estoc").item(0).setTextContent("5");
        assertEquals(1, ModificacioDom.marcarDisponibilitat(document));
        assertFalse(producte("P002").hasAttribute("disponible"));
    }

    @Test
    @DisplayName("Reanomena els elements i en conserva el contingut")
    void reanomenar() {
        assertEquals(2, ModificacioDom.reanomenarElements(document, "descripcio", "resum"));
        assertEquals(0, document.getElementsByTagName("descripcio").getLength());
        NodeList resums = document.getElementsByTagName("resum");
        assertEquals(2, resums.getLength());
        assertTrue(resums.item(0).getTextContent().contains("<blaus>"));
    }

    @Test
    @DisplayName("Ordena els productes del més barat al més car")
    void ordenar() {
        ModificacioDom.ordenarPerPreu(document);
        List<String> ids = LecturaDom.llegirProductes(document).stream().map(Producte::id).toList();
        assertEquals(List.of("P006", "P002", "P001", "P004", "P005", "P003"), ids);
    }

    @Test
    @DisplayName("Elimina tots els comentaris, també els de fora de l'element arrel")
    void eliminarComentaris() {
        assertEquals(2, ModificacioDom.eliminarComentaris(document));
        assertNull(LecturaDom.comptarTipusDeNodes(document).get("comentari"));
    }

    @Test
    @DisplayName("Elimina els nodes de text que només són espais i conserva la resta")
    void netejarEspais() {
        ModificacioDom.netejarEspaisEnBlanc(document);
        assertFalse(hiHaTextBuit(document));
        assertEquals("Teclat mecànic", producte("P001").getElementsByTagName("nom").item(0).getTextContent());
    }

    private static boolean hiHaTextBuit(Node node) {
        for (Node fill = node.getFirstChild(); fill != null; fill = fill.getNextSibling()) {
            if (fill.getNodeType() == Node.TEXT_NODE && fill.getTextContent().isBlank()) {
                return true;
            }
            if (hiHaTextBuit(fill)) {
                return true;
            }
        }
        return false;
    }

    @Test
    @DisplayName("El fitxer desat no té línies en blanc i es pot tornar a llegir")
    void desarSenseLiniesEnBlanc() throws Exception {
        ModificacioDom.aplicarDescompte(document, "monitors", new BigDecimal("10"));
        Path fitxer = temporal.resolve("cataleg-modificat.xml");
        ModificacioDom.desar(document, fitxer);
        List<String> linies = Files.readAllLines(fitxer);
        assertTrue(linies.stream().noneMatch(String::isBlank), "Hi ha línies en blanc");
        try (InputStream entrada = Files.newInputStream(fitxer)) {
            assertEquals(6, LecturaDom.llegirProductes(LecturaDom.carregar(entrada)).size());
        }
    }
}
