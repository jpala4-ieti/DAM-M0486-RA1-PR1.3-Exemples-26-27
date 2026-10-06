package com.project.exemples.excepcions;

import com.project.exemples.Fluxos;
import com.project.exemples.excepcions.LectorAmbDiagnostic.InformeLot;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Document;
import org.xml.sax.SAXParseException;

import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Exemple 8: gestió d'errors")
class LectorAmbDiagnosticTest {

    private static final String MAL_FORMAT = "<cataleg>\n  <producte id=\"P1\">\n    <nom>Sense tancar\n  </producte>\n</cataleg>\n";

    @TempDir
    Path temporal;

    private Path fitxer(String nom, String contingut) throws IOException {
        return Files.writeString(temporal.resolve(nom), contingut);
    }

    @Test
    @DisplayName("Llegeix un fitxer correcte")
    void fitxerCorrecte() throws Exception {
        Document document = LectorAmbDiagnostic.llegir(fitxer("correcte.xml", "<cataleg/>"));
        assertEquals("cataleg", document.getDocumentElement().getTagName());
    }

    @Test
    @DisplayName("Un fitxer que no existeix dona un error sense posició")
    void fitxerInexistent() {
        Path inexistent = temporal.resolve("no-existeix.xml");
        ErrorXmlException error = assertThrows(ErrorXmlException.class, () -> LectorAmbDiagnostic.llegir(inexistent));
        assertEquals("no-existeix.xml: el fitxer no existeix", error.getMessage());
        assertFalse(error.tePosicio());
        assertEquals(inexistent, error.fitxer().orElseThrow());
        assertInstanceOf(NoSuchFileException.class, error.getCause());
    }

    @Test
    @DisplayName("Un XML mal format dona la línia i la columna de l'error")
    void malFormat() throws Exception {
        Path malFormat = fitxer("mal-format.xml", MAL_FORMAT);
        ErrorXmlException error = assertThrows(ErrorXmlException.class, () -> LectorAmbDiagnostic.llegir(malFormat));
        assertEquals(4, error.linia());
        assertTrue(error.columna() > 0);
        assertTrue(error.getMessage().startsWith("mal-format.xml: línia 4: XML mal format"), error.getMessage());
        assertInstanceOf(SAXParseException.class, error.getCause());
    }

    @Test
    @DisplayName("Un fitxer buit no és un XML vàlid")
    void fitxerBuit() throws Exception {
        Path buit = fitxer("buit.xml", "");
        ErrorXmlException error = assertThrows(ErrorXmlException.class, () -> LectorAmbDiagnostic.llegir(buit));
        assertTrue(error.tePosicio());
    }

    @Test
    @DisplayName("Una carpeta en lloc d'un fitxer també es tradueix a ErrorXmlException")
    void carpeta() {
        assertThrows(ErrorXmlException.class, () -> LectorAmbDiagnostic.llegir(temporal));
    }

    @Test
    @DisplayName("Els errors no s'escriuen per la sortida d'errors")
    void senseSortidaDErrors() throws Exception {
        Path malFormat = fitxer("mal-format.xml", MAL_FORMAT);
        String errors = Fluxos.capturarErrors(() ->
                assertThrows(ErrorXmlException.class, () -> LectorAmbDiagnostic.llegir(malFormat)));
        assertEquals("", errors);
    }

    @Test
    @DisplayName("Avalua una expressió XPath correcta")
    void xpathCorrecte() throws Exception {
        Document document = LectorAmbDiagnostic.llegir(fitxer("cataleg.xml", "<cataleg><producte id=\"P1\"/></cataleg>"));
        assertEquals("P1", LectorAmbDiagnostic.avaluar(document, "/cataleg/producte/@id"));
    }

    @Test
    @DisplayName("Una expressió XPath incorrecta es tradueix a ErrorXmlException")
    void xpathIncorrecte() throws Exception {
        Document document = LectorAmbDiagnostic.llegir(fitxer("cataleg.xml", "<cataleg/>"));
        ErrorXmlException error = assertThrows(ErrorXmlException.class,
                () -> LectorAmbDiagnostic.avaluar(document, "/cataleg/producte[@id="));
        assertTrue(error.fitxer().isEmpty());
        assertInstanceOf(XPathExpressionException.class, error.getCause());
    }

    @Test
    @DisplayName("Un lot continua encara que algun fitxer falli")
    void lot() throws Exception {
        InformeLot informe = LectorAmbDiagnostic.processarLot(List.of(
                fitxer("a.xml", "<a/>"),
                fitxer("b.xml", MAL_FORMAT),
                fitxer("c.xml", "<c/>"),
                temporal.resolve("d.xml")));
        assertEquals(List.of("a.xml", "c.xml"), informe.correctes());
        assertEquals(List.of("b.xml", "d.xml"),
                informe.errors().stream().map(e -> e.fitxer().orElseThrow().getFileName().toString()).toList());
    }

    @Test
    @DisplayName("Un lot buit dona un informe buit")
    void lotBuit() {
        InformeLot informe = LectorAmbDiagnostic.processarLot(List.of());
        assertTrue(informe.correctes().isEmpty());
        assertTrue(informe.errors().isEmpty());
    }
}
