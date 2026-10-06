package com.project.exemples.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.project.exemples.comu.Recursos;
import com.project.exemples.comu.Sortida;
import com.project.exemples.comu.Xml;
import com.project.exemples.jaxb.Comandes;
import com.project.exemples.jaxb.GestorComandes;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Exemple 11. Conversió entre XML i JSON, el format de la majoria d'API web.
 *
 * <p>Hi ha dues maneres de fer-ho:</p>
 * <ol>
 *     <li><b>Conversió genèrica</b> (node a node): funciona amb qualsevol XML, però el resultat
 *     depèn de les dades. Per exemple, un element que es repeteix es converteix en una llista,
 *     però si només n'hi ha un, no.</li>
 *     <li><b>Conversió a través d'objectes</b>: XML → objectes Java (JAXB) → JSON (Jackson).
 *     El format és sempre el mateix. És la que es fa servir a les aplicacions reals.</li>
 * </ol>
 */
public final class ConversioJson {

    /** Un ObjectMapper configurat es pot reutilitzar i compartir entre fils. */
    private static final ObjectMapper JSON = JsonMapper.builder()
            // Suport per a LocalDate i la resta de tipus de java.time
            .addModule(new JavaTimeModule())
            // Les dates com a text ISO ("2026-09-01") i no com a llista de números
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            // Ignora els camps calculats (total, import) quan es tornen a llegir
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(SerializationFeature.INDENT_OUTPUT)
            .build();

    private ConversioJson() {
    }

    /**
     * Conversió genèrica d'un element XML a JSON. Regles:
     * <ul>
     *     <li>cada atribut es converteix en una clau que comença per {@code @};</li>
     *     <li>un element que només té text es converteix en un valor de text;</li>
     *     <li>els elements fills que es repeteixen formen una llista;</li>
     *     <li>el text d'un element que també té atributs o fills va a la clau {@code #text}.</li>
     * </ul>
     *
     * @param element element que es vol convertir
     * @return objecte JSON equivalent
     */
    public static ObjectNode xmlAJson(Element element) {
        ObjectNode objecte = JSON.createObjectNode();
        NamedNodeMap atributs = element.getAttributes();
        for (int i = 0; i < atributs.getLength(); i++) {
            Node atribut = atributs.item(i);
            objecte.put("@" + atribut.getNodeName(), atribut.getNodeValue());
        }
        // S'agrupen els fills pel nom, conservant l'ordre en què apareixen
        Map<String, List<Element>> fills = new LinkedHashMap<>();
        StringBuilder text = new StringBuilder();
        for (Node fill = element.getFirstChild(); fill != null; fill = fill.getNextSibling()) {
            switch (fill.getNodeType()) {
                case Node.ELEMENT_NODE -> fills.computeIfAbsent(fill.getNodeName(), nom -> new ArrayList<>()).add((Element) fill);
                case Node.TEXT_NODE, Node.CDATA_SECTION_NODE -> text.append(fill.getNodeValue());
                default -> {
                    // Els comentaris i les instruccions de processament no tenen equivalent en JSON
                }
            }
        }
        for (Map.Entry<String, List<Element>> grup : fills.entrySet()) {
            List<Element> elements = grup.getValue();
            if (elements.size() == 1) {
                objecte.set(grup.getKey(), valorDe(elements.get(0)));
            } else {
                ArrayNode llista = objecte.putArray(grup.getKey());
                elements.forEach(fill -> llista.add(valorDe(fill)));
            }
        }
        String textNet = text.toString().trim();
        if (!textNet.isEmpty()) {
            objecte.put("#text", textNet);
        }
        return objecte;
    }

    private static JsonNode valorDe(Element element) {
        if (!element.hasAttributes() && !teFillsElement(element)) {
            return TextNode.valueOf(element.getTextContent().trim());
        }
        return xmlAJson(element);
    }

    private static boolean teFillsElement(Element element) {
        for (Node fill = element.getFirstChild(); fill != null; fill = fill.getNextSibling()) {
            if (fill.getNodeType() == Node.ELEMENT_NODE) {
                return true;
            }
        }
        return false;
    }

    /**
     * Conversió genèrica de JSON a XML, amb les regles inverses de {@link #xmlAJson(Element)}.
     * Les llistes es converteixen en elements repetits amb el nom de la clau, i els valors
     * {@code null} i les llistes buides es perden (no tenen equivalent en XML).
     *
     * @param json      valor JSON
     * @param nomArrel  nom de l'element arrel
     * @return document XML
     */
    public static Document jsonAXml(JsonNode json, String nomArrel) {
        Document document = Xml.documentNou();
        Element arrel = document.createElement(nomArrel);
        document.appendChild(arrel);
        omplir(document, arrel, json);
        return document;
    }

