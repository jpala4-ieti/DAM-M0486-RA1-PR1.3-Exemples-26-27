package com.project.exemples.stax;

import com.project.exemples.Fluxos;
import com.project.exemples.comu.Recursos;
import com.project.exemples.comu.Xml;
import com.project.exemples.stax.StreamingStax.Resum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.stream.XMLStreamException;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Exemple 6: streaming amb StAX")
class StreamingStaxTest {

    @TempDir
    Path temporal;

    private static List<String> ids(Document document) {
        NodeList productes = document.getElementsByTagName("producte");
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < productes.getLength(); i++) {
            ids.add(((Element) productes.item(i)).getAttribute("id"));
        }
        return ids;
    }

    @Test
    @DisplayName("Genera un catàleg ben format amb els productes demanats")
    void generar() throws Exception {
        Path fitxer = temporal.resolve("generat.xml");
        StreamingStax.generarCataleg(fitxer, 4);
        Document document;
        try (InputStream entrada = Files.newInputStream(fitxer)) {
            document = Xml.llegir(entrada);
        }
        assertEquals(List.of("G000001", "G000002", "G000003", "G000004"), ids(document));
        assertEquals("Byte & Bit", document.getDocumentElement().getAttribute("botiga"));
        assertTrue(Files.readString(fitxer).contains("Byte &amp; Bit"), "El & s'hauria d'haver escapat");
    }

    @ParameterizedTest(name = "producte {0} → {1}")
    @CsvSource({"1, 1.99", "100, 0.99", "199, 99.99"})
    @DisplayName("Els preus generats van de 0,99 a 99,99")
    void preusGenerats(int numero, String preu) {
        assertEquals(new BigDecimal(preu), StreamingStax.preuGenerat(numero));
    }

    @Test
    @DisplayName("Resumeix el catàleg de mostra")
    void resumirMostra() throws Exception {
        try (InputStream entrada = Recursos.dades("cataleg.xml")) {
            assertEquals(new Resum(6, 193, Map.of("emmagatzematge", 2, "monitors", 2, "periferics", 2)),
                    StreamingStax.resumir(entrada));
        }
    }

    @Test
    @DisplayName("Resumeix un catàleg gran sense carregar-lo en memòria")
    void resumirGran() throws Exception {
        int nombre = 50_000;
        Path fitxer = temporal.resolve("gran.xml");
        StreamingStax.generarCataleg(fitxer, nombre);

        long unitatsEsperades = 0;
        Map<String, Integer> categoriesEsperades = new TreeMap<>();
        String[] categories = {"periferics", "monitors", "emmagatzematge"};
        for (int i = 1; i <= nombre; i++) {
            unitatsEsperades += i % 7;
            categoriesEsperades.merge(categories[i % 3], 1, Integer::sum);
        }
        try (InputStream entrada = Files.newInputStream(fitxer)) {
            assertEquals(new Resum(nombre, unitatsEsperades, categoriesEsperades), StreamingStax.resumir(entrada));
        }
    }

    @Test
    @DisplayName("Filtra els productes d'una categoria i conserva l'element arrel")
    void filtrar() throws Exception {
        ByteArrayOutputStream sortida = new ByteArrayOutputStream();
        try (InputStream entrada = Recursos.dades("cataleg.xml")) {
            assertEquals(2, StreamingStax.filtrarPerCategoria(entrada, sortida, "monitors"));
        }
        Document resultat = Xml.llegir(Fluxos.de(sortida.toString(StandardCharsets.UTF_8)));
        assertEquals(List.of("P003", "P005"), ids(resultat));
        assertEquals("Byte & Bit", resultat.getDocumentElement().getAttribute("botiga"));
        assertEquals("2026-09-15", resultat.getDocumentElement().getAttribute("actualitzat"));
    }

    @Test
    @DisplayName("Els productes filtrats es copien sencers")
    void filtratsSencers() throws Exception {
        ByteArrayOutputStream sortida = new ByteArrayOutputStream();
        try (InputStream entrada = Recursos.dades("cataleg.xml")) {
            StreamingStax.filtrarPerCategoria(entrada, sortida, "periferics");
        }
        String text = sortida.toString(StandardCharsets.UTF_8);
        assertTrue(text.contains("<etiqueta>gaming</etiqueta>"));
        assertFalse(text.contains("Producte en oferta"), "Els comentaris de fora dels productes es descarten");
        // El CDATA arriba com a text normal i s'escriu escapat, però el contingut és el mateix
        Document resultat = Xml.llegir(Fluxos.de(text));
        assertTrue(resultat.getElementsByTagName("descripcio").item(0).getTextContent().contains("<blaus>"));
    }

    @Test
    @DisplayName("Si cap producte és de la categoria, el resultat és un catàleg buit però vàlid")
    void filtrarSenseResultats() throws Exception {
        ByteArrayOutputStream sortida = new ByteArrayOutputStream();
        try (InputStream entrada = Recursos.dades("cataleg.xml")) {
            assertEquals(0, StreamingStax.filtrarPerCategoria(entrada, sortida, "impressores"));
        }
        Document resultat = Xml.llegir(Fluxos.de(sortida.toString(StandardCharsets.UTF_8)));
        assertEquals("cataleg", resultat.getDocumentElement().getTagName());
        assertEquals(List.of(), ids(resultat));
    }

    @Test
    @DisplayName("Un producte sense categoria es compta a part")
    void senseCategoria() throws Exception {
        Resum resum = StreamingStax.resumir(Fluxos.de("<cataleg><producte><estoc>2</estoc></producte></cataleg>"));
        assertEquals(Map.of(StreamingStax.SENSE_CATEGORIA, 1), resum.productesPerCategoria());
        assertEquals(2, resum.unitats());
    }

    @Test
    @DisplayName("Un XML mal format llança XMLStreamException")
    void malFormat() {
        assertThrows(XMLStreamException.class, () -> StreamingStax.resumir(Fluxos.de("<cataleg><producte></cataleg>")));
    }
}
