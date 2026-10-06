package com.project.exemples.xpath;

import com.project.exemples.Fluxos;
import com.project.exemples.comu.Recursos;
import com.project.exemples.comu.Xml;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Exemple 4: consultes XPath")
class ConsultesXPathTest {

    private static ConsultesXPath consultes;

    @BeforeAll
    static void carregarCataleg() throws Exception {
        try (InputStream entrada = Recursos.dades("cataleg.xml")) {
            consultes = new ConsultesXPath(Xml.llegir(entrada));
        }
    }

    @Nested
    @DisplayName("Resultats numèrics i booleans")
    class NumericsIBooleans {

        @Test
        @DisplayName("count() compta els productes")
        void comptar() {
            assertEquals(6, consultes.comptarProductes());
        }

        @Test
        @DisplayName("sum() suma les unitats en estoc")
        void sumar() {
            assertEquals(193, consultes.unitatsEnEstoc());
        }

        @Test
        @DisplayName("sum() div count() calcula el preu mitjà")
        void mitjana() {
            assertEquals(new BigDecimal("87.56"), consultes.preuMitja());
        }

        @Test
        @DisplayName("boolean() indica si hi ha productes sense estoc")
        void boolea() throws Exception {
            assertTrue(consultes.hiHaProductesSenseEstoc());
            ConsultesXPath ambEstoc = new ConsultesXPath(Xml.llegir(Fluxos.de(
                    "<cataleg><producte id=\"X1\"><estoc>3</estoc></producte></cataleg>")));
            assertFalse(ambEstoc.hiHaProductesSenseEstoc());
        }
    }

    @Nested
    @DisplayName("Consultes amb variables")
    class AmbVariables {

        @Test
        @DisplayName("Troba el nom d'un producte pel seu identificador")
        void nomPerId() {
            assertEquals(Optional.of("Monitor 27 polzades"), consultes.nomDelProducte("P003"));
            assertEquals(Optional.empty(), consultes.nomDelProducte("P999"));
        }

        @Test
        @DisplayName("La consulta concatenada és vulnerable a una injecció")
        void injeccioEnLaConsultaInsegura() {
            assertTrue(consultes.nomDelProducteInsegur("x' or '1'='1").isPresent());
        }

        @Test
        @DisplayName("La consulta amb variable tracta l'atac com un valor normal")
        void variableEvitaLaInjeccio() {
            assertEquals(Optional.empty(), consultes.nomDelProducte("x' or '1'='1"));
        }

        @Test
        @DisplayName("Un apòstrof trenca la consulta concatenada però no la de la variable")
        void apostrof() {
            assertThrows(IllegalArgumentException.class, () -> consultes.nomDelProducteInsegur("D'Artagnan"));
            assertEquals(Optional.empty(), consultes.nomDelProducte("D'Artagnan"));
        }

        @Test
        @DisplayName("Filtra pel preu amb una variable numèrica")
        void filtrarPerPreu() {
            assertEquals(List.of("P003", "P005"), consultes.productesMesCarsQue(new BigDecimal("100")));
            assertEquals(6, consultes.productesMesCarsQue(BigDecimal.ZERO).size());
            assertEquals(List.of(), consultes.productesMesCarsQue(new BigDecimal("229.00")));
        }

        @ParameterizedTest(name = "cerca \"{0}\"")
        @CsvSource(delimiter = '|', value = {
                "monitor | Monitor 27 polzades, Monitor portàtil 15 polzades",
                "MONITOR | Monitor 27 polzades, Monitor portàtil 15 polzades",
                "usb     | Memòria USB 128 GB",
                "MEMÒRIA | Memòria USB 128 GB",
                "tauleta | ''"})
        @DisplayName("Cerca dins del nom sense distingir majúscules")
        void cercarPerNom(String text, String esperats) {
            List<String> noms = esperats.isEmpty() ? List.of() : Arrays.stream(esperats.split(",")).map(String::trim).toList();
            assertEquals(noms, consultes.cercarPerNom(text));
        }
    }

    @Nested
    @DisplayName("Predicats, eixos i funcions")
    class PredicatsIEixos {

        @Test
        @DisplayName("Troba el producte més car sense ordenar")
        void mesCar() {
            assertEquals("Monitor 27 polzades", consultes.nomDelProducteMesCar());
        }

        @Test
        @DisplayName("Obté les categories sense repetir-les")
        void categories() {
            assertEquals(List.of("periferics", "monitors", "emmagatzematge"), consultes.categories());
        }

        @Test
        @DisplayName("Les expressions relatives s'avaluen des de cada producte")
        void relatives() {
            assertEquals(Map.of("P001", 2, "P002", 1, "P003", 3, "P004", 0, "P005", 1, "P006", 1),
                    consultes.etiquetesPerProducte());
            assertEquals(List.of("P001", "P002", "P003", "P004", "P005", "P006"),
                    List.copyOf(consultes.etiquetesPerProducte().keySet()));
        }

        @Test
        @DisplayName("La comparació entre conjunts de nodes troba etiquetes en comú")
        void etiquetesEnComu() {
            assertEquals(List.of("P003", "P005", "P006"), consultes.productesAmbEtiquetaComuna("P001"));
            assertEquals(List.of(), consultes.productesAmbEtiquetaComuna("P002"));
            assertEquals(List.of(), consultes.productesAmbEtiquetaComuna("P004"));
        }

        @Test
        @DisplayName("position() i last() seleccionen els últims productes")
        void ultims() {
            assertEquals(List.of("P005", "P006"), consultes.ultimsProductes(2));
            assertEquals(List.of(), consultes.ultimsProductes(0));
        }

        @Test
        @DisplayName("Avalua expressions qualssevol")
        void expressioLliure() {
            assertEquals(List.of("P002", "P005"), consultes.textosDe("/cataleg/producte[estoc = 0]/@id"));
        }

        @Test
        @DisplayName("Una expressió mal escrita dona un error que la inclou")
        void expressioInvalida() {
            IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                    () -> consultes.textosDe("/cataleg/producte[@id="));
            assertTrue(error.getMessage().contains("/cataleg/producte[@id="));
        }
    }
}
