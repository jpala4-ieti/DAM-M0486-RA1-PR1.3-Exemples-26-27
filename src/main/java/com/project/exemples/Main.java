package com.project.exemples;

import com.project.exemples.configuracio.LectorConfiguracio;
import com.project.exemples.dom.creacio.CreacioDom;
import com.project.exemples.dom.lectura.LecturaDom;
import com.project.exemples.dom.modificacio.ModificacioDom;
import com.project.exemples.excepcions.LectorAmbDiagnostic;
import com.project.exemples.jaxb.GestorComandes;
import com.project.exemples.json.ConversioJson;
import com.project.exemples.namespaces.EspaisDeNoms;
import com.project.exemples.sax.EstadistiquesSax;
import com.project.exemples.seguretat.AtacsXml;
import com.project.exemples.stax.StreamingStax;
import com.project.exemples.validacio.ValidadorXsd;
import com.project.exemples.xpath.ConsultesXPath;
import com.project.exemples.xslt.TransformacionsXslt;

import java.util.List;
import java.util.Optional;
import java.util.Scanner;

/**
 * Punt d'entrada del jar. Mostra un menú amb tots els exemples.
 *
 * <pre>
 * java -jar dam-m0486-ra1-pr13-exemples.jar        menú interactiu
 * java -jar dam-m0486-ra1-pr13-exemples.jar 4      executa l'exemple 4
 * java -jar dam-m0486-ra1-pr13-exemples.jar tots   executa tots els exemples
 * </pre>
 */
public final class Main {

    /** Acció d'un exemple: com un Runnable, però pot llançar excepcions comprovades. */
    @FunctionalInterface
    interface AccioExemple {
        void executar() throws Exception;
    }

    /**
     * Un exemple del menú.
     *
     * @param numero número de l'exemple
     * @param titol  descripció curta
     * @param accio  codi que l'executa
     */
    record Exemple(int numero, String titol, AccioExemple accio) {
    }

    private static final String[] SENSE_ARGUMENTS = new String[0];

    /** Llista de tots els exemples, en l'ordre del README. */
    static final List<Exemple> EXEMPLES = List.of(
            new Exemple(1, "DOM: llegir el catàleg de productes", () -> LecturaDom.main(SENSE_ARGUMENTS)),
            new Exemple(2, "DOM: crear una comanda des de zero", () -> CreacioDom.main(SENSE_ARGUMENTS)),
            new Exemple(3, "DOM: modificar el catàleg", () -> ModificacioDom.main(SENSE_ARGUMENTS)),
            new Exemple(4, "XPath: consultes sobre el catàleg", () -> ConsultesXPath.main(SENSE_ARGUMENTS)),
            new Exemple(5, "SAX: estadístiques en una sola passada", () -> EstadistiquesSax.main(SENSE_ARGUMENTS)),
            new Exemple(6, "StAX: fitxers grans en streaming", () -> StreamingStax.main(SENSE_ARGUMENTS)),
            new Exemple(7, "JAXB: comandes com a objectes Java", () -> GestorComandes.main(SENSE_ARGUMENTS)),
            new Exemple(8, "Excepcions: errors amb línia i columna", () -> LectorAmbDiagnostic.main(SENSE_ARGUMENTS)),
            new Exemple(9, "Validació amb un esquema XSD", () -> ValidadorXsd.main(SENSE_ARGUMENTS)),
            new Exemple(10, "XSLT: catàleg a HTML i a CSV", () -> TransformacionsXslt.main(SENSE_ARGUMENTS)),
            new Exemple(11, "JSON: conversió entre XML i JSON", () -> ConversioJson.main(SENSE_ARGUMENTS)),
            new Exemple(12, "Seguretat: atacs XXE i billion laughs", () -> AtacsXml.main(SENSE_ARGUMENTS)),
            new Exemple(13, "Espais de noms: factura electrònica", () -> EspaisDeNoms.main(SENSE_ARGUMENTS)),
            new Exemple(14, "Fitxer de configuració", () -> LectorConfiguracio.main(SENSE_ARGUMENTS)));

    private Main() {
    }

    /**
     * @param args un número d'exemple, {@code tots}, o res per veure el menú
     */
    public static void main(String[] args) {
        if (args.length > 0) {
            System.exit(executarArguments(args));
        }
        menu();
    }

    /**
     * Executa els exemples indicats als arguments.
     *
     * @param args arguments de la línia d'ordres
     * @return codi de sortida: 0 si tot ha anat bé
     */
    static int executarArguments(String[] args) {
        if (args[0].equalsIgnoreCase("tots")) {
            boolean totsCorrectes = true;
            for (Exemple exemple : EXEMPLES) {
                totsCorrectes &= executar(exemple.numero());
            }
            return totsCorrectes ? 0 : 1;
        }
        try {
            return executar(Integer.parseInt(args[0])) ? 0 : 1;
        } catch (NumberFormatException e) {
            System.out.println("Argument no vàlid: " + args[0]);
            mostrarLlista();
            return 1;
        }
    }

    /**
     * Executa un exemple pel seu número.
     *
     * @param numero número de l'exemple
     * @return {@code true} si existeix i s'ha executat sense errors
     */
    static boolean executar(int numero) {
        Optional<Exemple> exemple = EXEMPLES.stream().filter(e -> e.numero() == numero).findFirst();
        if (exemple.isEmpty()) {
            System.out.println("No hi ha cap exemple amb el número " + numero);
            return false;
        }
        System.out.println("\n=== " + numero + ". " + exemple.get().titol() + " ===\n");
        try {
            exemple.get().accio().executar();
            return true;
        } catch (Exception e) {
            System.out.println("L'exemple ha fallat: " + e);
            return false;
        }
    }

    private static void mostrarLlista() {
        System.out.println("\nExemples de tractament de fitxers XML amb Java (DAM, M0486, PR1.3)");
        for (Exemple exemple : EXEMPLES) {
            System.out.printf("%3d. %s%n", exemple.numero(), exemple.titol());
        }
        System.out.println("  0. Sortir");
    }

    private static void menu() {
        Scanner entrada = new Scanner(System.in);
        while (true) {
            mostrarLlista();
            System.out.print("Tria un exemple: ");
            if (!entrada.hasNextLine()) {
                return;
            }
            String text = entrada.nextLine().trim();
            if (text.equals("0")) {
                return;
            }
            try {
                executar(Integer.parseInt(text));
            } catch (NumberFormatException e) {
                System.out.println("Escriu un número de la llista.");
            }
        }
    }
}
