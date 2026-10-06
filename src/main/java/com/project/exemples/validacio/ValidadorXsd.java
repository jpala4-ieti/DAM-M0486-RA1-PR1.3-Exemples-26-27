package com.project.exemples.validacio;

import com.project.exemples.comu.Recursos;
import com.project.exemples.seguretat.ParsersSegurs;
import org.w3c.dom.Document;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.Validator;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Exemple 9. Validació d'un XML contra un esquema XSD.
 *
 * <p>Que un XML estigui ben format només vol dir que les etiquetes s'obren i es tanquen bé.
 * L'esquema comprova que sigui <b>vàlid</b>: que tingui els elements esperats, en l'ordre correcte
 * i amb els tipus de dades correctes. És el primer que es fa a la feina amb un XML que arriba d'un altre sistema.</p>
 */
public final class ValidadorXsd {

    /** Gravetat d'un problema de validació. */
    public enum Gravetat {
        /** No impedeix considerar el document vàlid. */
        AVIS,
        /** El document no compleix l'esquema. */
        ERROR,
        /** El document no està ben format i no es pot continuar validant. */
        ERROR_GREU
    }

    /**
     * Un problema trobat durant la validació.
     *
     * @param gravetat gravetat del problema
     * @param linia    línia (o -1 si no se sap, per exemple en validar un document DOM)
     * @param columna  columna (o -1)
     * @param missatge descripció del validador
     */
    public record ErrorValidacio(Gravetat gravetat, int linia, int columna, String missatge) {

        @Override
        public String toString() {
            return "[" + gravetat + "] línia " + linia + ", columna " + columna + ": " + missatge;
        }
    }

    /** Un Schema es compila un sol cop i es pot fer servir des de diversos fils. */
    private final Schema esquema;

    /**
     * @param urlEsquema URL de l'esquema XSD
     */
    public ValidadorXsd(URL urlEsquema) {
        this.esquema = compilar(urlEsquema);
    }

    /**
     * @return validador per a l'esquema de les comandes ({@code comanda.xsd})
     */
    public static ValidadorXsd perComandes() {
        return new ValidadorXsd(Recursos.url("/esquemes/comanda.xsd"));
    }

    private static Schema compilar(URL urlEsquema) {
        try (InputStream entrada = urlEsquema.openStream()) {
            return ParsersSegurs.fabricaEsquemesSegura().newSchema(new StreamSource(entrada, urlEsquema.toExternalForm()));
        } catch (IOException e) {
            throw new UncheckedIOException("No es pot llegir l'esquema " + urlEsquema, e);
        } catch (SAXException e) {
            throw new IllegalArgumentException("L'esquema no és vàlid: " + e.getMessage(), e);
        }
    }

    /**
     * Valida un XML i retorna <b>tots</b> els problemes trobats, no només el primer.
     *
     * @param xml flux amb l'XML (no es tanca)
     * @return llista de problemes (buida si el document és vàlid)
     * @throws IOException si no es pot llegir el flux
     */
    public List<ErrorValidacio> validar(InputStream xml) throws IOException {
        return validar(new StreamSource(xml));
    }

    /**
     * Valida un document que ja és en memòria. En aquest cas el validador no sap les línies.
     *
     * @param document document DOM
     * @return llista de problemes (buida si el document és vàlid)
     * @throws IOException no passa amb un document en memòria, però ho exigeix l'API
     */
    public List<ErrorValidacio> validar(Document document) throws IOException {
        return validar(new DOMSource(document));
    }

    /**
     * @param xml flux amb l'XML (no es tanca)
     * @return {@code true} si no hi ha cap error (els avisos no compten)
     * @throws IOException si no es pot llegir el flux
     */
    public boolean esValid(InputStream xml) throws IOException {
        return validar(xml).stream().noneMatch(error -> error.gravetat() != Gravetat.AVIS);
    }

    private List<ErrorValidacio> validar(Source font) throws IOException {
        Validator validador = esquema.newValidator();
        try {
            validador.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            validador.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        } catch (SAXException e) {
            throw new IllegalStateException("El validador no admet la configuració segura", e);
        }
        List<ErrorValidacio> errors = new ArrayList<>();
        validador.setErrorHandler(new ErrorHandler() {
            @Override
            public void warning(SAXParseException e) {
                errors.add(convertir(Gravetat.AVIS, e));
            }

            @Override
            public void error(SAXParseException e) {
                // No es llança l'excepció: així el validador continua i es recullen tots els errors
                errors.add(convertir(Gravetat.ERROR, e));
            }

            @Override
            public void fatalError(SAXParseException e) throws SAXException {
                // Un XML mal format no es pot continuar llegint
                errors.add(convertir(Gravetat.ERROR_GREU, e));
                throw e;
            }
        });
        try {
            validador.validate(font);
        } catch (SAXParseException e) {
            // Ja s'ha afegit a la llista des de fatalError
        } catch (SAXException e) {
            errors.add(new ErrorValidacio(Gravetat.ERROR_GREU, -1, -1, e.getMessage()));
        }
        return errors;
    }

    private static ErrorValidacio convertir(Gravetat gravetat, SAXParseException e) {
        return new ErrorValidacio(gravetat, e.getLineNumber(), e.getColumnNumber(), e.getMessage());
    }

    /**
     * Valida les dues comandes de mostra.
     *
     * @param args no es fan servir
     * @throws IOException si no es poden llegir
     */
    public static void main(String[] args) throws IOException {
        ValidadorXsd validador = perComandes();
        for (String nom : List.of("comanda-valida.xml", "comanda-invalida.xml")) {
            try (InputStream entrada = Recursos.dades(nom)) {
                List<ErrorValidacio> errors = validador.validar(entrada);
                System.out.println(nom + (errors.isEmpty() ? ": vàlida" : ": " + errors.size() + " problemes"));
                errors.forEach(error -> System.out.println("  " + error));
            }
        }
    }
}
