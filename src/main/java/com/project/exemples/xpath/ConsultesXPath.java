package com.project.exemples.xpath;

import com.project.exemples.comu.Recursos;
import com.project.exemples.comu.Xml;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.namespace.QName;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Exemple 4. Consultes XPath sobre el catàleg.
 *
 * <p>Mostra els cinc tipus de resultat (NODESET, NODE, STRING, NUMBER i BOOLEAN), predicats,
 * funcions, eixos, expressions relatives i variables. Totes les consultes són de lectura.</p>
 */
public final class ConsultesXPath {

    private static final String MAJUSCULES = "ABCDEFGHIJKLMNOPQRSTUVWXYZÀÈÉÍÏÒÓÚÜÇ";
    private static final String MINUSCULES = "abcdefghijklmnopqrstuvwxyzàèéíïòóúüç";

    private final Document document;
    private final XPath xpath;

    /**
     * @param document catàleg sobre el qual es faran les consultes
     */
    public ConsultesXPath(Document document) {
        this.document = document;
        this.xpath = XPathFactory.newInstance().newXPath();
    }

    /**
     * NUMBER: count() retorna sempre un Double, encara que el resultat sigui enter.
     *
     * @return nombre de productes
     */
    public int comptarProductes() {
        return ((Double) avaluar("count(/cataleg/producte)", XPathConstants.NUMBER)).intValue();
    }

    /**
     * NUMBER amb sum(): suma els valors de tots els nodes seleccionats.
     *
     * @return unitats totals en estoc
     */
    public int unitatsEnEstoc() {
        return ((Double) avaluar("sum(/cataleg/producte/estoc)", XPathConstants.NUMBER)).intValue();
    }

