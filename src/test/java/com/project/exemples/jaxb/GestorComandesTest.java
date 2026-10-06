package com.project.exemples.jaxb;

import com.project.exemples.Fluxos;
import com.project.exemples.comu.Recursos;
import jakarta.xml.bind.JAXBException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Exemple 7: vinculació amb JAXB")
class GestorComandesTest {

    private static GestorComandes gestor;
    private static Comandes comandes;

    @BeforeAll
    static void llegirComandes() throws Exception {
        gestor = new GestorComandes();
        try (InputStream entrada = Recursos.dades("comandes.xml")) {
            comandes = gestor.llegir(entrada);
        }
    }

    private static List<String> ids(Comandes comandes) {
        return comandes.getComandes().stream().map(Comanda::getId).toList();
    }

    @Test
    @DisplayName("Converteix cada <comanda> en un objecte")
    void llegeixLesComandes() {
        assertEquals(List.of("C1001", "C1002", "C1003"), ids(comandes));
    }

    @Test
    @DisplayName("Els atributs, els elements i les llistes es converteixen als tipus Java")
    void convertiixElsTipus() {
        Comanda comanda = comandes.getComandes().get(0);
        Linia linia = comanda.getLinies().get(0);
        assertAll(
                () -> assertEquals(LocalDate.of(2026, 9, 1), comanda.getData()),
                () -> assertEquals("Marta Puig", comanda.getClient().getNom()),
                () -> assertEquals("marta.puig@example.com", comanda.getClient().getEmail()),
                () -> assertEquals(2, comanda.getLinies().size()),
                () -> assertEquals("P001", linia.getProducte()),
                () -> assertEquals(1, linia.getQuantitat()),
                () -> assertEquals(new BigDecimal("49.90"), linia.getPreuUnitari()));
    }

    @Test
    @DisplayName("El total de cada comanda es calcula a partir de les línies")
    void totals() {
        assertEquals(List.of(new BigDecimal("85.90"), new BigDecimal("458.00"), new BigDecimal("74.50")),
                comandes.getComandes().stream().map(Comanda::getTotal).toList());
    }

    @Test
    @DisplayName("Calcula el total facturat i el total per client")
    void calculs() {
        assertEquals(new BigDecimal("618.40"), GestorComandes.totalFacturat(comandes));
        assertEquals(Map.of("Marta Puig", new BigDecimal("160.40"), "Pau Serra", new BigDecimal("458.00")),
                GestorComandes.totalPerClient(comandes));
    }

    @Test
    @DisplayName("Selecciona les comandes entre dues dates, ambdues incloses")
    void entreDates() {
        assertEquals(List.of("C1001", "C1002"),
                ids(GestorComandes.entreDates(comandes, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 5))));
        assertEquals(List.of("C1002", "C1003"),
                ids(GestorComandes.entreDates(comandes, LocalDate.of(2026, 9, 3), LocalDate.of(2026, 9, 10))));
        assertEquals(3, comandes.getComandes().size(), "L'original no s'ha de modificar");
    }

    @Test
    @DisplayName("L'XML generat té el contenidor <linies> i no inclou els valors calculats")
    void formatDeSortida() throws Exception {
        String text = gestor.aText(comandes);
        assertTrue(text.contains("<linies>"));
        assertTrue(text.contains("data=\"2026-09-01\""));
        assertFalse(text.contains("total"));
        assertFalse(text.contains("import"));
    }

    @Test
    @DisplayName("Escriure i tornar a llegir dona les mateixes dades")
    void anadaITornada() throws Exception {
        Comandes llegides = gestor.llegir(Fluxos.de(gestor.aText(comandes)));
        assertEquals(ids(comandes), ids(llegides));
        assertEquals(GestorComandes.totalFacturat(comandes), GestorComandes.totalFacturat(llegides));
        assertEquals(LocalDate.of(2026, 9, 10), llegides.getComandes().get(2).getData());
    }

    @Test
    @DisplayName("Converteix en XML objectes creats des del codi")
    void objectesNous() throws Exception {
        Comanda comanda = new Comanda("C9000", LocalDate.of(2026, 10, 1), new Client("Biel Roca", "biel@example.com"),
                List.of(new Linia("P002", 3, new BigDecimal("19.95"))));
        String text = gestor.aText(new Comandes(List.of(comanda)));
        assertTrue(text.contains("id=\"C9000\""));
        assertTrue(text.contains("<nom>Biel Roca</nom>"));
        assertTrue(text.contains("preuUnitari=\"19.95\""));
    }

    @Test
    @DisplayName("Una llista buida es converteix en un element buit")
    void comandesBuides() throws Exception {
        assertTrue(gestor.aText(new Comandes()).contains("<comandes/>"));
    }

    @Test
    @DisplayName("Un XML mal format llança JAXBException")
    void malFormat() {
        assertThrows(JAXBException.class, () -> gestor.llegir(Fluxos.de("<comandes><comanda></comandes>")));
    }
}
