package com.project.exemples.comu;

import com.project.exemples.seguretat.ParsersSegurs;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Operacions bàsiques que es repeteixen als exemples avançats: llegir un document amb un
 * parser segur, crear-ne un de nou i convertir-lo en text o desar-lo.
 *
 * <p>Els exemples de la teoria (paquets {@code dom.*}) no fan servir aquesta classe:
 * allà es mostra tot el codi pas a pas.</p>
 */
public final class Xml {

    /**
     * Gestor d'errors que no escriu res per consola. Sense ell, el parser escriu
     * "[Fatal Error] ..." per la sortida d'errors abans de llançar l'excepció.
     */
    public static final ErrorHandler ERRORS_SENSE_SORTIDA = new ErrorHandler() {
        @Override
        public void warning(SAXParseException e) {
            // Els avisos no aturen la lectura
        }

        @Override
        public void error(SAXParseException e) throws SAXException {
            throw e;
        }

        @Override
        public void fatalError(SAXParseException e) throws SAXException {
            throw e;
        }
    };

    private Xml() {
    }

    /**
     * Llegeix un document XML sense tenir en compte els espais de noms.
     *
     * @param entrada flux amb l'XML (no es tanca)
     * @return document llegit
     * @throws IOException si no es pot llegir el flux
     * @throws SAXException si l'XML no està ben format
     */
    public static Document llegir(InputStream entrada) throws IOException, SAXException {
        return llegir(entrada, false);
    }

    /**
     * Llegeix un document XML amb un parser configurat de manera segura.
     *
     * @param entrada         flux amb l'XML (no es tanca)
     * @param ambEspaisDeNoms si el parser ha de tenir en compte els espais de noms
     * @return document llegit
     * @throws IOException si no es pot llegir el flux
     * @throws SAXException si l'XML no està ben format
     */
    public static Document llegir(InputStream entrada, boolean ambEspaisDeNoms) throws IOException, SAXException {
        var fabrica = ParsersSegurs.fabricaDomSegura();
        fabrica.setNamespaceAware(ambEspaisDeNoms);
        DocumentBuilder constructor = crearConstructor(fabrica);
        constructor.setErrorHandler(ERRORS_SENSE_SORTIDA);
        return constructor.parse(entrada);
    }

    /**
     * Crea un document buit, a punt per afegir-hi nodes.
     *
     * @return document nou
     */
    public static Document documentNou() {
        return crearConstructor(ParsersSegurs.fabricaDomSegura()).newDocument();
    }

    /**
     * Converteix un node (normalment un document sencer) en text XML indentat.
     *
     * @param node node que es vol convertir
     * @return text XML
     */
    public static String aText(Node node) {
        StringWriter text = new StringWriter();
        try {
            transformadorIndentat().transform(new DOMSource(node), new StreamResult(text));
        } catch (TransformerException e) {
            throw new IllegalStateException("No s'ha pogut convertir el node en text", e);
        }
        return text.toString();
    }

    /**
     * Desa un node en un fitxer, en UTF-8 i indentat. Crea les carpetes que falten.
     *
     * @param node   node que es vol desar
     * @param fitxer fitxer de destinació
     * @throws IOException si no es pot escriure el fitxer
     */
    public static void desar(Node node, Path fitxer) throws IOException {
        Path carpeta = fitxer.toAbsolutePath().getParent();
        if (carpeta != null) {
            Files.createDirectories(carpeta);
        }
        try (OutputStream sortida = Files.newOutputStream(fitxer)) {
            transformadorIndentat().transform(new DOMSource(node), new StreamResult(sortida));
        } catch (TransformerException e) {
            throw new IOException("No s'ha pogut desar " + fitxer + ": " + e.getMessage(), e);
        }
    }

    private static Transformer transformadorIndentat() throws TransformerException {
        Transformer transformador = ParsersSegurs.fabricaTransformerSegura().newTransformer();
        transformador.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformador.setOutputProperty(OutputKeys.INDENT, "yes");
        transformador.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        return transformador;
    }

    private static DocumentBuilder crearConstructor(javax.xml.parsers.DocumentBuilderFactory fabrica) {
        try {
            return fabrica.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            // És un error de programació (una configuració impossible), no un problema de les dades
            throw new IllegalStateException("No es pot crear el parser XML", e);
        }
    }
}
