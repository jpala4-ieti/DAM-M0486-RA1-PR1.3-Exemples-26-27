package com.project.exemples.configuracio;

import com.project.exemples.comu.Recursos;
import com.project.exemples.comu.Xml;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Exemple 14. Lectura d'un fitxer de configuració en XML, com els {@code pom.xml},
 * {@code web.xml} o {@code log4j2.xml} que es troben a qualsevol projecte Java.
 *
 * <p>Patrons habituals que s'hi mostren:</p>
 * <ul>
 *     <li>cada paràmetre s'identifica amb un camí relatiu (per exemple {@code baseDades/url});</li>
 *     <li>els paràmetres opcionals tenen un valor per defecte;</li>
 *     <li>els valors es converteixen al tipus correcte i, si no es pot, l'error diu quin paràmetre falla;</li>
 *     <li>els valors es poden sobreescriure des de fora (variables d'entorn, línia d'ordres...).</li>
 * </ul>
 */
public final class LectorConfiguracio {

    /** Versió més alta del format de configuració que entén aquest programa. */
    public static final int VERSIO_SUPORTADA = 2;

    private final Element arrel;
    private final XPath xpath;
    private final Map<String, String> sobreescriptures;

    private LectorConfiguracio(Document document, Map<String, String> sobreescriptures) {
        this.arrel = document.getDocumentElement();
        this.xpath = XPathFactory.newInstance().newXPath();
        this.sobreescriptures = Map.copyOf(sobreescriptures);
    }

    /**
     * Llegeix la configuració d'un XML.
     *
     * @param entrada flux amb l'XML (no es tanca)
     * @return configuració
     */
    public static ConfiguracioBotiga llegir(InputStream entrada) {
        return llegir(entrada, Map.of());
    }

    /**
     * Llegeix la configuració d'un XML. Els valors de {@code sobreescriptures} tenen prioritat
     * sobre els del fitxer (la clau és el camí del paràmetre, per exemple {@code baseDades/url}).
     *
     * @param entrada          flux amb l'XML (no es tanca)
     * @param sobreescriptures valors que substitueixen els del fitxer
     * @return configuració
     */
    public static ConfiguracioBotiga llegir(InputStream entrada, Map<String, String> sobreescriptures) {
        Document document;
        try {
            document = Xml.llegir(entrada);
        } catch (IOException | SAXException e) {
            throw new ConfiguracioException("No es pot llegir la configuració: " + e.getMessage(), e);
        }
        LectorConfiguracio lector = new LectorConfiguracio(document, sobreescriptures);
        int versio = lector.enter("@versio", 1);
        if (versio > VERSIO_SUPORTADA) {
            throw new ConfiguracioException("Versió de configuració no suportada: " + versio
                    + " (com a màxim " + VERSIO_SUPORTADA + ")");
        }
        return new ConfiguracioBotiga(
                lector.textObligatori("aplicacio/nom"),
                lector.text("aplicacio/entorn", "desenvolupament"),
                lector.boolea("aplicacio/depuracio", false),
                lector.textObligatori("baseDades/url"),
                lector.text("baseDades/usuari", ""),
                lector.enter("baseDades/connexionsMaximes", 5),
                lector.durada("baseDades/tempsEspera", Duration.ofSeconds(10)),
                lector.boolea("correu/@activat", false),
                lector.text("correu/servidor", "localhost"),
                lector.enter("correu/port", 25),
                lector.llista("idiomes/idioma"),
                lector.text("idiomes/idioma[@predeterminat='true']", "ca"));
    }

    /**
     * @param cami       camí del paràmetre, relatiu a l'element arrel
     * @param perDefecte valor si el paràmetre no hi és o és buit
     * @return valor del paràmetre
     */
    String text(String cami, String perDefecte) {
        String sobreescrit = sobreescriptures.get(cami);
        if (sobreescrit != null) {
            return sobreescrit;
        }
        try {
            // L'expressió és relativa: s'avalua des de l'element arrel
            Node node = (Node) xpath.evaluate(cami, arrel, XPathConstants.NODE);
            if (node == null) {
                return perDefecte;
            }
            String valor = node.getTextContent().trim();
            return valor.isEmpty() ? perDefecte : valor;
        } catch (XPathExpressionException e) {
            throw new IllegalArgumentException("Camí de configuració no vàlid: " + cami, e);
        }
    }

    private String textObligatori(String cami) {
        String valor = text(cami, null);
        if (valor == null) {
            throw new ConfiguracioException("Falta el paràmetre obligatori " + cami);
        }
        return valor;
    }

    private int enter(String cami, int perDefecte) {
        String valor = text(cami, null);
        if (valor == null) {
            return perDefecte;
        }
        try {
            return Integer.parseInt(valor);
        } catch (NumberFormatException e) {
            throw new ConfiguracioException("El paràmetre " + cami + " ha de ser un número enter, i val '" + valor + "'", e);
        }
    }

    private boolean boolea(String cami, boolean perDefecte) {
        String valor = text(cami, null);
        if (valor == null) {
            return perDefecte;
        }
        return switch (valor.toLowerCase(Locale.ROOT)) {
            case "true", "sí", "si", "1" -> true;
            case "false", "no", "0" -> false;
            default -> throw new ConfiguracioException(
                    "El paràmetre " + cami + " ha de ser true o false, i val '" + valor + "'");
        };
    }

    private Duration durada(String cami, Duration perDefecte) {
        String valor = text(cami, null);
        if (valor == null) {
            return perDefecte;
        }
        long quantitat;
        try {
            quantitat = Long.parseLong(valor);
        } catch (NumberFormatException e) {
            throw new ConfiguracioException("El paràmetre " + cami + " ha de ser un número, i val '" + valor + "'", e);
        }
        String unitat = text(cami + "/@unitat", "s");
        return switch (unitat) {
            case "ms" -> Duration.ofMillis(quantitat);
            case "s" -> Duration.ofSeconds(quantitat);
            case "min" -> Duration.ofMinutes(quantitat);
            default -> throw new ConfiguracioException(
                    "Unitat de temps desconeguda a " + cami + ": '" + unitat + "' (es pot fer servir ms, s o min)");
        };
    }

    private List<String> llista(String cami) {
        String sobreescrit = sobreescriptures.get(cami);
        if (sobreescrit != null) {
            // Una llista sobreescrita s'escriu amb els valors separats per comes
            return Arrays.stream(sobreescrit.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
        }
        try {
            NodeList nodes = (NodeList) xpath.evaluate(cami, arrel, XPathConstants.NODESET);
            List<String> valors = new ArrayList<>();
            for (int i = 0; i < nodes.getLength(); i++) {
                valors.add(nodes.item(i).getTextContent().trim());
            }
            return valors;
        } catch (XPathExpressionException e) {
            throw new IllegalArgumentException("Camí de configuració no vàlid: " + cami, e);
        }
    }

    /**
     * Llegeix la configuració de mostra, primer tal com és i després amb dues sobreescriptures.
     *
     * @param args no es fan servir
     * @throws Exception si no es pot llegir
     */
    public static void main(String[] args) throws Exception {
        try (InputStream entrada = Recursos.dades("configuracio.xml")) {
            System.out.println("Configuració del fitxer:\n" + llegir(entrada));
        }
        Map<String, String> sobreescriptures = Map.of(
                "baseDades/url", "jdbc:sqlite:proves.db",
                "aplicacio/entorn", "proves");
        try (InputStream entrada = Recursos.dades("configuracio.xml")) {
            System.out.println("\nAmb sobreescriptures " + sobreescriptures + ":\n" + llegir(entrada, sobreescriptures));
        }
    }
}
