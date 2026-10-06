package com.project.exemples.sax;

import com.project.exemples.comu.Recursos;
import com.project.exemples.seguretat.ParsersSegurs;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Exemple 5. Lectura amb SAX: el parser recorre el fitxer una sola vegada i avisa el programa
 * de cada esdeveniment (obertura d'etiqueta, text, tancament). No es crea cap arbre en memòria,
 * per això SAX serveix per a fitxers de qualsevol mida.
 */
public final class EstadistiquesSax {

    /** Categoria que es fa servir quan un producte no té l'atribut categoria. */
    public static final String SENSE_CATEGORIA = "(sense categoria)";

    private EstadistiquesSax() {
    }

    /**
     * Resultat de l'anàlisi del catàleg.
     *
     * @param productes             nombre de productes
     * @param productesPerCategoria productes de cada categoria, en ordre d'aparició
     * @param unitatsEstoc          unitats totals en estoc
     * @param producteMesCar        nom del producte més car (o {@code null} si no n'hi ha cap)
     * @param preuMitja             preu mitjà amb dos decimals
     */
    public record Estadistiques(int productes, Map<String, Integer> productesPerCategoria, int unitatsEstoc,
                                String producteMesCar, BigDecimal preuMitja) {
    }

    /**
     * Resultat d'una cerca.
     *
     * @param nom             nom del producte trobat, o {@code null}
     * @param elementsLlegits quants elements s'han llegit abans d'aturar la lectura
     */
    public record ResultatCerca(String nom, int elementsLlegits) {

        /**
         * @return {@code true} si s'ha trobat el producte
         */
        public boolean trobat() {
            return nom != null;
        }
    }

    /**
     * Calcula estadístiques del catàleg en una sola passada.
     *
     * @param entrada flux amb el catàleg (no es tanca)
     * @return estadístiques calculades
     * @throws SAXException si l'XML no està ben format
     * @throws IOException  si no es pot llegir
     */
    public static Estadistiques analitzar(InputStream entrada) throws SAXException, IOException {
        GestorEstadistiques gestor = new GestorEstadistiques();
        crearParser().parse(entrada, gestor);
        return gestor.resultat();
    }

    /**
     * Cerca el nom d'un producte i atura la lectura tan bon punt el troba, sense llegir la resta del fitxer.
     *
     * @param entrada flux amb el catàleg (no es tanca)
     * @param id      identificador del producte
     * @return resultat de la cerca
     * @throws SAXException si l'XML no està ben format
     * @throws IOException  si no es pot llegir
     */
    public static ResultatCerca cercarNom(InputStream entrada, String id) throws SAXException, IOException {
        CercadorProducte cercador = new CercadorProducte(id);
        try {
            crearParser().parse(entrada, cercador);
        } catch (CercaAcabada e) {
            // No és cap error: és la manera d'aturar SAX abans d'arribar al final
            return new ResultatCerca(e.nom, cercador.elementsLlegits);
        }
        return new ResultatCerca(null, cercador.elementsLlegits);
    }

    private static SAXParser crearParser() throws SAXException {
        try {
            return ParsersSegurs.fabricaSaxSegura().newSAXParser();
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException("No es pot crear el parser SAX", e);
        }
    }

    /**
     * Gestor que acumula les dades mentre el parser avança pel fitxer.
     * Com que SAX no guarda res, el gestor ha de recordar en quin producte es troba.
     */
    private static final class GestorEstadistiques extends DefaultHandler {

        private final StringBuilder text = new StringBuilder();
        private final Map<String, Integer> perCategoria = new LinkedHashMap<>();
        private int productes;
        private int unitats;
        private BigDecimal sumaPreus = BigDecimal.ZERO;
        private BigDecimal preuMaxim;
        private String nomMesCar;
        private String nomActual;
        private BigDecimal preuActual;

        @Override
        public void startElement(String uri, String nomLocal, String nomQualificat, Attributes atributs) {
            text.setLength(0);
            if (nomQualificat.equals("producte")) {
                productes++;
                // getValue retorna null si l'atribut no hi és
                String categoria = atributs.getValue("categoria");
                perCategoria.merge(categoria == null ? SENSE_CATEGORIA : categoria, 1, Integer::sum);
                nomActual = null;
                preuActual = null;
            }
        }

        @Override
        public void characters(char[] caracters, int inici, int longitud) {
            // El text d'un element pot arribar en diversos trossos: s'ha d'anar acumulant
            text.append(caracters, inici, longitud);
        }

        @Override
        public void endElement(String uri, String nomLocal, String nomQualificat) {
            String valor = text.toString().trim();
            switch (nomQualificat) {
                case "nom" -> nomActual = valor;
                case "preu" -> {
                    preuActual = new BigDecimal(valor);
                    sumaPreus = sumaPreus.add(preuActual);
                }
                case "estoc" -> unitats += Integer.parseInt(valor);
                case "producte" -> {
                    if (preuActual != null && (preuMaxim == null || preuActual.compareTo(preuMaxim) > 0)) {
                        preuMaxim = preuActual;
                        nomMesCar = nomActual;
                    }
                }
                default -> {
                    // La resta d'elements no interessen per a les estadístiques
                }
            }
            text.setLength(0);
        }

        Estadistiques resultat() {
            BigDecimal mitjana = productes == 0
                    ? BigDecimal.ZERO.setScale(2)
                    : sumaPreus.divide(BigDecimal.valueOf(productes), 2, RoundingMode.HALF_UP);
            return new Estadistiques(productes, Collections.unmodifiableMap(new LinkedHashMap<>(perCategoria)),
                    unitats, nomMesCar, mitjana);
        }
    }

    /**
     * Gestor que busca un producte i llança una excepció per aturar el parser quan el troba.
     */
    private static final class CercadorProducte extends DefaultHandler {

        private final String idBuscat;
        private final StringBuilder text = new StringBuilder();
        private boolean dinsDelProducte;
        private int elementsLlegits;

        CercadorProducte(String idBuscat) {
            this.idBuscat = idBuscat;
        }

        @Override
        public void startElement(String uri, String nomLocal, String nomQualificat, Attributes atributs) {
            elementsLlegits++;
            text.setLength(0);
            if (nomQualificat.equals("producte")) {
                dinsDelProducte = idBuscat.equals(atributs.getValue("id"));
            }
        }

        @Override
        public void characters(char[] caracters, int inici, int longitud) {
            text.append(caracters, inici, longitud);
        }

        @Override
        public void endElement(String uri, String nomLocal, String nomQualificat) throws SAXException {
            if (dinsDelProducte && nomQualificat.equals("nom")) {
                throw new CercaAcabada(text.toString().trim());
            }
        }
    }

    /** Excepció que fa servir el cercador per aturar el parser quan ja té el resultat. */
    private static final class CercaAcabada extends SAXException {

        private final String nom;

        CercaAcabada(String nom) {
            super("Producte trobat");
            this.nom = nom;
        }
    }

    /**
     * Analitza el catàleg de mostra.
     *
     * @param args no es fan servir
     * @throws Exception si no es pot llegir el catàleg
     */
    public static void main(String[] args) throws Exception {
        try (InputStream entrada = Recursos.dades("cataleg.xml")) {
            Estadistiques estadistiques = analitzar(entrada);
            System.out.println("Productes: " + estadistiques.productes());
            System.out.println("Per categoria: " + estadistiques.productesPerCategoria());
            System.out.println("Unitats en estoc: " + estadistiques.unitatsEstoc());
            System.out.println("Producte més car: " + estadistiques.producteMesCar());
            System.out.println("Preu mitjà: " + estadistiques.preuMitja() + " EUR");
        }
        try (InputStream entrada = Recursos.dades("cataleg.xml")) {
            ResultatCerca cerca = cercarNom(entrada, "P002");
            System.out.println("Cerca de P002: " + cerca.nom() + " (s'han llegit només " + cerca.elementsLlegits() + " elements)");
        }
    }
}
