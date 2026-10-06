package com.project.exemples.configuracio;

/**
 * La configuració no es pot fer servir: falta un paràmetre obligatori, un valor no té el tipus
 * correcte o el fitxer no es pot llegir. És una excepció no comprovada perquè l'aplicació no
 * pot continuar sense configuració.
 */
public class ConfiguracioException extends RuntimeException {

    /**
     * @param missatge descripció del problema
     */
    public ConfiguracioException(String missatge) {
        super(missatge);
    }

    /**
     * @param missatge descripció del problema
     * @param causa    excepció original
     */
    public ConfiguracioException(String missatge, Throwable causa) {
        super(missatge, causa);
    }
}
