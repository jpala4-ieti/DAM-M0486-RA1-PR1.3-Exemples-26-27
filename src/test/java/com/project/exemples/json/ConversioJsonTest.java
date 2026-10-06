package com.project.exemples.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.project.exemples.comu.Recursos;
import com.project.exemples.comu.Xml;
import com.project.exemples.dom.lectura.LecturaDom;
import com.project.exemples.jaxb.Comanda;
import com.project.exemples.jaxb.Comandes;
import com.project.exemples.jaxb.GestorComandes;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Exemple 11: conversió entre XML i JSON")
class ConversioJsonTest {

    private static Document cataleg;

    @BeforeAll
    static void llegirCataleg() throws Exception {
        try (InputStream entrada = Recursos.dades("cataleg.xml")) {
            cataleg = Xml.llegir(entrada);
        }
    }

    @Nested
    @DisplayName("Conversió genèrica d'XML a JSON")
    class XmlAJson {

        private final ObjectNode json = ConversioJson.xmlAJson(cataleg.getDocumentElement());

        @Test
        @DisplayName("Els atributs es converteixen en claus amb @")
        void atributs() {
            assertEquals("Byte & Bit", json.get("@botiga").asText());
            assertEquals("P001", json.get("producte").get(0).get("@id").asText());
        }

        @Test
        @DisplayName("Els elements repetits formen una llista")
        void elementsRepetits() {
            assertTrue(json.get("producte").isArray());
            assertEquals(6, json.get("producte").size());
        }

        @Test
        @DisplayName("Un element amb atributs i text fa servir la clau #text")
        void textAmbAtributs() {
            JsonNode preu = json.get("producte").get(0).get("preu");
            assertEquals("EUR", preu.get("@moneda").asText());
            assertEquals("49.90", preu.get("#text").asText());
        }

        @Test
        @DisplayName("Problema de la conversió genèrica: una sola etiqueta no es converteix en llista")
        void inconsistenciaDeLlistes() {
            assertTrue(json.get("producte").get(0).get("etiquetes").get("etiqueta").isArray());
            assertTrue(json.get("producte").get(1).get("etiquetes").get("etiqueta").isTextual());
        }

        @Test
        @DisplayName("Tot són textos: la conversió genèrica no sap que l'estoc és un número")
        void totSonTextos() {
            assertTrue(json.get("producte").get(0).get("estoc").isTextual());
        }

        @Test
        @DisplayName("Els comentaris es descarten i el CDATA es converteix en text")
        void comentarisICdata() {
            assertFalse(json.toString().contains("oferta"));
            assertTrue(json.get("producte").get(0).get("descripcio").asText().contains("<blaus>"));
        }
    }

    @Nested
    @DisplayName("Conversió genèrica de JSON a XML")
    class JsonAXml {

        @Test
        @DisplayName("Les claus amb @ es converteixen en atributs i les llistes en elements repetits")
        void regles() throws Exception {
            JsonNode json = ConversioJson.llegirJson("{\"@id\": \"X1\", \"nom\": \"Prova\", \"preu\": [1, 2], \"buit\": null, \"llista\": []}");
            Element arrel = ConversioJson.jsonAXml(json, "producte").getDocumentElement();
            assertEquals("X1", arrel.getAttribute("id"));
            assertEquals("Prova", arrel.getElementsByTagName("nom").item(0).getTextContent());
            assertEquals(2, arrel.getElementsByTagName("preu").getLength());
            assertEquals(0, arrel.getElementsByTagName("buit").getLength());
            assertEquals(0, arrel.getElementsByTagName("llista").getLength());
        }

        @Test
        @DisplayName("La clau #text es converteix en el text de l'element")
        void textIAtributs() throws Exception {
            JsonNode json = ConversioJson.llegirJson("{\"preu\": {\"@moneda\": \"EUR\", \"#text\": \"9.99\"}}");
            Element preu = (Element) ConversioJson.jsonAXml(json, "producte").getElementsByTagName("preu").item(0);
            assertEquals("EUR", preu.getAttribute("moneda"));
            assertEquals("9.99", preu.getTextContent());
        }

