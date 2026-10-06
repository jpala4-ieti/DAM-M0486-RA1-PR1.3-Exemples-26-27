package com.project.exemples.configuracio;

import com.project.exemples.Fluxos;
import com.project.exemples.comu.Recursos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.xml.sax.SAXException;

import java.io.InputStream;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Exemple 14: fitxer de configuració")
class LectorConfiguracioTest {

    private static final String MINIMA = """
            <configuracio>
                <aplicacio><nom>App</nom></aplicacio>
                <baseDades><url>jdbc:sqlite:app.db</url></baseDades>
                %s
            </configuracio>
            """;

    private static ConfiguracioBotiga llegir(String xml) {
        return LectorConfiguracio.llegir(Fluxos.de(xml));
    }

    /** Configuració mínima amb un fragment afegit. */
    private static ConfiguracioBotiga minimaAmb(String fragment) {
        return llegir(MINIMA.formatted(fragment));
    }

    @Test
    @DisplayName("Llegeix tots els paràmetres del fitxer de mostra")
    void fitxerDeMostra() throws Exception {
        ConfiguracioBotiga configuracio;
        try (InputStream entrada = Recursos.dades("configuracio.xml")) {
            configuracio = LectorConfiguracio.llegir(entrada);
        }
        assertAll(
                () -> assertEquals("Botiga Byte & Bit", configuracio.nomAplicacio()),
                () -> assertEquals("produccio", configuracio.entorn()),
                () -> assertFalse(configuracio.depuracio()),
                () -> assertEquals("jdbc:sqlite:botiga.db", configuracio.urlBaseDades()),
                () -> assertEquals("botiga", configuracio.usuariBaseDades()),
                () -> assertEquals(10, configuracio.connexionsMaximes()),
                () -> assertEquals(Duration.ofSeconds(30), configuracio.tempsEspera()),
                () -> assertTrue(configuracio.correuActivat()),
                () -> assertEquals("smtp.example.com", configuracio.servidorCorreu()),
                () -> assertEquals(587, configuracio.portCorreu()),
                () -> assertEquals(List.of("ca", "es", "en"), configuracio.idiomes()),
                () -> assertEquals("ca", configuracio.idiomaPredeterminat()));
    }

    @Test
    @DisplayName("Els paràmetres opcionals que falten prenen el valor per defecte")
    void valorsPerDefecte() {
        ConfiguracioBotiga configuracio = minimaAmb("");
        assertAll(
                () -> assertEquals("desenvolupament", configuracio.entorn()),
                () -> assertFalse(configuracio.depuracio()),
                () -> assertEquals("", configuracio.usuariBaseDades()),
                () -> assertEquals(5, configuracio.connexionsMaximes()),
                () -> assertEquals(Duration.ofSeconds(10), configuracio.tempsEspera()),
                () -> assertFalse(configuracio.correuActivat()),
                () -> assertEquals("localhost", configuracio.servidorCorreu()),
                () -> assertEquals(25, configuracio.portCorreu()),
                () -> assertEquals(List.of(), configuracio.idiomes()),
                () -> assertEquals("ca", configuracio.idiomaPredeterminat()));
    }

    @Test
    @DisplayName("Un element buit compta com si no hi fos")
    void elementBuit() {
        assertEquals(5, minimaAmb("<baseDades><connexionsMaximes>  </connexionsMaximes></baseDades>").connexionsMaximes());
    }

    @Test
    @DisplayName("Si falta un paràmetre obligatori, l'error diu quin")
    void obligatori() {
        ConfiguracioException error = assertThrows(ConfiguracioException.class,
                () -> llegir("<configuracio><baseDades><url>jdbc:x</url></baseDades></configuracio>"));
        assertTrue(error.getMessage().contains("aplicacio/nom"));
    }

    @Test
    @DisplayName("Un número mal escrit dona un error amb el paràmetre i el valor")
    void enterInvalid() {
        ConfiguracioException error = assertThrows(ConfiguracioException.class,
                () -> llegir(MINIMA.formatted("").replace("<url>jdbc:sqlite:app.db</url>",
                        "<url>jdbc:sqlite:app.db</url><connexionsMaximes>deu</connexionsMaximes>")));
        assertTrue(error.getMessage().contains("baseDades/connexionsMaximes"));
        assertTrue(error.getMessage().contains("deu"));
    }

