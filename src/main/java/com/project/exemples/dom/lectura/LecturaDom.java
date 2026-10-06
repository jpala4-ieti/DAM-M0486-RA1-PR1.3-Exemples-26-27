package com.project.exemples.dom.lectura;

import com.project.exemples.comu.Recursos;
import com.project.exemples.model.Producte;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Exemple 1. Lectura d'un document XML amb DOM i conversió a objectes Java.
 *
 * <p>DOM carrega tot el document en memòria com un arbre de nodes. És la tècnica més còmoda
 * quan el fitxer és petit o mitjà i cal consultar-lo o modificar-lo.</p>
 */
public final class LecturaDom {

    private LecturaDom() {
    }

    /**
     * Llegeix un document XML amb DOM.
     *
     * @param entrada flux amb l'XML (no es tanca)
     * @return document carregat en memòria
     * @throws ParserConfigurationException si no es pot crear el parser
     * @throws SAXException                 si l'XML no està ben format
     * @throws IOException                  si no es pot llegir el flux
     */
    public static Document carregar(InputStream entrada) throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory fabrica = DocumentBuilderFactory.newInstance();
        DocumentBuilder constructor = fabrica.newDocumentBuilder();
        Document document = constructor.parse(entrada);
        // Uneix els nodes de text adjacents i elimina els que són buits
        document.getDocumentElement().normalize();
        return document;
    }

    /**
     * Converteix cada element {@code <producte>} del document en un objecte {@link Producte}.
     *
     * @param document document amb un catàleg
     * @return llista de productes en l'ordre del document
     */
    public static List<Producte> llegirProductes(Document document) {
        List<Producte> productes = new ArrayList<>();
        NodeList nodes = document.getElementsByTagName("producte");
        for (int i = 0; i < nodes.getLength(); i++) {
            productes.add(convertir((Element) nodes.item(i)));
        }
        return productes;
    }

    /**
     * Converteix un element {@code <producte>} en un objecte.
     *
     * @param producte element del producte
     * @return objecte amb les dades de l'element
     */
    static Producte convertir(Element producte) {
        Element preu = fillObligatori(producte, "preu");
        return new Producte(
                producte.getAttribute("id"),
                producte.getAttribute("categoria"),
                fillObligatori(producte, "nom").getTextContent().trim(),
                new BigDecimal(preu.getTextContent().trim()),
                // getAttribute retorna "" si l'atribut no existeix: per això es comprova abans
                preu.hasAttribute("moneda") ? preu.getAttribute("moneda") : "EUR",
                Integer.parseInt(fillObligatori(producte, "estoc").getTextContent().trim()),
                llegirEtiquetes(producte),
                textOpcional(producte, "descripcio"));
    }

    /**
     * Retorna el primer fill <b>directe</b> amb aquest nom. A diferència de
     * {@code getElementsByTagName}, no busca en els descendents més profunds.
     *
     * @param pare element on es busca
     * @param nom  nom de l'element fill
     * @return l'element trobat, o {@code null} si no n'hi ha cap
     */
    static Element primerFill(Element pare, String nom) {
        for (Node fill = pare.getFirstChild(); fill != null; fill = fill.getNextSibling()) {
            if (fill.getNodeType() == Node.ELEMENT_NODE && fill.getNodeName().equals(nom)) {
                return (Element) fill;
            }
        }
        return null;
    }

    private static Element fillObligatori(Element pare, String nom) {
        Element fill = primerFill(pare, nom);
        if (fill == null) {
            throw new IllegalArgumentException(
                    "Al producte " + pare.getAttribute("id") + " li falta l'element <" + nom + ">");
        }
        return fill;
    }

    private static String textOpcional(Element pare, String nom) {
        Element fill = primerFill(pare, nom);
        return fill == null ? null : fill.getTextContent().trim();
    }

    private static List<String> llegirEtiquetes(Element producte) {
        List<String> etiquetes = new ArrayList<>();
        Element contenidor = primerFill(producte, "etiquetes");
        if (contenidor != null) {
            NodeList nodes = contenidor.getElementsByTagName("etiqueta");
            for (int i = 0; i < nodes.getLength(); i++) {
                etiquetes.add(nodes.item(i).getTextContent().trim());
            }
        }
        return etiquetes;
    }

    /**
     * Recorre tot l'arbre i compta quants nodes hi ha de cada tipus. Serveix per veure que un
     * document DOM no només té elements: els salts de línia són nodes de text, els comentaris
     * són nodes de comentari, etc.
     *
     * @param node node des d'on es comença (normalment, el document)
     * @return recompte per tipus de node, ordenat alfabèticament
     */
    public static Map<String, Integer> comptarTipusDeNodes(Node node) {
        Map<String, Integer> recompte = new TreeMap<>();
        comptar(node, recompte);
        return recompte;
    }

    private static void comptar(Node node, Map<String, Integer> recompte) {
        recompte.merge(nomDelTipus(node.getNodeType()), 1, Integer::sum);
        // Els atributs no són fills de l'element: s'hi accedeix amb getAttributes()
        NamedNodeMap atributs = node.getAttributes();
        if (atributs != null && atributs.getLength() > 0) {
            recompte.merge("atribut", atributs.getLength(), Integer::sum);
        }
        for (Node fill = node.getFirstChild(); fill != null; fill = fill.getNextSibling()) {
            comptar(fill, recompte);
        }
    }

    static String nomDelTipus(short tipus) {
        return switch (tipus) {
            case Node.DOCUMENT_NODE -> "document";
            case Node.ELEMENT_NODE -> "element";
            case Node.TEXT_NODE -> "text";
            case Node.CDATA_SECTION_NODE -> "cdata";
            case Node.COMMENT_NODE -> "comentari";
            case Node.PROCESSING_INSTRUCTION_NODE -> "instruccio";
            default -> "altres";
        };
    }

    /**
     * Llegeix el catàleg de mostra i en mostra el contingut.
     *
     * @param args no es fan servir
     * @throws Exception si no es pot llegir el catàleg
     */
    public static void main(String[] args) throws Exception {
        try (InputStream entrada = Recursos.dades("cataleg.xml")) {
            Document document = carregar(entrada);
            Element arrel = document.getDocumentElement();
            System.out.println("Catàleg de " + arrel.getAttribute("botiga")
                    + " (actualitzat el " + arrel.getAttribute("actualitzat") + ")");
            for (Producte producte : llegirProductes(document)) {
                System.out.println("- " + producte.id() + " " + producte.nom() + ": " + producte.preu() + " "
                        + producte.moneda() + ", " + producte.estoc() + " unitats, etiquetes " + producte.etiquetes());
                producte.descripcioOpcional().ifPresent(text -> System.out.println("    " + text));
            }
            System.out.println("Nodes de l'arbre per tipus: " + comptarTipusDeNodes(document));
        }
    }
}
