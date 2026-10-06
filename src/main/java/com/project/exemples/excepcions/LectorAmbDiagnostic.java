package com.project.exemples.excepcions;

import com.project.exemples.comu.Xml;
import com.project.exemples.seguretat.ParsersSegurs;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Exemple 8. Gestió d'errors en llegir XML: cada problema es tracta pel seu tipus i es
 * converteix en un missatge útil, amb la línia i la columna quan se saben.
 */
public final class LectorAmbDiagnostic {

    private LectorAmbDiagnostic() {
    }

    /**
     * Resultat de processar diversos fitxers.
     *
     * @param correctes noms dels fitxers que s'han pogut llegir
     * @param errors    errors dels fitxers que han fallat
     */
    public record InformeLot(List<String> correctes, List<ErrorXmlException> errors) {
    }

    /**
     * Llegeix un fitxer XML i tradueix qualsevol error a una {@link ErrorXmlException}.
     *
     * @param fitxer fitxer que es vol llegir
     * @return document llegit
     * @throws ErrorXmlException si el fitxer no existeix, no es pot llegir o no és XML ben format
     */
    public static Document llegir(Path fitxer) throws ErrorXmlException {
        DocumentBuilder constructor = nouConstructor();
        // try-with-resources: el fitxer es tanca sempre, tant si va bé com si falla
        try (InputStream entrada = Files.newInputStream(fitxer)) {
            return constructor.parse(entrada);
        } catch (NoSuchFileException e) {
            // NoSuchFileException és una IOException: s'ha de capturar abans
            throw new ErrorXmlException(fitxer, "el fitxer no existeix", e);
        } catch (SAXParseException e) {
            // SAXParseException sap on és l'error dins del fitxer
            throw new ErrorXmlException(fitxer, e.getLineNumber(), e.getColumnNumber(),
                    "XML mal format: " + e.getMessage(), e);
        } catch (SAXException e) {
            throw new ErrorXmlException(fitxer, "error en processar l'XML: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new ErrorXmlException(fitxer, "no es pot llegir el fitxer: " + e.getMessage(), e);
        }
    }

    private static DocumentBuilder nouConstructor() {
        try {
            DocumentBuilder constructor = ParsersSegurs.fabricaDomSegura().newDocumentBuilder();
            // Sense aquest gestor, el parser escriu "[Fatal Error] ..." per consola abans de llançar l'excepció
            constructor.setErrorHandler(Xml.ERRORS_SENSE_SORTIDA);
            return constructor;
        } catch (ParserConfigurationException e) {
            // Una configuració impossible és un error de programació, no del fitxer: excepció no comprovada
            throw new IllegalStateException("No es pot crear el parser XML", e);
        }
    }

    /**
     * Avalua una expressió XPath i tradueix l'error si l'expressió no és vàlida.
     *
     * @param document  document sobre el qual s'avalua
     * @param expressio expressió XPath
     * @return resultat com a text
     * @throws ErrorXmlException si l'expressió no és vàlida
     */
    public static String avaluar(Document document, String expressio) throws ErrorXmlException {
        try {
            return XPathFactory.newInstance().newXPath().evaluate(expressio, document);
        } catch (XPathExpressionException e) {
            throw new ErrorXmlException(null, "expressió XPath no vàlida: " + expressio, e);
        }
    }

    /**
     * Llegeix diversos fitxers. Un fitxer erroni no atura la resta: és el patró habitual dels
     * processos per lots (importacions nocturnes, càrregues de dades...).
     *
     * @param fitxers fitxers que es volen llegir
     * @return informe amb els correctes i els errors
     */
    public static InformeLot processarLot(List<Path> fitxers) {
        List<String> correctes = new ArrayList<>();
        List<ErrorXmlException> errors = new ArrayList<>();
        for (Path fitxer : fitxers) {
            try {
                llegir(fitxer);
                correctes.add(fitxer.getFileName().toString());
            } catch (ErrorXmlException e) {
                errors.add(e);
            }
        }
        return new InformeLot(correctes, errors);
    }

    /**
     * Crea uns quants fitxers de prova (correctes i erronis) i els processa.
     *
     * @param args no es fan servir
     * @throws IOException si no es poden crear els fitxers de prova
     */
    public static void main(String[] args) throws IOException {
        Path carpeta = Files.createTempDirectory("exemple-errors");
        try {
            Path correcte = Files.writeString(carpeta.resolve("correcte.xml"), "<cataleg><producte id=\"P1\"/></cataleg>");
            Path malFormat = Files.writeString(carpeta.resolve("mal-format.xml"),
                    "<cataleg>\n  <producte id=\"P1\">\n    <nom>Sense tancar\n  </producte>\n</cataleg>\n");
            Path buit = Files.writeString(carpeta.resolve("buit.xml"), "");
            Path inexistent = carpeta.resolve("no-existeix.xml");

            InformeLot informe = processarLot(List.of(correcte, malFormat, buit, inexistent));
            System.out.println("Fitxers correctes: " + informe.correctes());
            System.out.println("Fitxers amb errors:");
            for (ErrorXmlException error : informe.errors()) {
                System.out.println("- " + error.getMessage());
            }

            try {
                avaluar(llegir(correcte), "/cataleg/producte[@id=");
            } catch (ErrorXmlException e) {
                System.out.println("Error XPath: " + e.getMessage());
            }
        } finally {
            try (var fitxers = Files.list(carpeta)) {
                for (Path fitxer : fitxers.toList()) {
                    Files.delete(fitxer);
                }
            }
            Files.delete(carpeta);
        }
    }
}
