package com.project.exemples.jaxb;

import com.project.exemples.comu.Recursos;
import com.project.exemples.comu.Sortida;
import com.project.exemples.seguretat.ParsersSegurs;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Map;
import java.util.TreeMap;

/**
 * Exemple 7. Vinculació (binding) amb JAXB: l'XML es converteix directament en objectes Java
 * (unmarshal) i els objectes en XML (marshal), sense tractar nodes un a un.
 */
public final class GestorComandes {

    /** El context és costós de crear però es pot reutilitzar i compartir entre fils. */
    private final JAXBContext context;

    /**
     * Crea el context JAXB per a les classes de les comandes.
     */
    public GestorComandes() {
        try {
            context = JAXBContext.newInstance(Comandes.class);
        } catch (JAXBException e) {
            throw new IllegalStateException("Les anotacions de les classes JAXB no són correctes", e);
        }
    }

    /**
     * Converteix un document {@code <comandes>} en objectes.
     *
     * @param entrada flux amb l'XML (no es tanca)
     * @return comandes llegides
     * @throws JAXBException si l'XML no correspon a les classes o no està ben format
     */
    public Comandes llegir(InputStream entrada) throws JAXBException {
        Unmarshaller unmarshaller = context.createUnmarshaller();
        try {
            // Es llegeix a través d'un lector StAX segur, sense DTD ni entitats externes
            XMLStreamReader lector = ParsersSegurs.fabricaStaxSegura().createXMLStreamReader(entrada);
            try {
                return (Comandes) unmarshaller.unmarshal(lector);
            } finally {
                lector.close();
            }
        } catch (XMLStreamException e) {
            throw new JAXBException("No es pot llegir l'XML: " + e.getMessage(), e);
        }
    }

    /**
     * Converteix les comandes en XML indentat.
     *
     * @param comandes comandes que es volen escriure
     * @param sortida  flux de destinació (no es tanca)
     * @throws JAXBException si no es poden convertir
     */
    public void escriure(Comandes comandes, OutputStream sortida) throws JAXBException {
        marshaller().marshal(comandes, sortida);
    }

    /**
     * Converteix les comandes en text XML.
     *
     * @param comandes comandes que es volen convertir
     * @return text XML
     * @throws JAXBException si no es poden convertir
     */
    public String aText(Comandes comandes) throws JAXBException {
        StringWriter text = new StringWriter();
        marshaller().marshal(comandes, text);
        return text.toString();
    }

    private Marshaller marshaller() throws JAXBException {
        Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
        return marshaller;
    }

    /**
     * Un cop tenim objectes, els càlculs són codi Java normal.
     *
     * @param comandes comandes
     * @return suma dels totals de totes les comandes
     */
    public static BigDecimal totalFacturat(Comandes comandes) {
        return comandes.getComandes().stream().map(Comanda::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * @param comandes comandes
     * @return import total de cada client, ordenat pel nom del client
     */
    public static Map<String, BigDecimal> totalPerClient(Comandes comandes) {
        Map<String, BigDecimal> totals = new TreeMap<>();
        for (Comanda comanda : comandes.getComandes()) {
            totals.merge(comanda.getClient().getNom(), comanda.getTotal(), BigDecimal::add);
        }
        return totals;
    }

    /**
     * Crea un document nou amb les comandes compreses entre dues dates (ambdues incloses).
     * El document original no es modifica.
     *
     * @param comandes comandes d'origen
     * @param desDe    primera data
     * @param finsA    última data
     * @return comandes seleccionades
     */
    public static Comandes entreDates(Comandes comandes, LocalDate desDe, LocalDate finsA) {
        return new Comandes(comandes.getComandes().stream()
                .filter(c -> !c.getData().isBefore(desDe) && !c.getData().isAfter(finsA))
                .toList());
    }

    /**
     * Llegeix les comandes de mostra, en calcula els totals i desa les de la primera setmana.
     *
     * @param args no es fan servir
     * @throws Exception si falla la lectura o l'escriptura
     */
    public static void main(String[] args) throws Exception {
        GestorComandes gestor = new GestorComandes();
        Comandes comandes;
        try (InputStream entrada = Recursos.dades("comandes.xml")) {
            comandes = gestor.llegir(entrada);
        }
        for (Comanda comanda : comandes.getComandes()) {
            System.out.println("- " + comanda.getId() + " (" + comanda.getData() + ") de " + comanda.getClient().getNom()
                    + ": " + comanda.getLinies().size() + " línies, " + comanda.getTotal() + " EUR");
        }
        System.out.println("Total facturat: " + totalFacturat(comandes) + " EUR");
        System.out.println("Total per client: " + totalPerClient(comandes));

        Comandes primeraSetmana = entreDates(comandes, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 7));
        Path fitxer = Sortida.fitxer("comandes-primera-setmana.xml");
        try (OutputStream sortida = Files.newOutputStream(fitxer)) {
            gestor.escriure(primeraSetmana, sortida);
        }
        System.out.println("\nComandes de la primera setmana desades a " + fitxer.toAbsolutePath() + ":");
        System.out.println(gestor.aText(primeraSetmana));
    }
}
