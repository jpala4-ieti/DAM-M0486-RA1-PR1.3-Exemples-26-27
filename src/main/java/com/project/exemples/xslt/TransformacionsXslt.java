package com.project.exemples.xslt;

import com.project.exemples.comu.Recursos;
import com.project.exemples.comu.Sortida;
import com.project.exemples.seguretat.ParsersSegurs;

import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Exemple 10. Transformació d'XML amb XSLT: el mateix catàleg es converteix en una pàgina
 * HTML i en un fitxer CSV, sense escriure cap bucle en Java.
 *
 * <p>Les fulles d'estil són a {@code src/main/resources/xslt}. Canviar el format de sortida
 * només vol dir canviar la fulla d'estil, no el programa.</p>
 */
public final class TransformacionsXslt {

    /**
     * Templates és una fulla d'estil ja compilada. Compilar-la és el pas costós; després es pot
     * reutilitzar tantes vegades com calgui i des de diversos fils.
     */
    private final Templates plantillaHtml;
    private final Templates plantillaCsv;

    /**
     * Compila les dues fulles d'estil del catàleg.
     */
    public TransformacionsXslt() {
        this.plantillaHtml = compilar("/xslt/cataleg-a-html.xsl");
        this.plantillaCsv = compilar("/xslt/cataleg-a-csv.xsl");
    }

    private static Templates compilar(String ruta) {
        try (InputStream xsl = Recursos.obrir(ruta)) {
            return ParsersSegurs.fabricaTransformerSegura()
                    .newTemplates(new StreamSource(xsl, Recursos.url(ruta).toExternalForm()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (TransformerConfigurationException e) {
            throw new IllegalStateException("La fulla d'estil " + ruta + " no és vàlida", e);
        }
    }

    /**
     * Converteix el catàleg en una pàgina HTML.
     *
     * @param cataleg flux amb el catàleg (no es tanca)
     * @param titol   títol de la pàgina (es passa com a paràmetre a la fulla d'estil)
     * @return codi HTML
     * @throws TransformerException si falla la transformació
     */
    public String catalegAHtml(InputStream cataleg, String titol) throws TransformerException {
        return aplicar(plantillaHtml, cataleg, Map.of("titol", titol));
    }

    /**
     * Converteix el catàleg en CSV, amb punt i coma com a separador.
     *
     * @param cataleg flux amb el catàleg (no es tanca)
     * @return text CSV
     * @throws TransformerException si falla la transformació
     */
    public String catalegACsv(InputStream cataleg) throws TransformerException {
        return catalegACsv(cataleg, ";");
    }

    /**
     * Converteix el catàleg en CSV amb el separador indicat.
     *
     * @param cataleg   flux amb el catàleg (no es tanca)
     * @param separador separador de camps
     * @return text CSV
     * @throws TransformerException si falla la transformació
     */
    public String catalegACsv(InputStream cataleg, String separador) throws TransformerException {
        return aplicar(plantillaCsv, cataleg, Map.of("separador", separador));
    }

    /**
     * Aplica una fulla d'estil qualsevol (que no cal compilar abans) a un XML.
     *
     * @param xml        flux amb l'XML (no es tanca)
     * @param xsl        flux amb la fulla d'estil (no es tanca)
     * @param parametres paràmetres de la fulla d'estil
     * @return resultat de la transformació
     * @throws TransformerException si la fulla d'estil no és vàlida o falla la transformació
     */
    public static String transformar(InputStream xml, InputStream xsl, Map<String, Object> parametres) throws TransformerException {
        Templates plantilla = ParsersSegurs.fabricaTransformerSegura().newTemplates(new StreamSource(xsl));
        return aplicar(plantilla, xml, parametres);
    }

    private static String aplicar(Templates plantilla, InputStream xml, Map<String, Object> parametres) throws TransformerException {
        // Cada transformació necessita el seu Transformer: no són segurs entre fils
        Transformer transformador = plantilla.newTransformer();
        parametres.forEach(transformador::setParameter);
        StringWriter resultat = new StringWriter();
        transformador.transform(new StreamSource(xml), new StreamResult(resultat));
        return resultat.toString();
    }

    /**
     * Genera el catàleg en HTML i en CSV a la carpeta de sortida.
     *
     * @param args no es fan servir
     * @throws Exception si falla alguna transformació
     */
    public static void main(String[] args) throws Exception {
        TransformacionsXslt transformacions = new TransformacionsXslt();

        Path html = Sortida.fitxer("cataleg.html");
        try (InputStream cataleg = Recursos.dades("cataleg.xml")) {
            Files.writeString(html, transformacions.catalegAHtml(cataleg, "Catàleg de tardor"));
        }
        System.out.println("Pàgina HTML generada: " + html.toAbsolutePath() + " (obre-la amb el navegador)");

        Path csv = Sortida.fitxer("cataleg.csv");
        try (InputStream cataleg = Recursos.dades("cataleg.xml")) {
            Files.writeString(csv, transformacions.catalegACsv(cataleg));
        }
        System.out.println("Fitxer CSV generat: " + csv.toAbsolutePath());
        System.out.println(Files.readString(csv));
    }
}
