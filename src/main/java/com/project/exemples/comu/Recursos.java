package com.project.exemples.comu;

import java.io.InputStream;
import java.net.URL;

/**
 * Accés als fitxers de mostra que van dins del jar (carpeta {@code src/main/resources}).
 *
 * <p>Com que els fitxers formen part del classpath, els exemples funcionen igual des de l'IDE,
 * amb Maven o amb {@code java -jar}, sigui quin sigui el directori de treball.</p>
 */
public final class Recursos {

    private Recursos() {
    }

    /**
     * Obre un fitxer de mostra de la carpeta {@code dades}. Qui el crida l'ha de tancar
     * (millor amb un try-with-resources).
     *
     * @param nom nom del fitxer, per exemple {@code "cataleg.xml"}
     * @return flux de lectura del fitxer
     */
    public static InputStream dades(String nom) {
        return obrir("/dades/" + nom);
    }

    /**
     * Obre qualsevol recurs del classpath a partir de la seva ruta absoluta.
     *
     * @param ruta ruta del recurs, per exemple {@code "/xslt/cataleg-a-html.xsl"}
     * @return flux de lectura del recurs
     */
    public static InputStream obrir(String ruta) {
        InputStream entrada = Recursos.class.getResourceAsStream(ruta);
        if (entrada == null) {
            throw new IllegalArgumentException("No s'ha trobat el recurs " + ruta);
        }
        return entrada;
    }

    /**
     * Retorna la URL d'un recurs. Els esquemes i les fulles d'estil la necessiten com a
     * identificador ({@code systemId}) per resoldre referències relatives i per als missatges d'error.
     *
     * @param ruta ruta del recurs
     * @return URL del recurs (dins del jar o del sistema de fitxers)
     */
    public static URL url(String ruta) {
        URL url = Recursos.class.getResource(ruta);
        if (url == null) {
            throw new IllegalArgumentException("No s'ha trobat el recurs " + ruta);
        }
        return url;
    }
}
