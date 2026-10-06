package com.project.exemples.namespaces;

import com.project.exemples.comu.Recursos;
import com.project.exemples.comu.Sortida;
import com.project.exemples.comu.Xml;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Exemple 13. Documents amb espais de noms (namespaces), com les factures electròniques,
 * els serveis web SOAP o els fitxers d'Office.
 *
 * <p>Un espai de noms és un URI que identifica de quin vocabulari és cada element. Permet
 * barrejar en un mateix document elements amb el mateix nom però de significat diferent:
 * a la factura de mostra hi ha {@code emp:nom} (nom d'una empresa) i {@code nom} de les notes.</p>
 */
public final class EspaisDeNoms {

    /** URI de l'espai de noms de les factures. */
    public static final String NS_FACTURA = "https://exemple.cat/esquemes/factura";
    /** URI de l'espai de noms de les dades d'empresa. */
    public static final String NS_EMPRESA = "https://exemple.cat/esquemes/empresa";
    /** URI de l'espai de noms de les notes. */
    public static final String NS_NOTES = "https://exemple.cat/esquemes/notes";
    /** URI de l'espai de noms dels resums que genera aquest exemple. */
    public static final String NS_RESUM = "https://exemple.cat/esquemes/resum";

    private EspaisDeNoms() {
    }

    /**
     * Totals d'una factura.
     *
     * @param numero   número de la factura
     * @param client   nom de l'empresa receptora
     * @param base     base imposable (suma de les línies)
     * @param impostos import dels impostos
     * @param total    base més impostos
     */
    public record ResumFactura(String numero, String client, BigDecimal base, BigDecimal impostos, BigDecimal total) {
    }

    /**
     * Llegeix la factura tenint en compte els espais de noms. Sense
     * {@code setNamespaceAware(true)}, els mètodes {@code ...NS} i XPath amb prefixos no funcionen.
     *
     * @param entrada flux amb l'XML (no es tanca)
     * @return document llegit
     * @throws Exception si no es pot llegir
     */
    public static Document llegir(InputStream entrada) throws Exception {
        return Xml.llegir(entrada, true);
    }

    /**
     * {@code getElementsByTagNameNS} busca pel nom local i l'URI, sense dependre del prefix.
     *
     * @param factura document llegit amb espais de noms
     * @return noms de les empreses (emissor i receptor)
     */
    public static List<String> nomsEmpreses(Document factura) {
        NodeList noms = factura.getElementsByTagNameNS(NS_EMPRESA, "nom");
        List<String> resultat = new ArrayList<>();
        for (int i = 0; i < noms.getLength(); i++) {
            resultat.add(noms.item(i).getTextContent().trim());
        }
        return resultat;
    }

    /**
     * @return context amb els prefixos que fan servir les expressions XPath d'aquest exemple
     */
    public static ContextEspaisDeNoms contextFactura() {
        return new ContextEspaisDeNoms(Map.of("f", NS_FACTURA, "e", NS_EMPRESA, "n", NS_NOTES));
    }

    /**
     * Crea un objecte XPath que entén els prefixos {@code f}, {@code e} i {@code n}.
     *
     * @return objecte XPath
     */
    public static XPath xpathFactura() {
        XPath xpath = XPathFactory.newInstance().newXPath();
        xpath.setNamespaceContext(contextFactura());
        return xpath;
    }

    /**
     * Calcula els totals de la factura amb XPath.
     *
     * @param factura document llegit amb espais de noms
     * @return totals de la factura
     */
    public static ResumFactura calcular(Document factura) {
        XPath xpath = xpathFactura();
        try {
            String numero = xpath.evaluate("/f:factura/@numero", factura);
            String client = xpath.evaluate("/f:factura/f:receptor/e:nom", factura);
            NodeList linies = (NodeList) xpath.evaluate("/f:factura/f:linies/f:linia", factura, XPathConstants.NODESET);
            BigDecimal base = BigDecimal.ZERO;
            for (int i = 0; i < linies.getLength(); i++) {
                Element linia = (Element) linies.item(i);
                // Els atributs sense prefix no pertanyen a cap espai de noms: getAttribute funciona igual
                base = base.add(new BigDecimal(linia.getAttribute("preuUnitari"))
                        .multiply(new BigDecimal(linia.getAttribute("quantitat"))));
            }
            BigDecimal percentatge = new BigDecimal(xpath.evaluate("/f:factura/f:impostos/@percentatge", factura));
            BigDecimal impostos = base.multiply(percentatge).movePointLeft(2).setScale(2, RoundingMode.HALF_UP);
            base = base.setScale(2, RoundingMode.HALF_UP);
            return new ResumFactura(numero, client, base, impostos, base.add(impostos));
        } catch (XPathExpressionException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Crea un document nou dins d'un espai de noms propi i amb el prefix {@code res}.
     *
     * @param resum totals de la factura
     * @return document nou
     */
    public static Document crearResum(ResumFactura resum) {
        Document document = Xml.documentNou();
        Element arrel = document.createElementNS(NS_RESUM, "res:resum");
        // La declaració xmlns:res és un atribut especial que pertany a l'espai de noms XMLNS
        arrel.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns:res", NS_RESUM);
        arrel.setAttribute("factura", resum.numero());
        document.appendChild(arrel);
        afegir(document, arrel, "res:client", resum.client());
        afegir(document, arrel, "res:base", resum.base().toPlainString());
        afegir(document, arrel, "res:impostos", resum.impostos().toPlainString());
        afegir(document, arrel, "res:total", resum.total().toPlainString());
        return document;
    }

    private static void afegir(Document document, Element pare, String nomQualificat, String text) {
        Element element = document.createElementNS(NS_RESUM, nomQualificat);
        element.setTextContent(text);
        pare.appendChild(element);
    }

    /**
     * Llegeix la factura de mostra, en calcula els totals i en desa un resum.
     *
     * @param args no es fan servir
     * @throws Exception si falla la lectura o l'escriptura
     */
    public static void main(String[] args) throws Exception {
        Document factura;
        try (InputStream entrada = Recursos.dades("factura.xml")) {
            factura = llegir(entrada);
        }
        System.out.println("Empreses: " + nomsEmpreses(factura));
        System.out.println("Notes: " + xpathFactura().evaluate("/f:factura/n:notes/n:nom", factura));
        ResumFactura resum = calcular(factura);
        System.out.println("Factura " + resum.numero() + " per a " + resum.client() + ": base " + resum.base()
                + " + impostos " + resum.impostos() + " = " + resum.total() + " EUR");

        Document document = crearResum(resum);
        Path fitxer = Sortida.fitxer("resum-factura.xml");
        Xml.desar(document, fitxer);
        System.out.println("Resum desat a " + fitxer.toAbsolutePath() + ":");
        System.out.println(Xml.aText(document));
    }
}
