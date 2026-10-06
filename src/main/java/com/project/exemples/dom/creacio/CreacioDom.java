package com.project.exemples.dom.creacio;

import com.project.exemples.comu.Sortida;
import com.project.exemples.model.Client;
import com.project.exemples.model.Comanda;
import com.project.exemples.model.LiniaComanda;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

/**
 * Exemple 2. Creació d'un document XML des de zero amb DOM i escriptura amb Transformer.
 *
 * <p>Es crea una comanda amb elements, atributs, un comentari i una secció CDATA.
 * El resultat compleix l'esquema {@code comanda.xsd} de l'exemple 9.</p>
 */
public final class CreacioDom {

    private CreacioDom() {
    }

    /**
     * Construeix el document XML d'una comanda.
     *
     * @param comanda dades de la comanda
     * @return document nou
     * @throws ParserConfigurationException si no es pot crear el constructor de documents
     */
    public static Document crearComanda(Comanda comanda) throws ParserConfigurationException {
        DocumentBuilderFactory fabrica = DocumentBuilderFactory.newInstance();
        Document document = fabrica.newDocumentBuilder().newDocument();

        Element arrel = document.createElement("comanda");
        arrel.setAttribute("id", comanda.id());
        // LocalDate.toString() ja fa servir el format ISO (aaaa-mm-dd) que espera l'XML Schema
        arrel.setAttribute("data", comanda.data().toString());
        document.appendChild(arrel);

        // Els comentaris també són nodes: es creen i s'afegeixen com qualsevol altre
        arrel.appendChild(document.createComment(" Comanda generada amb DOM "));

        Element client = document.createElement("client");
        afegirElementAmbText(document, client, "nom", comanda.client().nom());
        afegirElementAmbText(document, client, "email", comanda.client().email());
        arrel.appendChild(client);

        Element linies = document.createElement("linies");
        for (LiniaComanda linia : comanda.linies()) {
            Element element = document.createElement("linia");
            element.setAttribute("producte", linia.producte());
            element.setAttribute("quantitat", String.valueOf(linia.quantitat()));
            // toPlainString evita la notació científica (1E+3) que podria fer servir toString
            element.setAttribute("preuUnitari", linia.preuUnitari().toPlainString());
            linies.appendChild(element);
        }
        arrel.appendChild(linies);

        Element total = afegirElementAmbText(document, arrel, "total", comanda.total().toPlainString());
        total.setAttribute("moneda", "EUR");

        if (comanda.observacions() != null && !comanda.observacions().isBlank()) {
            Element observacions = document.createElement("observacions");
            // CDATA: el text pot contenir < i & sense que s'hagin d'escapar
            observacions.appendChild(document.createCDATASection(comanda.observacions()));
            arrel.appendChild(observacions);
        }
        return document;
    }

    /**
     * Crea un element amb un text i l'afegeix com a últim fill del pare.
     * Amb setTextContent, DOM s'encarrega d'escapar els caràcters especials en desar.
     *
     * @return l'element creat
     */
    private static Element afegirElementAmbText(Document document, Element pare, String nom, String text) {
        Element element = document.createElement(nom);
        element.setTextContent(text);
        pare.appendChild(element);
        return element;
    }

    /**
     * Desa un document en un fitxer, indentat i en UTF-8. Crea les carpetes que falten.
     *
     * @param document document que es vol desar
     * @param fitxer   fitxer de destinació
     * @throws TransformerException si falla la transformació
     * @throws IOException          si no es pot escriure el fitxer
     */
    public static void desar(Document document, Path fitxer) throws TransformerException, IOException {
        Path carpeta = fitxer.toAbsolutePath().getParent();
        if (carpeta != null) {
            Files.createDirectories(carpeta);
        }
        try (OutputStream sortida = Files.newOutputStream(fitxer)) {
            transformador().transform(new DOMSource(document), new StreamResult(sortida));
        }
    }

    /**
     * Converteix un document en text XML indentat.
     *
     * @param document document que es vol convertir
     * @return text XML
     * @throws TransformerException si falla la transformació
     */
    public static String aText(Document document) throws TransformerException {
        StringWriter text = new StringWriter();
        transformador().transform(new DOMSource(document), new StreamResult(text));
        return text.toString();
    }

    /**
     * Un Transformer sense fulla d'estil copia l'arbre tal com és (transformació identitat).
     * Les propietats de sortida decideixen com s'escriu.
     */
    private static Transformer transformador() throws TransformerException {
        Transformer transformador = TransformerFactory.newInstance().newTransformer();
        transformador.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformador.setOutputProperty(OutputKeys.INDENT, "yes");
        transformador.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
        return transformador;
    }

    /**
     * Crea una comanda de mostra i la desa a la carpeta de sortida.
     *
     * @param args no es fan servir
     * @throws Exception si no es pot crear o desar el document
     */
    public static void main(String[] args) throws Exception {
        Comanda comanda = new Comanda("C2001", LocalDate.of(2026, 10, 5),
                new Client("Laia Ferrer", "laia.ferrer@example.com"),
                List.of(new LiniaComanda("P003", 1, new BigDecimal("229.00")),
                        new LiniaComanda("P006", 2, new BigDecimal("12.00"))),
                "Lliurar abans de les 14 h. Codi de la porta: <A&B>");
        Document document = crearComanda(comanda);
        Path fitxer = Sortida.fitxer("comanda-" + comanda.id() + ".xml");
        desar(document, fitxer);
        System.out.println(aText(document));
        System.out.println("Comanda desada a " + fitxer.toAbsolutePath());
    }
}
