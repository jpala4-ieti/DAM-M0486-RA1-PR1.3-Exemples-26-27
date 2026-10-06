package com.project.exemples.namespaces;

import com.project.exemples.Fluxos;
import com.project.exemples.comu.Recursos;
import com.project.exemples.comu.Xml;
import com.project.exemples.namespaces.EspaisDeNoms.ResumFactura;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.XMLConstants;
import javax.xml.xpath.XPathFactory;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Exemple 13: espais de noms")
class EspaisDeNomsTest {

    private static Document factura;
    private static Document facturaSenseEspais;

    @BeforeAll
    static void llegirFactura() throws Exception {
        try (InputStream entrada = Recursos.dades("factura.xml")) {
            factura = EspaisDeNoms.llegir(entrada);
        }
        try (InputStream entrada = Recursos.dades("factura.xml")) {
            facturaSenseEspais = Xml.llegir(entrada, false);
        }
    }

    @Test
    @DisplayName("L'element arrel té nom local, prefix i URI")
    void parts() {
        Element arrel = factura.getDocumentElement();
        assertEquals("factura", arrel.getLocalName());
        assertEquals("fac", arrel.getPrefix());
        assertEquals(EspaisDeNoms.NS_FACTURA, arrel.getNamespaceURI());
    }

    @Test
    @DisplayName("getElementsByTagNameNS distingeix elements amb el mateix nom local")
    void cercaPerEspaiDeNoms() {
        assertEquals(List.of("Byte & Bit SL", "Oficines Llevant SA"), EspaisDeNoms.nomsEmpreses(factura));
        assertEquals(1, factura.getElementsByTagNameNS(EspaisDeNoms.NS_NOTES, "nom").getLength());
        assertEquals(3, factura.getElementsByTagNameNS("*", "nom").getLength());
    }

    @Test
    @DisplayName("Sense tenir en compte els espais de noms, el prefix forma part del nom")
    void senseEspaisDeNoms() {
        assertEquals(1, facturaSenseEspais.getElementsByTagName("nom").getLength());
        assertEquals(2, facturaSenseEspais.getElementsByTagName("emp:nom").getLength());
        assertNull(facturaSenseEspais.getDocumentElement().getNamespaceURI());
    }

    @Test
    @DisplayName("Error típic: XPath sense context no troba res en un document amb espais de noms")
    void xpathSenseContext() throws Exception {
        assertEquals("", XPathFactory.newInstance().newXPath().evaluate("/factura/@numero", factura));
    }

    @Test
    @DisplayName("Amb el context, els prefixos de l'expressió no han de coincidir amb els del document")
    void xpathAmbContext() throws Exception {
        assertEquals("F-2026-0042", EspaisDeNoms.xpathFactura().evaluate("/f:factura/@numero", factura));
    }

    @Test
    @DisplayName("Els elements de l'espai de noms predeterminat també necessiten prefix a XPath")
    void espaiPredeterminat() throws Exception {
        assertEquals("Lliurament a recepció", EspaisDeNoms.xpathFactura().evaluate("/f:factura/n:notes/n:nom", factura));
        assertEquals("", EspaisDeNoms.xpathFactura().evaluate("/f:factura/notes/nom", factura));
    }

    @Test
    @DisplayName("Calcula la base, els impostos i el total de la factura")
    void calcular() {
        assertEquals(new ResumFactura("F-2026-0042", "Oficines Llevant SA",
                        new BigDecimal("1115.60"), new BigDecimal("234.28"), new BigDecimal("1349.88")),
                EspaisDeNoms.calcular(factura));
    }

    @Test
    @DisplayName("Crea un document amb un espai de noms i un prefix propis")
    void crearResum() throws Exception {
        ResumFactura resum = EspaisDeNoms.calcular(factura);
        Document document = EspaisDeNoms.crearResum(resum);
        String text = Xml.aText(document);
        assertTrue(text.contains("<res:resum xmlns:res=\"" + EspaisDeNoms.NS_RESUM + "\""), text);

        Document llegit = Xml.llegir(Fluxos.de(text), true);
        assertEquals(EspaisDeNoms.NS_RESUM, llegit.getDocumentElement().getNamespaceURI());
        assertEquals("1349.88", llegit.getElementsByTagNameNS(EspaisDeNoms.NS_RESUM, "total").item(0).getTextContent());
    }

    @Test
    @DisplayName("El context resol prefixos en URI i a l'inrevés")
    void context() {
        ContextEspaisDeNoms context = new ContextEspaisDeNoms(Map.of("f", EspaisDeNoms.NS_FACTURA));
        assertEquals(EspaisDeNoms.NS_FACTURA, context.getNamespaceURI("f"));
        assertEquals(XMLConstants.XML_NS_URI, context.getNamespaceURI("xml"));
        assertEquals(XMLConstants.NULL_NS_URI, context.getNamespaceURI("desconegut"));
        assertEquals("f", context.getPrefix(EspaisDeNoms.NS_FACTURA));
        assertNull(context.getPrefix("urn:desconegut"));
        assertFalse(context.getPrefixes("urn:desconegut").hasNext());
        assertThrows(IllegalArgumentException.class, () -> context.getNamespaceURI(null));
    }
}
