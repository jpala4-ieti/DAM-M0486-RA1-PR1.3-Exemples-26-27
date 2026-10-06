package com.project.exemples.comu;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Utilitats: recursos del classpath")
class RecursosTest {

    @Test
    @DisplayName("Obre un fitxer de dades de mostra")
    void obreUnFitxerDeDades() throws Exception {
        try (InputStream entrada = Recursos.dades("cataleg.xml")) {
            String text = new String(entrada.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(text.contains("<cataleg"));
        }
    }

    @Test
    @DisplayName("Un recurs que no existeix dona un error clar")
    void recursInexistent() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> Recursos.dades("no-existeix.xml"));
        assertTrue(error.getMessage().contains("/dades/no-existeix.xml"));
    }

    @Test
    @DisplayName("Retorna la URL d'un recurs")
    void urlDUnRecurs() {
        assertNotNull(Recursos.url("/esquemes/comanda.xsd"));
        assertThrows(IllegalArgumentException.class, () -> Recursos.url("/no-existeix.xsd"));
    }
}