        @Test
        @DisplayName("Una llista al primer nivell es converteix en elements <item>")
        void llistaArrel() throws Exception {
            Document document = ConversioJson.jsonAXml(ConversioJson.llegirJson("[1, 2, 3]"), "valors");
            assertEquals(3, document.getElementsByTagName("item").getLength());
        }

        @Test
        @DisplayName("Convertir el catàleg a JSON i tornar a XML conserva les dades dels productes")
        void anadaITornada() {
            Document reconstruit = ConversioJson.jsonAXml(ConversioJson.xmlAJson(cataleg.getDocumentElement()), "cataleg");
            assertEquals(LecturaDom.llegirProductes(cataleg), LecturaDom.llegirProductes(reconstruit));
        }

        @Test
        @DisplayName("El JSON d'una API es converteix en XML (els decimals de JSON no conserven els zeros)")
        void jsonDUnaApi() throws Exception {
            String text;
            try (InputStream entrada = Recursos.dades("productes-api.json")) {
                text = new String(entrada.readAllBytes(), StandardCharsets.UTF_8);
            }
            Document document = ConversioJson.jsonAXml(ConversioJson.llegirJson(text), "importacio");
            assertEquals(2, document.getElementsByTagName("productes").getLength());
            assertEquals(2, document.getElementsByTagName("etiquetes").getLength());
            assertEquals("35.5", document.getElementsByTagName("preu").item(0).getTextContent());
            assertEquals(0, document.getElementsByTagName("descatalogat").getLength());
        }
    }

    @Nested
    @DisplayName("Conversió a través d'objectes")
    class AmbObjectes {

        private static Comandes comandes;

        @BeforeAll
        static void llegirComandes() throws Exception {
            try (InputStream entrada = Recursos.dades("comandes.xml")) {
                comandes = new GestorComandes().llegir(entrada);
            }
        }

        @Test
        @DisplayName("Les dates surten en format ISO i els números com a números")
        void format() throws Exception {
            JsonNode json = ConversioJson.llegirJson(ConversioJson.comandesAJson(comandes));
            JsonNode primera = json.get("comandes").get(0);
            assertEquals(3, json.get("comandes").size());
            assertEquals("2026-09-01", primera.get("data").asText());
            assertTrue(primera.get("linies").get(0).get("quantitat").isNumber());
            assertEquals(0, new BigDecimal("85.90").compareTo(primera.get("total").decimalValue()));
        }

        @Test
        @DisplayName("Les llistes són sempre llistes, encara que tinguin un sol element")
        void llistesConsistents() throws Exception {
            JsonNode json = ConversioJson.llegirJson(ConversioJson.comandesAJson(comandes));
            assertTrue(json.get("comandes").get(1).get("linies").isArray());
            assertEquals(1, json.get("comandes").get(1).get("linies").size());
        }

        @Test
        @DisplayName("JSON → objectes recupera les mateixes dades (ignorant els camps calculats)")
        void anadaITornada() throws Exception {
            Comandes llegides = ConversioJson.comandesDeJson(ConversioJson.comandesAJson(comandes));
            assertEquals(comandes.getComandes().stream().map(Comanda::getId).toList(),
                    llegides.getComandes().stream().map(Comanda::getId).toList());
            assertEquals(0, GestorComandes.totalFacturat(comandes).compareTo(GestorComandes.totalFacturat(llegides)));
            assertEquals(comandes.getComandes().get(2).getData(), llegides.getComandes().get(2).getData());
        }

        @Test
        @DisplayName("Un JSON mal format llança JsonProcessingException")
        void jsonInvalid() {
            assertThrows(JsonProcessingException.class, () -> ConversioJson.comandesDeJson("{\"comandes\": ["));
        }
    }

    @Test
    @DisplayName("El text JSON surt indentat")
    void indentat() throws Exception {
        String text = ConversioJson.aText(ConversioJson.llegirJson("{\"a\": [1, 2]}"));
        assertTrue(text.lines().count() > 1);
        assertEquals(List.of(1, 2), List.of(ConversioJson.llegirJson(text).get("a").get(0).asInt(),
                ConversioJson.llegirJson(text).get("a").get(1).asInt()));
    }
}
