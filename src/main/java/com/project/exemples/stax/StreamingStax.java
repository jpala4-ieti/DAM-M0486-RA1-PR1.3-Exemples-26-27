package com.project.exemples.stax;

import com.project.exemples.comu.Recursos;
import com.project.exemples.comu.Sortida;
import com.project.exemples.seguretat.ParsersSegurs;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.XMLEvent;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;

/**
 * Exemple 6. Lectura i escriptura en streaming amb StAX.
 *
 * <p>Com SAX, StAX no carrega el document en memòria. La diferència és que és el programa qui
 * demana el següent element (model <i>pull</i>), cosa que fa el codi més fàcil de seguir.
 * A més, StAX també permet escriure XML de manera seqüencial.</p>
 *
 * <ul>
 *     <li>API de cursor ({@link XMLStreamReader} / {@link XMLStreamWriter}): la més eficient.</li>
 *     <li>API d'esdeveniments ({@link XMLEventReader} / {@link XMLEventWriter}): cada pas és un objecte
 *     que es pot guardar o copiar, ideal per filtrar d'un fitxer a un altre.</li>
 * </ul>
 */
public final class StreamingStax {

    private static final String[] CATEGORIES = {"periferics", "monitors", "emmagatzematge"};

    /** Categoria que es fa servir quan un producte no té l'atribut categoria. */
    public static final String SENSE_CATEGORIA = "(sense categoria)";

    private StreamingStax() {
    }

    /**
     * Resum d'un catàleg.
     *
     * @param productes             nombre de productes
     * @param unitats               unitats totals en estoc
     * @param productesPerCategoria productes de cada categoria, en ordre alfabètic
     */
    public record Resum(int productes, long unitats, Map<String, Integer> productesPerCategoria) {
    }

    /**
     * Genera un catàleg amb tants productes com calgui, escrivint-los un a un.
     * La memòria que es fa servir no depèn de la mida del fitxer.
     *
     * @param fitxer          fitxer de destinació
     * @param nombreProductes nombre de productes que es generen
     * @throws IOException        si no es pot escriure el fitxer
     * @throws XMLStreamException si falla l'escriptura XML
     */
    public static void generarCataleg(Path fitxer, int nombreProductes) throws IOException, XMLStreamException {
        try (OutputStream sortida = Files.newOutputStream(fitxer)) {
            XMLStreamWriter escriptor = XMLOutputFactory.newInstance().createXMLStreamWriter(sortida, "UTF-8");
            try {
                escriptor.writeStartDocument("UTF-8", "1.0");
                escriptor.writeCharacters("\n");
                escriptor.writeStartElement("cataleg");
                // StAX escapa els caràcters especials: el & s'escriu com &amp;
                escriptor.writeAttribute("botiga", "Byte & Bit");
                for (int i = 1; i <= nombreProductes; i++) {
                    escriptor.writeCharacters("\n    ");
                    escriptor.writeStartElement("producte");
                    escriptor.writeAttribute("id", String.format("G%06d", i));
                    escriptor.writeAttribute("categoria", CATEGORIES[i % CATEGORIES.length]);
                    escriureElement(escriptor, "nom", "Producte generat " + i);
                    escriureElement(escriptor, "preu", preuGenerat(i).toPlainString());
                    escriureElement(escriptor, "estoc", String.valueOf(i % 7));
                    escriptor.writeEndElement();
                }
                escriptor.writeCharacters("\n");
                escriptor.writeEndElement();
                escriptor.writeEndDocument();
                escriptor.flush();
            } finally {
                // XMLStreamWriter no és AutoCloseable i close() no tanca el flux de sota
                escriptor.close();
            }
        }
    }

    /**
     * Preu dels productes generats: va de 0,99 a 99,99.
     *
     * @param i número del producte
     * @return preu del producte
     */
    static BigDecimal preuGenerat(int i) {
        return BigDecimal.valueOf(i % 100).add(new BigDecimal("0.99"));
    }

    private static void escriureElement(XMLStreamWriter escriptor, String nom, String text) throws XMLStreamException {
        escriptor.writeStartElement(nom);
        escriptor.writeCharacters(text);
        escriptor.writeEndElement();
    }

    /**
     * Recorre el catàleg amb l'API de cursor i en calcula un resum.
     *
     * @param entrada flux amb el catàleg (no es tanca)
     * @return resum del catàleg
     * @throws XMLStreamException si l'XML no està ben format
     */
    public static Resum resumir(InputStream entrada) throws XMLStreamException {
        XMLStreamReader lector = ParsersSegurs.fabricaStaxSegura().createXMLStreamReader(entrada);
        try {
            int productes = 0;
            long unitats = 0;
            Map<String, Integer> perCategoria = new TreeMap<>();
            while (lector.hasNext()) {
                // És el programa qui avança: next() retorna el tipus del següent esdeveniment
                if (lector.next() == XMLStreamConstants.START_ELEMENT) {
                    switch (lector.getLocalName()) {
                        case "producte" -> {
                            productes++;
                            // getAttributeValue retorna null si l'atribut no hi és
                            String categoria = lector.getAttributeValue(null, "categoria");
                            perCategoria.merge(categoria == null ? SENSE_CATEGORIA : categoria, 1, Integer::sum);
                        }
                        // getElementText llegeix tot el text fins al tancament de l'element
                        case "estoc" -> unitats += Long.parseLong(lector.getElementText().trim());
                        default -> {
                            // La resta d'elements no interessen
                        }
                    }
                }
            }
            return new Resum(productes, unitats, perCategoria);
        } finally {
            lector.close();
        }
    }

