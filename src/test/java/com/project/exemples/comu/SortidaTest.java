package com.project.exemples.comu;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Utilitats: carpeta de sortida")
class SortidaTest {

    @TempDir
    Path temporal;

    @AfterEach
    void restaurarPropietat() {
        System.clearProperty(Sortida.PROPIETAT);
    }

    @Test
    @DisplayName("La propietat del sistema canvia la carpeta i es crea si no existeix")
    void fesServirLaPropietat() {
        Path carpeta = temporal.resolve("nova/sortida");
        System.setProperty(Sortida.PROPIETAT, carpeta.toString());
        assertEquals(carpeta, Sortida.directori());
        assertTrue(Files.isDirectory(carpeta));
    }

    @Test
    @DisplayName("Els fitxers es creen dins de la carpeta de sortida")
    void fitxerDinsDeLaCarpeta() {
        System.setProperty(Sortida.PROPIETAT, temporal.toString());
        assertEquals(temporal.resolve("resultat.xml"), Sortida.fitxer("resultat.xml"));
    }
}
