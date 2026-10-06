package com.project.exemples.dom.modificacio;

import com.project.exemples.comu.Recursos;
import com.project.exemples.comu.Sortida;
import com.project.exemples.dom.creacio.CreacioDom;
import com.project.exemples.dom.lectura.LecturaDom;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Exemple 3. Modificació d'un document que ja existeix: canviar text i atributs,
 * reanomenar elements, reordenar-los, eliminar comentaris i desar el resultat net.
 */
public final class ModificacioDom {

    private static final BigDecimal CENT = new BigDecimal("100");

    private ModificacioDom() {
    }

    /**
     * Rebaixa el preu dels productes d'una categoria i deixa constància del descompte en un atribut.
     *
     * @param document    catàleg
     * @param categoria   categoria afectada
     * @param percentatge percentatge de descompte (entre 0 i 100)
     * @return nombre de productes modificats
     */
    public static int aplicarDescompte(Document document, String categoria, BigDecimal percentatge) {
        if (percentatge.signum() < 0 || percentatge.compareTo(CENT) > 0) {
            throw new IllegalArgumentException("El percentatge ha d'estar entre 0 i 100, i val " + percentatge);
        }
        BigDecimal factor = BigDecimal.ONE.subtract(percentatge.divide(CENT));
        int modificats = 0;
        NodeList productes = document.getElementsByTagName("producte");
        for (int i = 0; i < productes.getLength(); i++) {
            Element producte = (Element) productes.item(i);
            if (!producte.getAttribute("categoria").equals(categoria)) {
                continue;
            }
            Element preu = (Element) producte.getElementsByTagName("preu").item(0);
            BigDecimal nouPreu = new BigDecimal(preu.getTextContent().trim())
                    .multiply(factor)
                    .setScale(2, RoundingMode.HALF_UP);
            // setTextContent substitueix tot el contingut de l'element pel text nou
            preu.setTextContent(nouPreu.toPlainString());
            preu.setAttribute("descompte", percentatge.stripTrailingZeros().toPlainString() + "%");
            modificats++;
        }
        return modificats;
    }

    /**
     * Posa l'atribut {@code disponible="false"} als productes sense estoc i el treu dels que en tenen.
     *
     * @param document catàleg
     * @return nombre de productes marcats com a no disponibles
     */
    public static int marcarDisponibilitat(Document document) {
        int noDisponibles = 0;
        NodeList productes = document.getElementsByTagName("producte");
        for (int i = 0; i < productes.getLength(); i++) {
            Element producte = (Element) productes.item(i);
            int estoc = Integer.parseInt(producte.getElementsByTagName("estoc").item(0).getTextContent().trim());
            if (estoc == 0) {
                producte.setAttribute("disponible", "false");
                noDisponibles++;
            } else {
                // removeAttribute no falla si l'atribut no existeix
                producte.removeAttribute("disponible");
            }
        }
        return noDisponibles;
    }

    /**
     * Canvia el nom de tots els elements amb un nom determinat, conservant-ne el contingut.
     *
     * @param document  document que es modifica
     * @param nomActual nom que tenen ara els elements
     * @param nomNou    nom que han de tenir
     * @return nombre d'elements reanomenats
     */
    public static int reanomenarElements(Document document, String nomActual, String nomNou) {
        // La NodeList de getElementsByTagName és «viva»: s'actualitza quan canvia el document.
        // Com que un element reanomenat deixa de coincidir amb la cerca, primer se'n fa una còpia.
        NodeList nodes = document.getElementsByTagName(nomActual);
        List<Element> elements = new ArrayList<>();
        for (int i = 0; i < nodes.getLength(); i++) {
            elements.add((Element) nodes.item(i));
        }
        for (Element element : elements) {
            document.renameNode(element, null, nomNou);
        }
        return elements.size();
    }

    /**
     * Ordena els productes del catàleg del més barat al més car.
     *
     * @param document catàleg
     */
    public static void ordenarPerPreu(Document document) {
        Element cataleg = document.getDocumentElement();
        List<Element> productes = new ArrayList<>();
        for (Node fill = cataleg.getFirstChild(); fill != null; fill = fill.getNextSibling()) {
            if (fill.getNodeType() == Node.ELEMENT_NODE && fill.getNodeName().equals("producte")) {
                productes.add((Element) fill);
            }
        }
        productes.sort(Comparator.comparing(ModificacioDom::preuDe));
        // appendChild d'un node que ja és a l'arbre el mou: el treu del seu lloc i el posa al final
        for (Element producte : productes) {
            cataleg.appendChild(producte);
        }
    }

    private static BigDecimal preuDe(Element producte) {
        return new BigDecimal(producte.getElementsByTagName("preu").item(0).getTextContent().trim());
    }

    /**
     * Elimina tots els comentaris del node i dels seus descendents.
     *
     * @param node node des d'on es comença
     * @return nombre de comentaris eliminats
     */
    public static int eliminarComentaris(Node node) {
        int eliminats = 0;
        Node fill = node.getFirstChild();
        while (fill != null) {
            // Cal guardar el germà següent abans d'eliminar el node actual
            Node seguent = fill.getNextSibling();
            if (fill.getNodeType() == Node.COMMENT_NODE) {
                node.removeChild(fill);
                eliminats++;
            } else {
                eliminats += eliminarComentaris(fill);
            }
            fill = seguent;
        }
        return eliminats;
    }

    /**
     * Elimina els nodes de text que només tenen espais, tabuladors o salts de línia.
     * Si no es fa, en desar el document indentat apareixen línies en blanc.
     *
     * @param node node des d'on es comença
     */
    public static void netejarEspaisEnBlanc(Node node) {
        NodeList fills = node.getChildNodes();
        // getChildNodes també és una llista viva: es recorre de darrere cap endavant
        for (int i = fills.getLength() - 1; i >= 0; i--) {
            Node fill = fills.item(i);
            if (fill.getNodeType() == Node.TEXT_NODE && fill.getTextContent().isBlank()) {
                node.removeChild(fill);
            } else {
                netejarEspaisEnBlanc(fill);
            }
        }
    }

    /**
     * Desa el document modificat sense línies en blanc.
     *
     * @param document document que es vol desar
     * @param fitxer   fitxer de destinació
     * @throws TransformerException si falla la transformació
     * @throws IOException          si no es pot escriure el fitxer
     */
    public static void desar(Document document, Path fitxer) throws TransformerException, IOException {
        netejarEspaisEnBlanc(document);
        CreacioDom.desar(document, fitxer);
    }

    /**
     * Aplica totes les modificacions al catàleg de mostra i desa el resultat.
     *
     * @param args no es fan servir
     * @throws Exception si no es pot llegir o desar el catàleg
     */
    public static void main(String[] args) throws Exception {
        Document document;
        try (InputStream entrada = Recursos.dades("cataleg.xml")) {
            document = LecturaDom.carregar(entrada);
        }
        System.out.println("Productes rebaixats: " + aplicarDescompte(document, "monitors", new BigDecimal("10")));
        System.out.println("Productes sense estoc: " + marcarDisponibilitat(document));
        System.out.println("Elements <descripcio> reanomenats a <resum>: " + reanomenarElements(document, "descripcio", "resum"));
        ordenarPerPreu(document);
        System.out.println("Comentaris eliminats: " + eliminarComentaris(document));
        Path fitxer = Sortida.fitxer("cataleg-modificat.xml");
        desar(document, fitxer);
        System.out.println(CreacioDom.aText(document));
        System.out.println("Catàleg modificat desat a " + fitxer.toAbsolutePath());
    }
}