    private static void omplir(Document document, Element element, JsonNode valor) {
        if (valor.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> camps = valor.fields();
            while (camps.hasNext()) {
                Map.Entry<String, JsonNode> camp = camps.next();
                afegirCamp(document, element, camp.getKey(), camp.getValue());
            }
        } else if (valor.isArray()) {
            for (JsonNode item : valor) {
                Element fill = document.createElement("item");
                element.appendChild(fill);
                omplir(document, fill, item);
            }
        } else if (!valor.isNull()) {
            element.setTextContent(valor.asText());
        }
    }

    private static void afegirCamp(Document document, Element element, String nom, JsonNode valor) {
        if (valor.isNull()) {
            return;
        }
        if (nom.startsWith("@")) {
            element.setAttribute(nom.substring(1), valor.asText());
        } else if (nom.equals("#text")) {
            element.appendChild(document.createTextNode(valor.asText()));
        } else if (valor.isArray()) {
            for (JsonNode item : valor) {
                Element fill = document.createElement(nom);
                element.appendChild(fill);
                omplir(document, fill, item);
            }
        } else {
            Element fill = document.createElement(nom);
            element.appendChild(fill);
            omplir(document, fill, valor);
        }
    }

    /**
     * Converteix un valor JSON en text indentat.
     *
     * @param json valor JSON
     * @return text JSON
     */
    public static String aText(JsonNode json) {
        try {
            return JSON.writeValueAsString(json);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Llegeix un text JSON.
     *
     * @param text text JSON
     * @return arbre JSON
     * @throws JsonProcessingException si el text no és JSON vàlid
     */
    public static JsonNode llegirJson(String text) throws JsonProcessingException {
        return JSON.readTree(text);
    }

    /**
     * Conversió a través d'objectes: les comandes llegides amb JAXB es converteixen en JSON
     * amb Jackson. El format no depèn de quantes línies tingui cada comanda.
     *
     * @param comandes comandes (per exemple, llegides d'un XML amb JAXB)
     * @return text JSON
     * @throws JsonProcessingException si no es poden convertir
     */
    public static String comandesAJson(Comandes comandes) throws JsonProcessingException {
        return JSON.writeValueAsString(comandes);
    }

    /**
     * Operació inversa: JSON → objectes. Després es poden escriure en XML amb JAXB.
     *
     * @param json text JSON
     * @return comandes
     * @throws JsonProcessingException si el JSON no és vàlid o no correspon a les classes
     */
    public static Comandes comandesDeJson(String json) throws JsonProcessingException {
        return JSON.readValue(json, Comandes.class);
    }

    /**
     * Fa les tres conversions amb les dades de mostra.
     *
     * @param args no es fan servir
     * @throws Exception si falla alguna conversió
     */
    public static void main(String[] args) throws Exception {
        Document cataleg;
        try (InputStream entrada = Recursos.dades("cataleg.xml")) {
            cataleg = Xml.llegir(entrada);
        }
        System.out.println("1. Catàleg XML → JSON (conversió genèrica):");
        System.out.println(aText(xmlAJson(cataleg.getDocumentElement())));

        Comandes comandes;
        try (InputStream entrada = Recursos.dades("comandes.xml")) {
            comandes = new GestorComandes().llegir(entrada);
        }
        Path fitxerJson = Sortida.fitxer("comandes.json");
        Files.writeString(fitxerJson, comandesAJson(comandes));
        System.out.println("\n2. Comandes XML → objectes → JSON, desat a " + fitxerJson.toAbsolutePath() + ":");
        System.out.println(Files.readString(fitxerJson));

        String api;
        try (InputStream entrada = Recursos.dades("productes-api.json")) {
            api = new String(entrada.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        }
        Document importacio = jsonAXml(llegirJson(api), "importacio");
        Path fitxerXml = Sortida.fitxer("productes-api.xml");
        Xml.desar(importacio, fitxerXml);
        System.out.println("3. JSON d'una API → XML (conversió genèrica), desat a " + fitxerXml.toAbsolutePath() + ":");
        System.out.println(Xml.aText(importacio));
    }

    /**
     * Per si cal llegir un fitxer JSON des del disc.
     *
     * @param fitxer fitxer JSON
     * @return arbre JSON
     * @throws IOException si no es pot llegir o no és JSON vàlid
     */
    public static JsonNode llegirJson(Path fitxer) throws IOException {
        return JSON.readTree(fitxer.toFile());
    }
}