    /**
     * Copia a la sortida només els productes d'una categoria, sense carregar mai el catàleg sencer.
     * És el patró típic dels processos ETL (extreure, transformar i carregar dades).
     * Es conserven l'element arrel (amb els seus atributs) i els productes seleccionats;
     * els comentaris i els espais de fora dels productes es descarten. El lector StAX
     * lliura les seccions CDATA com a text normal: el contingut es conserva, però s'escriu escapat.
     *
     * @param entrada   flux amb el catàleg (no es tanca)
     * @param sortida   flux on s'escriu el resultat (no es tanca)
     * @param categoria categoria que es vol conservar
     * @return nombre de productes copiats
     * @throws XMLStreamException si falla la lectura o l'escriptura
     */
    public static int filtrarPerCategoria(InputStream entrada, OutputStream sortida, String categoria) throws XMLStreamException {
        XMLEventReader lector = ParsersSegurs.fabricaStaxSegura().createXMLEventReader(entrada);
        XMLEventWriter escriptor = XMLOutputFactory.newInstance().createXMLEventWriter(sortida, "UTF-8");
        XMLEventFactory esdeveniments = XMLEventFactory.newInstance();
        int copiats = 0;
        boolean copiant = false;
        try {
            while (lector.hasNext()) {
                XMLEvent esdeveniment = lector.nextEvent();
                if (esInici(esdeveniment, "producte")) {
                    Attribute atribut = esdeveniment.asStartElement().getAttributeByName(new QName("categoria"));
                    copiant = atribut != null && categoria.equals(atribut.getValue());
                    if (copiant) {
                        copiats++;
                        escriptor.add(esdeveniments.createCharacters("\n    "));
                    }
                }
                if (copiant) {
                    // Dins d'un producte seleccionat es copia tot: elements, text, comentaris...
                    escriptor.add(esdeveniment);
                    if (esFinal(esdeveniment, "producte")) {
                        copiant = false;
                    }
                } else if (esdeveniment.isStartDocument() || esInici(esdeveniment, "cataleg") || esdeveniment.isEndDocument()) {
                    escriptor.add(esdeveniment);
                    if (esdeveniment.isStartDocument()) {
                        escriptor.add(esdeveniments.createCharacters("\n"));
                    }
                } else if (esFinal(esdeveniment, "cataleg")) {
                    escriptor.add(esdeveniments.createCharacters("\n"));
                    escriptor.add(esdeveniment);
                }
            }
            escriptor.flush();
        } finally {
            lector.close();
            escriptor.close();
        }
        return copiats;
    }

    private static boolean esInici(XMLEvent esdeveniment, String nom) {
        return esdeveniment.isStartElement() && esdeveniment.asStartElement().getName().getLocalPart().equals(nom);
    }

    private static boolean esFinal(XMLEvent esdeveniment, String nom) {
        return esdeveniment.isEndElement() && esdeveniment.asEndElement().getName().getLocalPart().equals(nom);
    }

    /**
     * Genera un catàleg gran, el resumeix i en filtra una categoria.
     *
     * @param args no es fan servir
     * @throws Exception si falla algun pas
     */
    public static void main(String[] args) throws Exception {
        Path gran = Sortida.fitxer("cataleg-gran.xml");
        generarCataleg(gran, 100_000);
        System.out.println("Catàleg generat: " + gran.toAbsolutePath() + " (" + Files.size(gran) / 1024 + " KB)");
        try (InputStream entrada = Files.newInputStream(gran)) {
            System.out.println("Resum del catàleg gran: " + resumir(entrada));
        }
        try (InputStream entrada = Recursos.dades("cataleg.xml")) {
            System.out.println("Resum del catàleg de mostra: " + resumir(entrada));
        }
        Path monitors = Sortida.fitxer("cataleg-monitors.xml");
        try (InputStream entrada = Recursos.dades("cataleg.xml"); OutputStream sortida = Files.newOutputStream(monitors)) {
            int copiats = filtrarPerCategoria(entrada, sortida, "monitors");
            System.out.println("Productes de la categoria monitors copiats a " + monitors.getFileName() + ": " + copiats);
        }
        System.out.println(Files.readString(monitors));
    }
}