    /**
     * NUMBER amb una operació aritmètica entre dues funcions.
     *
     * @return preu mitjà, arrodonit a dos decimals
     */
    public BigDecimal preuMitja() {
        Double mitjana = (Double) avaluar("sum(/cataleg/producte/preu) div count(/cataleg/producte)", XPathConstants.NUMBER);
        return BigDecimal.valueOf(mitjana).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * BOOLEAN: un conjunt de nodes és cert si no és buit.
     *
     * @return {@code true} si algun producte té l'estoc a zero
     */
    public boolean hiHaProductesSenseEstoc() {
        return (Boolean) avaluar("boolean(/cataleg/producte[estoc = 0])", XPathConstants.BOOLEAN);
    }

    /**
     * STRING amb una variable. El valor s'envia a part i XPath el tracta sempre com un valor,
     * mai com a codi: així no hi pot haver injeccions.
     *
     * @param id identificador del producte
     * @return nom del producte, si existeix
     */
    public Optional<String> nomDelProducte(String id) {
        String nom = (String) avaluarAmbVariables("/cataleg/producte[@id = $id]/nom",
                Map.of("id", id), XPathConstants.STRING);
        return nom.isEmpty() ? Optional.empty() : Optional.of(nom);
    }

    /**
     * <b>Exemple del que NO s'ha de fer</b>: construir l'expressió concatenant el text que
     * arriba de fora. Si l'id és {@code x' or '1'='1}, la consulta retorna un producte qualsevol.
     *
     * @param id identificador del producte
     * @return nom del producte, si existeix
     */
    public Optional<String> nomDelProducteInsegur(String id) {
        String nom = (String) avaluar("/cataleg/producte[@id = '" + id + "']/nom", XPathConstants.STRING);
        return nom.isEmpty() ? Optional.empty() : Optional.of(nom);
    }

    /**
     * NODESET amb un predicat numèric. Les variables numèriques es passen com a Double.
     *
     * @param preuMinim preu a partir del qual es busca (sense incloure'l)
     * @return identificadors dels productes més cars que el preu indicat
     */
    public List<String> productesMesCarsQue(BigDecimal preuMinim) {
        NodeList ids = (NodeList) avaluarAmbVariables("/cataleg/producte[preu > $minim]/@id",
                Map.of("minim", preuMinim.doubleValue()), XPathConstants.NODESET);
        return textos(ids);
    }

    /**
     * Cerca sense distingir majúscules: XPath 1.0 no té lower-case(), però translate() substitueix
     * cada caràcter de la primera llista pel de la mateixa posició de la segona.
     *
     * @param text text que es busca dins del nom
     * @return noms dels productes que el contenen
     */
    public List<String> cercarPerNom(String text) {
        NodeList noms = (NodeList) avaluarAmbVariables(
                "/cataleg/producte/nom[contains(translate(., $majuscules, $minuscules), $text)]",
                Map.of("majuscules", MAJUSCULES, "minuscules", MINUSCULES, "text", text.toLowerCase(Locale.ROOT)),
                XPathConstants.NODESET);
        return textos(noms);
    }

    /**
     * El màxim sense ordenar: el producte que no té cap altre producte amb un preu més alt.
     *
     * @return nom del producte més car
     */
    public String nomDelProducteMesCar() {
        return (String) avaluar("/cataleg/producte[not(preu < /cataleg/producte/preu)]/nom", XPathConstants.STRING);
    }

    /**
     * Valors diferents d'un atribut (com un DISTINCT de SQL), fent servir l'eix preceding-sibling.
     *
     * @return categories, en l'ordre en què apareixen per primer cop
     */
    public List<String> categories() {
        NodeList categories = (NodeList) avaluar(
                "/cataleg/producte[not(@categoria = preceding-sibling::producte/@categoria)]/@categoria",
                XPathConstants.NODESET);
        return textos(categories);
    }

    /**
     * Expressions relatives: es compilen un cop i s'avaluen des de cada node producte.
     *
     * @return nombre d'etiquetes de cada producte, per identificador
     */
    public Map<String, Integer> etiquetesPerProducte() {
        NodeList productes = (NodeList) avaluar("/cataleg/producte", XPathConstants.NODESET);
        Map<String, Integer> resultat = new LinkedHashMap<>();
        try {
            // Una expressió compilada es pot reutilitzar tantes vegades com calgui
            XPathExpression id = xpath.compile("@id");
            XPathExpression nombreEtiquetes = xpath.compile("count(etiquetes/etiqueta)");
            for (int i = 0; i < productes.getLength(); i++) {
                Node producte = productes.item(i);
                resultat.put(id.evaluate(producte),
                        ((Double) nombreEtiquetes.evaluate(producte, XPathConstants.NUMBER)).intValue());
            }
        } catch (XPathExpressionException e) {
            throw new IllegalStateException(e);
        }
        return resultat;
    }

    /**
     * Comparació entre conjunts de nodes: {@code A = B} és cert si algun valor d'A coincideix amb algun de B.
     *
     * @param id identificador del producte de referència
     * @return identificadors dels altres productes que comparteixen alguna etiqueta amb ell
     */
    public List<String> productesAmbEtiquetaComuna(String id) {
        NodeList ids = (NodeList) avaluarAmbVariables(
                "/cataleg/producte[@id != $id][etiquetes/etiqueta = /cataleg/producte[@id = $id]/etiquetes/etiqueta]/@id",
                Map.of("id", id), XPathConstants.NODESET);
        return textos(ids);
    }

    /**
     * Funcions de posició: position() i last().
     *
     * @param n quants productes es volen
     * @return identificadors dels últims n productes del catàleg
     */
    public List<String> ultimsProductes(int n) {
        NodeList ids = (NodeList) avaluarAmbVariables("/cataleg/producte[position() > last() - $n]/@id",
                Map.of("n", (double) n), XPathConstants.NODESET);
        return textos(ids);
    }

    /**
     * Avalua qualsevol expressió que retorni nodes i en retorna el text.
     *
     * @param expressio expressió XPath
     * @return text de cada node seleccionat
     */
    public List<String> textosDe(String expressio) {
        return textos((NodeList) avaluar(expressio, XPathConstants.NODESET));
    }

    private Object avaluar(String expressio, QName tipus) {
        try {
            return xpath.evaluate(expressio, document, tipus);
        } catch (XPathExpressionException e) {
            throw new IllegalArgumentException("Expressió XPath no vàlida: " + expressio, e);
        }
    }

    private Object avaluarAmbVariables(String expressio, Map<String, Object> variables, QName tipus) {
        // Cada consulta té el seu propi objecte XPath amb un resolutor de variables
        XPath xpathAmbVariables = XPathFactory.newInstance().newXPath();
        xpathAmbVariables.setXPathVariableResolver(nom -> variables.get(nom.getLocalPart()));
        try {
            return xpathAmbVariables.evaluate(expressio, document, tipus);
        } catch (XPathExpressionException e) {
            throw new IllegalArgumentException("Expressió XPath no vàlida: " + expressio, e);
        }
    }

    private static List<String> textos(NodeList nodes) {
        List<String> textos = new ArrayList<>();
        for (int i = 0; i < nodes.getLength(); i++) {
            textos.add(nodes.item(i).getTextContent().trim());
        }
        return textos;
    }

    /**
     * Executa totes les consultes sobre el catàleg de mostra.
     *
     * @param args no es fan servir
     * @throws Exception si no es pot llegir el catàleg
     */
    public static void main(String[] args) throws Exception {
        Document document;
        try (InputStream entrada = Recursos.dades("cataleg.xml")) {
            document = Xml.llegir(entrada);
        }
        ConsultesXPath consultes = new ConsultesXPath(document);
        System.out.println("Productes: " + consultes.comptarProductes());
        System.out.println("Unitats en estoc: " + consultes.unitatsEnEstoc());
        System.out.println("Preu mitjà: " + consultes.preuMitja() + " EUR");
        System.out.println("Hi ha productes sense estoc? " + (consultes.hiHaProductesSenseEstoc() ? "sí" : "no"));
        System.out.println("Nom del producte P003: " + consultes.nomDelProducte("P003").orElse("(no existeix)"));
        System.out.println("Productes de més de 100 EUR: " + consultes.productesMesCarsQue(new BigDecimal("100")));
        System.out.println("Noms que contenen \"monitor\": " + consultes.cercarPerNom("monitor"));
        System.out.println("Producte més car: " + consultes.nomDelProducteMesCar());
        System.out.println("Categories: " + consultes.categories());
        System.out.println("Etiquetes per producte: " + consultes.etiquetesPerProducte());
        System.out.println("Productes amb alguna etiqueta en comú amb P001: " + consultes.productesAmbEtiquetaComuna("P001"));
        System.out.println("Últims dos productes: " + consultes.ultimsProductes(2));

        String atac = "x' or '1'='1";
        System.out.println("\nInjecció XPath amb l'id \"" + atac + "\":");
        System.out.println("- Consulta insegura (concatenant): " + consultes.nomDelProducteInsegur(atac).orElse("(cap resultat)"));
        System.out.println("- Consulta segura (amb variable): " + consultes.nomDelProducte(atac).orElse("(cap resultat)"));
    }
}
