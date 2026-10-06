package com.project.exemples.validacio;

import com.project.exemples.Fluxos;
import com.project.exemples.comu.Recursos;
import com.project.exemples.dom.creacio.CreacioDom;
import com.project.exemples.model.Client;
import com.project.exemples.model.Comanda;
import com.project.exemples.model.LiniaComanda;
import com.project.exemples.seguretat.AtacsXml;
import com.project.exemples.validacio.ValidadorXsd.ErrorValidacio;
import com.project.exemples.validacio.ValidadorXsd.Gravetat;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Exemple 9: validació amb XSD")
class ValidadorXsdTest {

    private static ValidadorXsd validador;

    @TempDir
    Path temporal;

    @BeforeAll
    static void crearValidador() {
        validador = ValidadorXsd.perComandes();
    }

    private static List<ErrorValidacio> validarRecurs(String nom) throws Exception {
        try (InputStream entrada = Recursos.dades(nom)) {
            return validador.validar(entrada);
        }
    }

    private static Comanda comandaCorrecta() {
        return new Comanda("C2001", LocalDate.of(2026, 10, 5), new Client("Laia Ferrer", "laia@example.com"),
                List.of(new LiniaComanda("P001", 1, new BigDecimal("49.90"))), "Res a destacar");
    }

    @Test
    @DisplayName("La comanda vàlida no té cap error")
    void comandaValida() throws Exception {
        assertEquals(List.of(), validarRecurs("comanda-valida.xml"));
        try (InputStream entrada = Recursos.dades("comanda-valida.xml")) {
            assertTrue(validador.esValid(entrada));
        }
    }

    @Test
    @DisplayName("La comanda invàlida té errors a totes les línies problemàtiques")
    void comandaInvalida() throws Exception {
        List<ErrorValidacio> errors = validarRecurs("comanda-invalida.xml");
        Set<Integer> linies = errors.stream().map(ErrorValidacio::linia).collect(Collectors.toSet());
        // 2: id i data; 5: email; 8: quantitat 0; 9: preu amb 3 decimals; 11: falta <total>
        assertEquals(Set.of(2, 5, 8, 9, 11), linies);
        assertTrue(errors.stream().allMatch(error -> error.gravetat() == Gravetat.ERROR));
    }

    @Test
    @DisplayName("Els missatges indiquen quin valor és incorrecte")
    void missatges() throws Exception {
        List<ErrorValidacio> errors = validarRecurs("comanda-invalida.xml");
        assertTrue(errors.stream().anyMatch(error -> error.missatge().contains("C12")));
        assertTrue(errors.stream().anyMatch(error -> error.missatge().contains("12.005")));
        try (InputStream entrada = Recursos.dades("comanda-invalida.xml")) {
            assertFalse(validador.esValid(entrada));
        }
    }

    @Test
    @DisplayName("La comanda creada amb DOM a l'exemple 2 compleix l'esquema")
    void comandaCreadaAmbDom() throws Exception {
        assertEquals(List.of(), validador.validar(CreacioDom.crearComanda(comandaCorrecta())));
    }

    @Test
    @DisplayName("Un document DOM invàlid dona errors sense número de línia")
    void documentDomInvalid() throws Exception {
        Document document = CreacioDom.crearComanda(comandaCorrecta());
        Node total = document.getElementsByTagName("total").item(0);
        total.getParentNode().removeChild(total);
        List<ErrorValidacio> errors = validador.validar(document);
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().allMatch(error -> error.linia() == -1));
    }

    @Test
    @DisplayName("Un XML mal format acaba amb un error greu")
    void malFormat() throws Exception {
        List<ErrorValidacio> errors = validador.validar(Fluxos.de("<comanda><client></comanda>"));
        assertEquals(Gravetat.ERROR_GREU, errors.get(errors.size() - 1).gravetat());
    }

    @Test
    @DisplayName("El validador no llegeix fitxers locals a través d'entitats externes")
    void noLlegeixEntitatsExternes() throws Exception {
        Path secret = Files.writeString(temporal.resolve("secret.txt"), "SECRET-1234");
        String xml = AtacsXml.documentXxe(secret).replace("producte", "comanda");
        List<ErrorValidacio> errors = validador.validar(Fluxos.de(xml));
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().noneMatch(error -> error.missatge().contains("SECRET-1234")));
    }

    @Test
    @DisplayName("Un esquema mal escrit es detecta en crear el validador")
    void esquemaInvalid() throws Exception {
        Path esquema = Files.writeString(temporal.resolve("dolent.xsd"),
                "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"><xs:element name=\"a\" type=\"NoExisteix\"/></xs:schema>");
        assertThrows(IllegalArgumentException.class, () -> new ValidadorXsd(esquema.toUri().toURL()));
    }

    @Test
    @DisplayName("Cada error es pot mostrar en una línia llegible")
    void textDeLError() {
        ErrorValidacio error = new ErrorValidacio(Gravetat.ERROR, 3, 7, "Valor incorrecte");
        assertEquals("[ERROR] línia 3, columna 7: Valor incorrecte", error.toString());
    }
}
