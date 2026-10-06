package com.project.exemples.comu;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Carpeta on els exemples escriuen els fitxers que generen.
 *
 * <p>Per defecte és {@code sortida}, dins del directori de treball. Es pot canviar amb la
 * propietat del sistema {@value #PROPIETAT}: els tests la fan servir per escriure en una
 * carpeta temporal i no deixar fitxers dins del projecte.</p>
 */
public final class Sortida {

    /** Nom de la propietat del sistema que indica la carpeta de sortida. */
    public static final String PROPIETAT = "exemples.sortida";

    private Sortida() {
    }

    /**
     * Retorna la carpeta de sortida i la crea si encara no existeix.
     *
     * @return carpeta de sortida
     */
    public static Path directori() {
        Path directori = Path.of(System.getProperty(PROPIETAT, "sortida"));
        try {
            Files.createDirectories(directori);
        } catch (IOException e) {
            throw new UncheckedIOException("No es pot crear la carpeta de sortida " + directori, e);
        }
        return directori;
    }

    /**
     * Retorna la ruta d'un fitxer dins de la carpeta de sortida.
     *
     * @param nom nom del fitxer
     * @return ruta completa del fitxer
     */
    public static Path fitxer(String nom) {
        return directori().resolve(nom);
    }
}
