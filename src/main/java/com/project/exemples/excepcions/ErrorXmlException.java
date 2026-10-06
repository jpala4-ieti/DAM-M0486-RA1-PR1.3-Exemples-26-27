package com.project.exemples.excepcions;

import java.nio.file.Path;
import java.util.Optional;

/**
 * Excepció pròpia que agrupa qualsevol problema en llegir un fitxer XML.
 *
 * <p>Qui crida només ha de capturar un tipus d'excepció, i el missatge ja inclou el fitxer i,
 * si se sap, la línia i la columna de l'error. L'excepció original es conserva com a causa.</p>
 */
public class ErrorXmlException extends Exception {

    private final transient Path fitxer;
    private final int linia;
    private final int columna;

    /**
     * Error sense posició (per exemple, un fitxer que no existeix).
     *
     * @param fitxer   fitxer afectat (pot ser {@code null})
     * @param missatge descripció de l'error
     * @param causa    excepció original
     */
    public ErrorXmlException(Path fitxer, String missatge, Throwable causa) {
        this(fitxer, -1, -1, missatge, causa);
    }

    /**
     * Error amb posició dins del fitxer.
     *
     * @param fitxer   fitxer afectat (pot ser {@code null})
     * @param linia    línia de l'error (o -1)
     * @param columna  columna de l'error (o -1)
     * @param missatge descripció de l'error
     * @param causa    excepció original
     */
    public ErrorXmlException(Path fitxer, int linia, int columna, String missatge, Throwable causa) {
        super(compondre(fitxer, linia, missatge), causa);
        this.fitxer = fitxer;
        this.linia = linia;
        this.columna = columna;
    }

    private static String compondre(Path fitxer, int linia, String missatge) {
        StringBuilder text = new StringBuilder();
        if (fitxer != null) {
            text.append(fitxer.getFileName()).append(": ");
        }
        if (linia > 0) {
            text.append("línia ").append(linia).append(": ");
        }
        return text.append(missatge).toString();
    }

    /**
     * @return fitxer afectat, si se sap
     */
    public Optional<Path> fitxer() {
        return Optional.ofNullable(fitxer);
    }

    /**
     * @return línia de l'error, o -1 si no se sap
     */
    public int linia() {
        return linia;
    }

    /**
     * @return columna de l'error, o -1 si no se sap
     */
    public int columna() {
        return columna;
    }

    /**
     * @return {@code true} si l'error té línia i columna
     */
    public boolean tePosicio() {
        return linia > 0;
    }
}
