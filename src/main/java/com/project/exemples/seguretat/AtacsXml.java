package com.project.exemples.seguretat;

import com.project.exemples.comu.Xml;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Exemple 12. Demostració dels dos atacs més coneguts contra parsers XML, per veure per què cal
 * configurar-los amb {@link ParsersSegurs}.
 *
 * <ul>
 *     <li><b>XXE</b> (XML External Entity): una entitat externa fa que el parser llegeixi un
 *     fitxer del servidor i n'inclogui el contingut al document.</li>
 *     <li><b>Billion laughs</b>: entitats niades que, en expandir-se, ocupen gigues de memòria
 *     a partir d'un fitxer d'uns pocs bytes (denegació de servei).</li>
 * </ul>
 */
public final class AtacsXml {

    private AtacsXml() {
    }

    /**
     * Crea un document que intenta incloure el contingut d'un fitxer local dins de l'element {@code <nom>}.
     *
     * @param fitxer fitxer que l'atacant voldria llegir
     * @return text del document maliciós
     */
    public static String documentXxe(Path fitxer) {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE producte [
                  <!ENTITY secret SYSTEM "%s">
                ]>
                <producte><nom>&secret;</nom></producte>
                """.formatted(fitxer.toUri());
    }

    /**
     * Crea un document "billion laughs": cada nivell repeteix deu vegades l'anterior,
     * de manera que el text final creix com 10 elevat al nombre de nivells.
     *
     * @param nivells nombre de nivells d'entitats niades
     * @return text del document maliciós
     */
    public static String documentBillionLaughs(int nivells) {
        StringBuilder dtd = new StringBuilder("  <!ENTITY r0 \"ha\">\n");
        for (int i = 1; i <= nivells; i++) {
            dtd.append("  <!ENTITY r").append(i).append(" \"");
            dtd.append(("&r" + (i - 1) + ";").repeat(10));
            dtd.append("\">\n");
        }
        return "<?xml version=\"1.0\"?>\n<!DOCTYPE riure [\n" + dtd + "]>\n<riure>&r" + nivells + ";</riure>\n";
    }

    /**
     * Llegeix un document amb la fàbrica indicada i retorna el text del seu element arrel.
     *
     * @param fabrica fàbrica DOM que es vol provar
     * @param xml     text del document
     * @return text de l'element arrel
     * @throws SAXException si el parser rebutja el document
     * @throws IOException  si no es pot llegir
     */
    public static String textDelDocument(DocumentBuilderFactory fabrica, String xml) throws SAXException, IOException {
        try {
            DocumentBuilder constructor = fabrica.newDocumentBuilder();
            constructor.setErrorHandler(Xml.ERRORS_SENSE_SORTIDA);
            Document document = constructor.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
            return document.getDocumentElement().getTextContent();
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Executa la demostració: crea un fitxer "secret" temporal i intenta llegir-lo amb els dos parsers.
     *
     * @param args no es fan servir
     * @throws IOException si no es pot crear el fitxer temporal
     */
    public static void main(String[] args) throws IOException {
        Path secret = Files.createTempFile("secret", ".txt");
        try {
            Files.writeString(secret, "contrasenya-de-la-base-de-dades=1234");
            String xxe = documentXxe(secret);
            System.out.println("Document maliciós:\n" + xxe);

            provar("Parser per defecte", ParsersSegurs.fabricaDomPerDefecte(), xxe);
            provar("Parser segur", ParsersSegurs.fabricaDomSegura(), xxe);

            String bomba = documentBillionLaughs(10);
            System.out.println("\nDocument \"billion laughs\" de " + bomba.length() + " caràcters (10^10 rialles si s'expandeix)");
            provar("Parser per defecte", ParsersSegurs.fabricaDomPerDefecte(), bomba);
            provar("Parser segur", ParsersSegurs.fabricaDomSegura(), bomba);
        } finally {
            Files.deleteIfExists(secret);
        }
    }

    private static void provar(String descripcio, DocumentBuilderFactory fabrica, String xml) throws IOException {
        try {
            String text = textDelDocument(fabrica, xml);
            System.out.println("- " + descripcio + ": ha llegit «" + text + "»");
        } catch (SAXException e) {
            System.out.println("- " + descripcio + ": ha rebutjat el document (" + e.getMessage() + ")");
        }
    }
}
