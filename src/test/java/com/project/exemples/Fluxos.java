package com.project.exemples;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

/**
 * Utilitats per als tests: convertir text en fluxos i capturar el que s'escriu per consola.
 */
public final class Fluxos {

    /** Codi que es vol executar mentre es captura la consola. */
    @FunctionalInterface
    public interface Accio {
        void executar() throws Exception;
    }

    private Fluxos() {
    }

    /**
     * @param text text que es vol llegir com si fos un fitxer
     * @return flux amb el text en UTF-8
     */
    public static InputStream de(String text) {
        return new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Executa l'acció i retorna el que ha escrit a System.out, amb els salts de línia unificats.
     */
    public static String capturarSortida(Accio accio) throws Exception {
        PrintStream original = System.out;
        ByteArrayOutputStream memoria = new ByteArrayOutputStream();
        try {
            System.setOut(new PrintStream(memoria, true, StandardCharsets.UTF_8));
            accio.executar();
        } finally {
            System.setOut(original);
        }
        return memoria.toString(StandardCharsets.UTF_8).replace("\r\n", "\n");
    }

    /**
     * Executa l'acció i retorna el que ha escrit a System.err.
     */
    public static String capturarErrors(Accio accio) throws Exception {
        PrintStream original = System.err;
        ByteArrayOutputStream memoria = new ByteArrayOutputStream();
        try {
            System.setErr(new PrintStream(memoria, true, StandardCharsets.UTF_8));
            accio.executar();
        } finally {
            System.setErr(original);
        }
        return memoria.toString(StandardCharsets.UTF_8);
    }

    /**
     * @param text text amb salts de línia de qualsevol sistema
     * @return el mateix text amb salts de línia {@code \n}
     */
    public static String unificarSalts(String text) {
        return text.replace("\r\n", "\n");
    }
}