    @ParameterizedTest(name = "\"{0}\" → {1}")
    @CsvSource({"true, true", "sí, true", "SI, true", "1, true", "false, false", "no, false", "0, false"})
    @DisplayName("Els booleans admeten diverses maneres d'escriure'ls")
    void booleans(String valor, boolean esperat) {
        assertEquals(esperat, minimaAmb("<correu activat=\"" + valor + "\"/>").correuActivat());
    }

    @Test
    @DisplayName("Un booleà desconegut no s'accepta")
    void booleaInvalid() {
        assertThrows(ConfiguracioException.class, () -> minimaAmb("<correu activat=\"potser\"/>"));
    }

    @ParameterizedTest(name = "{0} {1}")
    @CsvSource({"500, ms, PT0.5S", "30, s, PT30S", "2, min, PT2M"})
    @DisplayName("Els temps s'interpreten segons la unitat")
    void durades(String valor, String unitat, String esperat) {
        ConfiguracioBotiga configuracio = llegir(MINIMA.formatted("").replace("<url>jdbc:sqlite:app.db</url>",
                "<url>jdbc:sqlite:app.db</url><tempsEspera unitat=\"" + unitat + "\">" + valor + "</tempsEspera>"));
        assertEquals(Duration.parse(esperat), configuracio.tempsEspera());
    }

    @Test
    @DisplayName("Sense unitat, el temps és en segons")
    void duradaSenseUnitat() {
        ConfiguracioBotiga configuracio = llegir(MINIMA.formatted("").replace("<url>jdbc:sqlite:app.db</url>",
                "<url>jdbc:sqlite:app.db</url><tempsEspera>15</tempsEspera>"));
        assertEquals(Duration.ofSeconds(15), configuracio.tempsEspera());
    }

    @Test
    @DisplayName("Una unitat de temps desconeguda dona un error que diu quines s'admeten")
    void unitatInvalida() {
        ConfiguracioException error = assertThrows(ConfiguracioException.class,
                () -> llegir(MINIMA.formatted("").replace("<url>jdbc:sqlite:app.db</url>",
                        "<url>jdbc:sqlite:app.db</url><tempsEspera unitat=\"h\">1</tempsEspera>")));
        assertTrue(error.getMessage().contains("ms, s o min"));
    }

    @Test
    @DisplayName("Les sobreescriptures tenen prioritat sobre el fitxer")
    void sobreescriptures() throws Exception {
        ConfiguracioBotiga configuracio;
        try (InputStream entrada = Recursos.dades("configuracio.xml")) {
            configuracio = LectorConfiguracio.llegir(entrada, Map.of(
                    "baseDades/url", "jdbc:sqlite:proves.db",
                    "correu/port", "2525",
                    "idiomes/idioma", "ca, en"));
        }
        assertEquals("jdbc:sqlite:proves.db", configuracio.urlBaseDades());
        assertEquals(2525, configuracio.portCorreu());
        assertEquals(List.of("ca", "en"), configuracio.idiomes());
        assertEquals("botiga", configuracio.usuariBaseDades(), "La resta de valors no canvien");
    }

    @Test
    @DisplayName("Una sobreescriptura pot donar un paràmetre obligatori que falta al fitxer")
    void sobreescripturaObligatori() {
        ConfiguracioBotiga configuracio = LectorConfiguracio.llegir(
                Fluxos.de("<configuracio><aplicacio><nom>App</nom></aplicacio></configuracio>"),
                Map.of("baseDades/url", "jdbc:sqlite:memoria"));
        assertEquals("jdbc:sqlite:memoria", configuracio.urlBaseDades());
    }

    @Test
    @DisplayName("No accepta versions del format més noves que les que coneix")
    void versio() {
        assertThrows(ConfiguracioException.class,
                () -> llegir(MINIMA.formatted("").replace("<configuracio>", "<configuracio versio=\"3\">")));
        assertEquals("App", llegir(MINIMA.formatted("").replace("<configuracio>", "<configuracio versio=\"2\">")).nomAplicacio());
    }

    @Test
    @DisplayName("Un XML mal format es converteix en ConfiguracioException")
    void malFormat() {
        ConfiguracioException error = assertThrows(ConfiguracioException.class, () -> llegir("<configuracio>"));
        assertInstanceOf(SAXException.class, error.getCause());
    }

    @Test
    @DisplayName("La llista d'idiomes no es pot modificar")
    void immutable() {
        assertThrows(UnsupportedOperationException.class, () -> minimaAmb("").idiomes().add("fr"));
    }
}
