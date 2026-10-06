package com.project.exemples.xslt;

import com.project.exemples.Fluxos;
import com.project.exemples.comu.Recursos;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.xml.transform.TransformerException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Exemple 10: transformacions XSLT")
class TransformacionsXsltTest {

    private static TransformacionsXslt transformacions;

    @BeforeAll
    static void compilarFullesDEstil() {
        transformacions = new TransformacionsXslt();
    }

    private static int comptar(String text, String fragment) {
        Matcher coincidencies = Pattern.compile(Pattern.quote(fragment)).matcher(text);
        int total = 0;
        while (coincidencies.find()) {
            total++;
        }
        return total;
    }

    @Nested
    @DisplayName("Catàleg a HTML")
    class Html {

        private static String html;

        @BeforeAll
        static void transformar() throws Exception {
            try (InputStream cataleg = Recursos.dades("cataleg.xml")) {
                html = transformacions.catalegAHtml(cataleg, "Novetats");
            }
        }

        @Test
        @DisplayName("El títol arriba com a paràmetre")
        void titol() {
            assertTrue(html.contains("<title>Novetats</title>"));
            assertTrue(html.contains("<h1>Novetats</h1>"));
        }

        @Test
        @DisplayName("Hi ha un article per a cada producte")
        void articles() {
            assertEquals(6, comptar(html, "<article "));
        }

        @Test
        @DisplayName("Els productes surten del més barat al més car")
        void ordenats() {
            List<String> ordre = List.of("P006", "P002", "P001", "P004", "P005", "P003");
            for (int i = 1; i < ordre.size(); i++) {
                assertTrue(html.indexOf("id=\"" + ordre.get(i - 1) + "\"") < html.indexOf("id=\"" + ordre.get(i) + "\""),
                        ordre.get(i - 1) + " hauria d'anar abans que " + ordre.get(i));
            }
        }

        @Test
        @DisplayName("Els productes sense estoc tenen una classe CSS diferent")
        void senseEstoc() {
            assertTrue(html.contains("id=\"P002\" class=\"producte sense-estoc\""));
            assertTrue(html.contains("id=\"P005\" class=\"producte sense-estoc\""));
            assertTrue(html.contains("id=\"P001\" class=\"producte\""));
        }

        @Test
        @DisplayName("Els preus tenen format català (coma decimal)")
        void formatDelPreu() {
            assertTrue(html.contains("229,00 &euro;"));
            assertTrue(html.contains("12,00 &euro;"));
        }

        @Test
        @DisplayName("El text es protegeix: <blaus> no es converteix en una etiqueta HTML")
        void textEscapat() {
            assertTrue(html.contains("&lt;blaus&gt;"));
        }

        @Test
        @DisplayName("Hi ha un element de llista per a cada etiqueta")
        void etiquetes() {
            assertEquals(8, comptar(html, "<li>"));
        }
    }

    @Nested
    @DisplayName("Catàleg a CSV")
    class Csv {

        private static List<String> linies(String csv) {
            return csv.lines().toList();
        }

        @Test
        @DisplayName("Té una capçalera i una línia per producte")
        void estructura() throws Exception {
            try (InputStream cataleg = Recursos.dades("cataleg.xml")) {
                List<String> linies = linies(transformacions.catalegACsv(cataleg));
                assertEquals(7, linies.size());
                assertEquals("id;nom;categoria;preu;estoc", linies.get(0));
                assertEquals("P001;\"Teclat mecànic\";periferics;49.90;25", linies.get(1));
            }
        }

        @Test
        @DisplayName("El separador es pot canviar amb un paràmetre")
        void separador() throws Exception {
            try (InputStream cataleg = Recursos.dades("cataleg.xml")) {
                assertEquals("id,nom,categoria,preu,estoc", linies(transformacions.catalegACsv(cataleg, ",")).get(0));
            }
        }

        @Test
        @DisplayName("Les cometes dins d'un nom es dupliquen")
        void cometes() throws Exception {
            String xml = "<cataleg><producte id=\"X1\" categoria=\"cables\"><nom>Cable \"pla\" de 2 m</nom>"
                    + "<preu>3.50</preu><estoc>10</estoc></producte></cataleg>";
            assertEquals("X1;\"Cable \"\"pla\"\" de 2 m\";cables;3.50;10",
                    linies(transformacions.catalegACsv(Fluxos.de(xml))).get(1));
        }
    }

    @Test
    @DisplayName("Aplica una fulla d'estil qualsevol")
    void fullaQualsevol() throws Exception {
        String xsl = """
                <xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
                    <xsl:output method="text"/>
                    <xsl:template match="/"><xsl:value-of select="count(//producte[estoc &gt; 0])"/></xsl:template>
                </xsl:stylesheet>
                """;
        try (InputStream cataleg = Recursos.dades("cataleg.xml")) {
            assertEquals("4", TransformacionsXslt.transformar(cataleg, Fluxos.de(xsl), Map.of()));
        }
    }

    @Test
    @DisplayName("Sense paràmetre, la fulla d'estil fa servir el valor per defecte")
    void parametrePerDefecte() throws Exception {
        try (InputStream cataleg = Recursos.dades("cataleg.xml"); InputStream xsl = Recursos.obrir("/xslt/cataleg-a-html.xsl")) {
            assertTrue(TransformacionsXslt.transformar(cataleg, xsl, Map.of()).contains("<title>Cat&agrave;leg</title>"));
        }
    }

    @Test
    @DisplayName("Una fulla d'estil mal escrita llança TransformerException")
    void fullaInvalida() {
        String xsl = "<xsl:stylesheet version=\"1.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\"><xsl:template></xsl:stylesheet>";
        assertThrows(TransformerException.class,
                () -> TransformacionsXslt.transformar(Fluxos.de("<a/>"), Fluxos.de(xsl), Map.of()));
    }

    @Test
    @DisplayName("Una fulla d'estil compilada es pot reutilitzar")
    void reutilitzacio() throws Exception {
        String primer;
        String segon;
        try (InputStream cataleg = Recursos.dades("cataleg.xml")) {
            primer = transformacions.catalegACsv(cataleg);
        }
        try (InputStream cataleg = Recursos.dades("cataleg.xml")) {
            segon = transformacions.catalegACsv(cataleg);
        }
        assertEquals(primer, segon);
    }
}
