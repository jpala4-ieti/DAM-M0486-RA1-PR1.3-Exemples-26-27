package com.project.exemples.configuracio;

import java.time.Duration;
import java.util.List;

/**
 * Configuració de l'aplicació, ja convertida als tipus Java adequats.
 *
 * @param nomAplicacio        nom de l'aplicació
 * @param entorn              entorn d'execució (desenvolupament, proves, produccio...)
 * @param depuracio           si s'ha d'activar el mode de depuració
 * @param urlBaseDades        URL JDBC de la base de dades
 * @param usuariBaseDades     usuari de la base de dades
 * @param connexionsMaximes   mida màxima del conjunt de connexions
 * @param tempsEspera         temps màxim d'espera d'una connexió
 * @param correuActivat       si s'envien correus
 * @param servidorCorreu      servidor SMTP
 * @param portCorreu          port del servidor SMTP
 * @param idiomes             idiomes disponibles
 * @param idiomaPredeterminat idioma per defecte
 */
public record ConfiguracioBotiga(
        String nomAplicacio,
        String entorn,
        boolean depuracio,
        String urlBaseDades,
        String usuariBaseDades,
        int connexionsMaximes,
        Duration tempsEspera,
        boolean correuActivat,
        String servidorCorreu,
        int portCorreu,
        List<String> idiomes,
        String idiomaPredeterminat) {

    /**
     * Fa una còpia immutable de la llista d'idiomes.
     */
    public ConfiguracioBotiga {
        idiomes = List.copyOf(idiomes);
    }
}
