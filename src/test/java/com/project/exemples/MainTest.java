package com.project.exemples;

import com.project.exemples.comu.Sortida;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Menú d'exemples")
class MainTest {

    @TempDir
    Path sortida;

    @BeforeEach
    void redirigirSortida() {
        System.setProperty(Sortida.PROPIETAT, sortida.toString());
    }

    @AfterEach
    void restaurarSortida() {
        System.clearProperty(Sortida.PROPIETAT);
    }

    @Test
    @DisplayName("Hi ha 14 exemples numerats de l'1 al 14")
    void numeracio() {
        assertEquals(IntStream.rangeClosed(1, 14).boxed().toList(),
                Main.EXEMPLES.stream().map(Main.Exemple::numero).toList());
    }

    @Test
    @DisplayName("Executa un exemple pel seu número")
    void executarUnExemple() throws Exception {
        String text = Fluxos.capturarSortida(() -> assertTrue(Main.executar(4)));
        assertTrue(text.contains("=== 4. XPath"));
        assertTrue(text.contains("Productes: 6"));
    }

    @Test
    @DisplayName("Un número que no existeix no executa res")
    void numeroInexistent() throws Exception {
        Fluxos.capturarSortida(() -> assertFalse(Main.executar(99)));
    }

    @Test
    @DisplayName("Un argument que no és un número dona un codi d'error")
    void argumentInvalid() throws Exception {
        String text = Fluxos.capturarSortida(() -> assertEquals(1, Main.executarArguments(new String[]{"hola"})));
        assertTrue(text.contains("Argument no vàlid"));
    }

    @Test
    @DisplayName("Tots els exemples s'executen sense errors i generen els seus fitxers")
    void totsElsExemples() throws Exception {
        String text = Fluxos.capturarSortida(() -> assertEquals(0, Main.executarArguments(new String[]{"tots"})));
        assertFalse(text.contains("L'exemple ha fallat"), text);
        for (String fitxer : List.of("comanda-C2001.xml", "cataleg-modificat.xml", "cataleg-gran.xml",
                "cataleg-monitors.xml", "comandes-primera-setmana.xml", "cataleg.html", "cataleg.csv",
                "comandes.json", "productes-api.xml", "resum-factura.xml")) {
            assertTrue(Files.exists(sortida.resolve(fitxer)), "Falta " + fitxer);
        }
    }
}
