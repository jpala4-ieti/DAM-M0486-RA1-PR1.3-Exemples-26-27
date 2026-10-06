package com.project.exemples.sax;

import com.project.exemples.Fluxos;
import com.project.exemples.comu.Recursos;
import com.project.exemples.sax.EstadistiquesSax.Estadistiques;
import com.project.exemples.sax.EstadistiquesSax.ResultatCerca;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXParseException;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Exemple 5: lectura amb SAX")
class EstadistiquesSaxTest {

    /** Elements del catàleg de mostra, comptant l'arrel. */
    private static final int ELEMENTS_DEL_CATALEG = 41;

    private static Estadistiques analitzarCataleg() throws Exception {
        try (InputStream entrada = Recursos.dades("cataleg.xml")) {
            return EstadistiquesSax.analitzar(entrada);
        }
    }

    @Test
    @DisplayName("Calcula totes les estadístiques en una sola passada")
    void estadistiques() throws Exception {
        Estadistiques estadistiques = analitzarCataleg();
        assertAll(
                () -> assertEquals(6, estadistiques.productes()),
                () -> assertEquals(193, estadistiques.unitatsEstoc()),
                () -> assertEquals("Monitor 27 polzades", estadistiques.producteMesCar()),
                () -> assertEquals(new BigDecimal("87.56"), estadistiques.preuMitja()));
    }

    @Test
    @DisplayName("Compta els productes de cada categoria en ordre d'aparició")
    void perCategoria() throws Exception {
        Map<String, Integer> perCategoria = analitzarCataleg().productesPerCategoria();
        assertEquals(Map.of("periferics", 2, "monitors", 2, "emmagatzematge", 2), perCategoria);
        assertEquals(List.of("periferics", "monitors", "emmagatzematge"), List.copyOf(perCategoria.keySet()));
    }

    @Test
    @DisplayName("Un catàleg buit dona estadístiques a zero")
    void catalegBuit() throws Exception {
        Estadistiques estadistiques = EstadistiquesSax.analitzar(Fluxos.de("<cataleg/>"));
        assertAll(
                () -> assertEquals(0, estadistiques.productes()),
                () -> assertTrue(estadistiques.productesPerCategoria().isEmpty()),
                () -> assertNull(estadistiques.producteMesCar()),
                () -> assertEquals(new BigDecimal("0.00"), estadistiques.preuMitja()));
    }

    @Test
    @DisplayName("Un producte sense categoria es compta a part")
    void senseCategoria() throws Exception {
        String xml = "<cataleg><producte id=\"X1\"><nom>Prova</nom><preu>1.00</preu><estoc>1</estoc></producte></cataleg>";
        assertEquals(Map.of(EstadistiquesSax.SENSE_CATEGORIA, 1), EstadistiquesSax.analitzar(Fluxos.de(xml)).productesPerCategoria());
    }

    @Test
    @DisplayName("Ajunta el text que arriba en diversos trossos")
    void textEnTrossos() throws Exception {
        // L'entitat &amp; fa que el parser enviï el text del nom en més d'una crida a characters()
        String xml = "<cataleg><producte id=\"X1\" categoria=\"c\"><nom>Ratolí &amp; alfombreta</nom>"
                + "<preu>9.99</preu><estoc>1</estoc></producte></cataleg>";
        assertEquals("Ratolí & alfombreta", EstadistiquesSax.analitzar(Fluxos.de(xml)).producteMesCar());
    }

    @Test
    @DisplayName("Atura la lectura tan bon punt troba el producte")
    void cercaAturaLaLectura() throws Exception {
        try (InputStream entrada = Recursos.dades("cataleg.xml")) {
            ResultatCerca cerca = EstadistiquesSax.cercarNom(entrada, "P004");
            assertTrue(cerca.trobat());
            assertEquals("Disc SSD 1 TB", cerca.nom());
            assertTrue(cerca.elementsLlegits() < ELEMENTS_DEL_CATALEG);
        }
    }

    @Test
    @DisplayName("Si no troba el producte, llegeix tot el fitxer")
    void cercaSenseResultat() throws Exception {
        try (InputStream entrada = Recursos.dades("cataleg.xml")) {
            ResultatCerca cerca = EstadistiquesSax.cercarNom(entrada, "P999");
            assertFalse(cerca.trobat());
            assertEquals(ELEMENTS_DEL_CATALEG, cerca.elementsLlegits());
        }
    }

    @Test
    @DisplayName("Un XML mal format llança SAXParseException")
    void malFormat() {
        assertThrows(SAXParseException.class, () -> EstadistiquesSax.analitzar(Fluxos.de("<cataleg><producte></cataleg>")));
    }

    @Test
    @DisplayName("El parser rebutja els documents amb DOCTYPE")
    void rebutjaDoctype() {
        String xml = "<!DOCTYPE cataleg [<!ENTITY x \"y\">]><cataleg>&x;</cataleg>";
        assertThrows(SAXParseException.class, () -> EstadistiquesSax.analitzar(Fluxos.de(xml)));
    }
}
