package com.project.exemples.seguretat;

import com.project.exemples.Fluxos;
import com.project.exemples.comu.Xml;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Exemple 12: seguretat dels parsers XML")
class ParsersSegursTest {

    private static final String SECRET = "contrasenya=1234";

    @TempDir
    Path temporal;

    private String xxe;

    @BeforeEach
    void crearFitxerSecret() throws Exception {
        Path secret = Files.writeString(temporal.resolve("secret.txt"), SECRET);
        xxe = AtacsXml.documentXxe(secret);
    }

    @Test
    @DisplayName("El parser per defecte és vulnerable: llegeix el fitxer secret")
    void parserPerDefecteVulnerable() throws Exception {
        assertEquals(SECRET, AtacsXml.textDelDocument(ParsersSegurs.fabricaDomPerDefecte(), xxe));
    }

    @Test
    @DisplayName("El parser DOM segur rebutja el document")
    void domSegur() {
        SAXParseException error = assertThrows(SAXParseException.class,
                () -> AtacsXml.textDelDocument(ParsersSegurs.fabricaDomSegura(), xxe));
        assertTrue(error.getMessage().contains("DOCTYPE"));
    }

    @Test
    @DisplayName("La utilitat Xml també fa servir el parser segur")
    void utilitatXml() {
        assertThrows(SAXParseException.class, () -> Xml.llegir(Fluxos.de(xxe)));
    }

    @Test
    @DisplayName("El parser SAX segur rebutja el document")
    void saxSegur() {
        assertThrows(SAXException.class,
                () -> ParsersSegurs.fabricaSaxSegura().newSAXParser().parse(Fluxos.de(xxe), new DefaultHandler()));
    }

    @Test
    @DisplayName("El lector StAX segur no resol l'entitat")
    void staxSegur() {
        assertThrows(XMLStreamException.class, () -> {
            XMLStreamReader lector = ParsersSegurs.fabricaStaxSegura().createXMLStreamReader(Fluxos.de(xxe));
            while (lector.hasNext()) {
                if (lector.next() == XMLStreamConstants.CHARACTERS) {
                    assertFalse(lector.getText().contains(SECRET));
                }
            }
        });
    }

    @Test
    @DisplayName("El Transformer segur no accedeix al fitxer")
    void transformerSegur() {
        assertThrows(TransformerException.class, () -> ParsersSegurs.fabricaTransformerSegura().newTransformer()
                .transform(new StreamSource(Fluxos.de(xxe)), new StreamResult(new StringWriter())));
    }

    @Test
    @DisplayName("Billion laughs: el JDK ja limita les expansions, i el parser segur ni tan sols ho intenta")
    void billionLaughs() {
        String bomba = AtacsXml.documentBillionLaughs(10);
        assertThrows(SAXException.class, () -> AtacsXml.textDelDocument(ParsersSegurs.fabricaDomPerDefecte(), bomba));
        assertThrows(SAXParseException.class, () -> AtacsXml.textDelDocument(ParsersSegurs.fabricaDomSegura(), bomba));
    }

    @Test
    @DisplayName("Amb pocs nivells, el document billion laughs és petit i s'entén")
    void estructuraBillionLaughs() throws Exception {
        String document = AtacsXml.documentBillionLaughs(2);
        assertTrue(document.contains("<!ENTITY r2 \"&r1;&r1;&r1;"));
        assertEquals("ha".repeat(100), AtacsXml.textDelDocument(ParsersSegurs.fabricaDomPerDefecte(), document));
    }

    @Test
    @DisplayName("Un document normal es llegeix sense problemes amb la configuració segura")
    void documentNormal() throws Exception {
        assertEquals("Teclat", AtacsXml.textDelDocument(ParsersSegurs.fabricaDomSegura(), "<producte><nom>Teclat</nom></producte>"));
    }

    @Test
    @DisplayName("La fàbrica segura té activades les proteccions")
    void configuracio() throws Exception {
        DocumentBuilderFactory fabrica = ParsersSegurs.fabricaDomSegura();
        assertTrue(fabrica.getFeature("http://apache.org/xml/features/disallow-doctype-decl"));
        assertFalse(fabrica.isXIncludeAware());
        assertFalse(fabrica.isExpandEntityReferences());
    }
}
