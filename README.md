# Exemples de fitxers XML amb Java #

**DAM · M0486 Accés a dades · RA1 · PR1.3 · Curs 2026-27**

Aquest repositori conté 14 exemples complets de tractament de fitxers XML amb Java: les tècniques que s'expliquen a la teoria (DOM, XPath, SAX, StAX, JAXB i gestió d'errors) i els casos d'ús més habituals a la feina (validació amb XSD, XSLT, JSON, seguretat, espais de noms i fitxers de configuració).

Tots els exemples treballen amb les dades d'una botiga en línia imaginària, **Byte & Bit**: un catàleg de productes, comandes de clients i una factura. **No resolen els exercicis de la pràctica PR1.3**: mostren les tècniques sobre unes altres dades i amb unes altres operacions, i és feina teva adaptar-les.

Cada exemple té:

* una classe amb mètodes reutilitzables i un `main` que l'executa amb les dades de mostra;
* una classe de tests que en comprova el funcionament (més de 190 tests en total);
* una secció en aquest README.

## Índex ##

1. [Requisits](#requisits)
2. [Començar ràpidament](#començar-ràpidament)
3. [Estructura del projecte](#estructura-del-projecte)
4. [Les dades de mostra](#les-dades-de-mostra)
5. [Llista d'exemples](#llista-dexemples)
6. [Explicació de cada exemple](#explicació-de-cada-exemple)
7. [Quina tècnica he de fer servir?](#quina-tècnica-he-de-fer-servir)
8. [Bones pràctiques que es repeteixen](#bones-pràctiques-que-es-repeteixen)
9. [Tests i cobertura](#tests-i-cobertura)
10. [Problemes freqüents](#problemes-freqüents)

## Requisits ##

* JDK 21 o posterior
* Maven 3.6.3 o posterior
* Connexió a internet la primera vegada (Maven descarrega les llibreries)

## Començar ràpidament ##

```bash
# Compilar i executar tots els tests
mvn test

# Generar el jar executable (target/dam-m0486-ra1-pr13-exemples.jar)
mvn package

# Generar el jar, l'informe de cobertura i comprovar la cobertura mínima
mvn verify
```

Executar els exemples amb el jar:

```bash
java -jar target/dam-m0486-ra1-pr13-exemples.jar        # menú interactiu
java -jar target/dam-m0486-ra1-pr13-exemples.jar 4      # executa directament l'exemple 4
java -jar target/dam-m0486-ra1-pr13-exemples.jar tots   # executa tots els exemples
```

També es poden fer servir els scripts, que primer generen el jar:

```bash
./run.sh 4          # Linux i macOS
.\run.ps1 4         # Windows (PowerShell)
```

O executar-los sense generar el jar:

```bash
mvn compile exec:java                                  # menú
mvn compile exec:java -Dexec.args="4"                  # exemple 4
mvn compile exec:java -Dexec.mainClass=com.project.exemples.xpath.ConsultesXPath
```

Els fitxers que generen els exemples es desen a la carpeta `sortida/` (no es puja al repositori).

## Estructura del projecte ##

```
├── pom.xml                         configuració de Maven, comentada pas a pas
├── run.sh / run.ps1                scripts per generar el jar i executar-lo
├── .github/workflows/tests.yml     integració contínua: tests a cada push
├── src/main/java/com/project/exemples
│   ├── Main.java                   menú amb tots els exemples
│   ├── comu/                       utilitats compartides (recursos, sortida, lectura segura)
│   ├── model/                      records del domini: Producte, Comanda, Client, LiniaComanda
│   ├── dom/lectura/                1. DOM: lectura
│   ├── dom/creacio/                2. DOM: creació
│   ├── dom/modificacio/            3. DOM: modificació
│   ├── xpath/                      4. XPath
│   ├── sax/                        5. SAX
│   ├── stax/                       6. StAX
│   ├── jaxb/                       7. JAXB
│   ├── excepcions/                 8. Gestió d'errors
│   ├── validacio/                  9. Validació XSD
│   ├── xslt/                       10. XSLT
│   ├── json/                       11. XML i JSON
│   ├── seguretat/                  12. Seguretat
│   ├── namespaces/                 13. Espais de noms
│   └── configuracio/               14. Fitxers de configuració
├── src/main/resources
│   ├── dades/                      fitxers de mostra (XML i JSON)
│   ├── esquemes/comanda.xsd        esquema de les comandes
│   └── xslt/                       fulles d'estil (HTML i CSV)
└── src/test/java/...               un fitxer de tests per a cada exemple
```

Els fitxers de mostra són a `src/main/resources`, de manera que van dins del jar: els exemples funcionen des de l'IDE, amb Maven o amb `java -jar`, sigui quin sigui el directori des d'on s'executen. S'hi accedeix amb la classe `comu.Recursos`.

## Les dades de mostra ##

| Fitxer | Contingut | Exemples |
|---|---|---|
| `dades/cataleg.xml` | 6 productes amb atributs, preus, estoc, etiquetes, un CDATA i comentaris | 1, 3, 4, 5, 6, 10, 11 |
| `dades/comandes.xml` | 3 comandes amb client i línies | 7, 11 |
| `dades/comanda-valida.xml` i `comanda-invalida.xml` | una comanda correcta i una amb 6 errors | 9 |
| `dades/factura.xml` | factura amb tres espais de noms | 13 |
| `dades/configuracio.xml` | configuració d'una aplicació | 14 |
| `dades/productes-api.json` | resposta d'una API en JSON | 11 |
| `esquemes/comanda.xsd` | esquema XSD d'una comanda | 9 |
| `xslt/cataleg-a-html.xsl` i `cataleg-a-csv.xsl` | fulles d'estil | 10 |

## Llista d'exemples ##

| # | Paquet | Tècnica | Teoria |
|---|---|---|---|
| 1 | `dom.lectura` | Llegir un XML amb DOM i convertir-lo en objectes | diapositives 5-8 |
| 2 | `dom.creacio` | Crear un document des de zero i desar-lo amb Transformer | 12, 14 i 15 |
| 3 | `dom.modificacio` | Modificar text i atributs, reanomenar, reordenar i desar net | 13 i 16 |
| 4 | `xpath` | Consultes XPath: tipus de resultat, funcions, eixos i variables | 9-11 |
| 5 | `sax` | Lectura en una sola passada amb SAX | 18 i 19 |
| 6 | `stax` | Lectura i escriptura en streaming amb StAX | 18 |
| 7 | `jaxb` | Vinculació XML-objectes amb JAXB | 20 i 21 |
| 8 | `excepcions` | Gestió d'errors amb línia i columna | 17 |
| 9 | `validacio` | Validació contra un esquema XSD | ampliació |
| 10 | `xslt` | Transformació a HTML i a CSV amb XSLT | ampliació |
| 11 | `json` | Conversió entre XML i JSON | ampliació |
| 12 | `seguretat` | Atacs XXE i billion laughs, i com evitar-los | ampliació |
| 13 | `namespaces` | Documents amb espais de noms | ampliació |
| 14 | `configuracio` | Fitxers de configuració en XML | ampliació |

## Explicació de cada exemple ##

### 1. DOM: lectura (`LecturaDom`) ###

**Què mostra.** Com es carrega un XML en memòria amb `DocumentBuilder` i com es recorre l'arbre per convertir cada `<producte>` en un objecte `Producte`. També compta quants nodes hi ha de cada tipus, per veure que un document DOM no només té elements: els salts de línia són nodes de text i els comentaris són nodes de comentari.

**Fragment clau.**

```java
DocumentBuilder constructor = DocumentBuilderFactory.newInstance().newDocumentBuilder();
Document document = constructor.parse(entrada);
document.getDocumentElement().normalize();

NodeList nodes = document.getElementsByTagName("producte");
for (int i = 0; i < nodes.getLength(); i++) {
    Element producte = (Element) nodes.item(i);
    String id = producte.getAttribute("id");
    ...
}
```

**Detalls importants.**
* `getElementsByTagName` busca a **tots** els descendents. El mètode `primerFill` mostra com quedar-se només amb els fills directes.
* `getAttribute` retorna una cadena buida si l'atribut no existeix; per distingir-ho cal `hasAttribute`.
* Els preus es llegeixen amb `BigDecimal`, mai amb `double`.

### 2. DOM: creació (`CreacioDom`) ###

**Què mostra.** Com es construeix una comanda des de zero amb `createElement`, `setAttribute`, `setTextContent`, `createComment` i `createCDATASection`, i com es desa amb un `Transformer` indentat i en UTF-8.

**Fragment clau.**

```java
Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
Element arrel = document.createElement("comanda");
arrel.setAttribute("id", comanda.id());
document.appendChild(arrel);
...
Transformer transformador = TransformerFactory.newInstance().newTransformer();
transformador.setOutputProperty(OutputKeys.INDENT, "yes");
transformador.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
transformador.transform(new DOMSource(document), new StreamResult(sortida));
```

**Detalls importants.**
* Amb `setTextContent`, DOM escapa automàticament `&` i `<` en desar.
* Les dades d'un CDATA no s'escapen: és útil per a textos amb molts caràcters especials.
* El document generat compleix l'esquema de l'exemple 9 (ho comprova un test).

### 3. DOM: modificació (`ModificacioDom`) ###

**Què mostra.** Com es modifica un document que ja existeix: canviar preus (`setTextContent`), afegir i treure atributs (`setAttribute`, `removeAttribute`), canviar el nom d'elements (`renameNode`), reordenar-los (`appendChild` d'un node que ja és a l'arbre el mou) i eliminar comentaris. Al final es desa sense línies en blanc.

**Detalls importants.**
* Les `NodeList` de `getElementsByTagName` i `getChildNodes` són **vives**: canvien quan es modifica el document. Per això, abans de reanomenar o eliminar, se'n fa una còpia o es recorren de darrere cap endavant.
* Abans de desar s'eliminen els nodes de text que només són espais (`netejarEspaisEnBlanc`). Si no es fa, el `Transformer` afegeix la seva indentació a la que ja hi havia i el fitxer s'omple de línies en blanc.

### 4. XPath (`ConsultesXPath`) ###

**Què mostra.** Els cinc tipus de resultat (`NODESET`, `NODE`, `STRING`, `NUMBER` i `BOOLEAN`), les funcions `count`, `sum`, `contains`, `translate`, `position` i `last`, l'eix `preceding-sibling`, les expressions relatives compilades amb `XPathExpression` i les **variables**.

**Fragment clau: consulta amb variable.**

```java
XPath xpath = XPathFactory.newInstance().newXPath();
xpath.setXPathVariableResolver(nom -> variables.get(nom.getLocalPart()));
String nom = xpath.evaluate("/cataleg/producte[@id = $id]/nom", document);
```

**Per què variables?** Si l'expressió es construeix concatenant text que arriba de fora, un usuari pot canviar-ne el significat (injecció XPath). L'exemple ho demostra: amb l'identificador `x' or '1'='1`, la versió concatenada retorna un producte qualsevol i la versió amb variable no retorna res. A més, un simple apòstrof (`D'Artagnan`) trenca la versió concatenada.

### 5. SAX (`EstadistiquesSax`) ###

**Què mostra.** Com es calculen estadístiques del catàleg (productes per categoria, unitats, producte més car, preu mitjà) en una sola passada, sense crear cap arbre. També mostra com aturar la lectura quan ja s'ha trobat el que es buscava, llançant una excepció des del gestor.

**Detalls importants.**
* El text d'un element pot arribar en **diversos trossos** (diverses crides a `characters`): s'ha d'acumular en un `StringBuilder`. Un test ho comprova amb un nom que conté `&amp;`.
* SAX no recorda res: el gestor ha de guardar en quin producte es troba.

### 6. StAX (`StreamingStax`) ###

**Què mostra.**
* **Escriure** un catàleg de 100.000 productes amb `XMLStreamWriter`, element a element.
* **Llegir-lo** amb `XMLStreamReader` (API de cursor) per fer-ne un resum.
* **Filtrar** els productes d'una categoria d'un fitxer a un altre amb `XMLEventReader` i `XMLEventWriter` (API d'esdeveniments), el patró típic dels processos ETL.

Cap d'aquestes operacions carrega el fitxer sencer en memòria. A diferència de SAX, aquí és el programa qui demana el següent element (model *pull*).

### 7. JAXB (`GestorComandes`) ###

**Què mostra.** Com es converteix un XML directament en objectes Java (unmarshal) i a l'inrevés (marshal) amb les anotacions `@XmlRootElement`, `@XmlAttribute`, `@XmlElement`, `@XmlElementWrapper` i `@XmlJavaTypeAdapter`, que fa falta per a `LocalDate`.

**Fragment clau.**

```java
JAXBContext context = JAXBContext.newInstance(Comandes.class);
Comandes comandes = (Comandes) context.createUnmarshaller().unmarshal(lector);

Marshaller marshaller = context.createMarshaller();
marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
marshaller.marshal(comandes, sortida);
```

**Detalls importants.**
* JAXB necessita classes JavaBean (constructor buit, getters i setters). Per això té les seves pròpies classes al paquet `jaxb` i no fa servir els records del paquet `model`.
* Des de Java 11, JAXB no forma part del JDK: les dependències són al `pom.xml`.
* Els valors calculats (`getTotal`) no surten a l'XML perquè l'accés és per camps (`XmlAccessType.FIELD`).

### 8. Gestió d'errors (`LectorAmbDiagnostic`) ###

**Què mostra.** Com es tracta cada tipus d'error pel seu nom i es converteix en una excepció pròpia (`ErrorXmlException`) amb un missatge útil:

| Excepció original | Causa | Missatge |
|---|---|---|
| `NoSuchFileException` | el fitxer no existeix | `dades.xml: el fitxer no existeix` |
| `SAXParseException` | XML mal format | `dades.xml: línia 4: XML mal format: ...` |
| `IOException` | no es pot llegir | `dades.xml: no es pot llegir el fitxer: ...` |
| `XPathExpressionException` | expressió XPath incorrecta | `expressió XPath no vàlida: ...` |
| `ParserConfigurationException` | error de programació | es converteix en `IllegalStateException` |

També mostra com processar un **lot de fitxers** sense que un fitxer erroni aturi la resta, i com evitar que el parser escrigui `[Fatal Error]` per consola (amb un `ErrorHandler`).

### 9. Validació amb XSD (`ValidadorXsd`) ###

**Què mostra.** La diferència entre un XML **ben format** (etiquetes correctes) i un XML **vàlid** (compleix un esquema). Valida les comandes contra `comanda.xsd`, que exigeix l'ordre dels elements, dates correctes, quantitats positives, imports amb dos decimals i formats d'identificador i de correu.

**Fragment clau.**

```java
Schema esquema = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(fontXsd);
Validator validador = esquema.newValidator();
validador.setErrorHandler(gestorQueRecullErrors);   // per obtenir TOTS els errors, no només el primer
validador.validate(new StreamSource(xml));
```

Amb un `ErrorHandler` que no llança excepcions als errors, el validador continua i es poden mostrar tots els problemes d'un cop, amb línia i columna.

### 10. XSLT (`TransformacionsXslt`) ###

**Què mostra.** Com es transforma el mateix catàleg en una pàgina HTML i en un fitxer CSV sense escriure cap bucle en Java. Les fulles d'estil fan servir paràmetres, plantilles, `xsl:sort`, `xsl:choose`, `xsl:for-each`, `format-number` amb format català i una plantilla recursiva per escapar les cometes del CSV.

**Detalls importants.**
* `Templates` és una fulla d'estil compilada: es compila un cop i es reutilitza. Cada transformació necessita el seu `Transformer`.
* Canviar el format de sortida només vol dir canviar la fulla d'estil.
* El serialitzador HTML del JDK escriu els caràcters accentuats com a entitats (`&agrave;`): és HTML correcte.

### 11. XML i JSON (`ConversioJson`) ###

**Què mostra.** Les dues maneres de passar d'XML a JSON (el format de la majoria d'API web), fetes amb Jackson:

1. **Conversió genèrica**, node a node. Funciona amb qualsevol XML, però el resultat depèn de les dades: un element que es repeteix es converteix en una llista, però si només n'hi ha un, no. Tampoc distingeix números de textos. Els tests documenten aquests problemes.
2. **Conversió a través d'objectes**: XML → objectes (JAXB) → JSON (Jackson). El format és sempre el mateix, les dates surten en format ISO i els números com a números. És la que es fa servir a les aplicacions reals.

També converteix un JSON d'una API en XML, i mostra què es perd pel camí (els valors `null`, les llistes buides i els zeros finals dels decimals).

### 12. Seguretat (`ParsersSegurs` i `AtacsXml`) ###

**Què mostra.** Per defecte, els parsers XML de Java resolen entitats externes. L'exemple ho demostra: un document maliciós fa que el parser per defecte llegeixi un fitxer "secret" del disc i n'inclogui el contingut (atac **XXE**). També mostra l'atac **billion laughs**, en què unes poques línies s'expandeixen fins a ocupar gigues de memòria.

`ParsersSegurs` crea fàbriques DOM, SAX, StAX, Transformer i d'esquemes configurades segons les recomanacions de l'OWASP. La regla més senzilla i efectiva:

```java
fabrica.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
```

Tots els exemples a partir del 4 llegeixen l'XML amb aquestes fàbriques segures.

### 13. Espais de noms (`EspaisDeNoms`) ###

**Què mostra.** Com es treballa amb documents que barregen vocabularis, com les factures electròniques, els serveis SOAP o els fitxers d'Office. La factura de mostra té `emp:nom` (nom d'una empresa) i `nom` d'un altre espai de noms (una nota).

**Detalls importants.**
* Cal activar `setNamespaceAware(true)`. Sense això, `emp:nom` és simplement un element que es diu així.
* `getElementsByTagNameNS(uri, nomLocal)` busca per URI, no pel prefix.
* **Error típic**: una expressió XPath sense prefixos no troba res en un document amb espais de noms. Cal un `NamespaceContext`, i els prefixos de l'expressió **no han de coincidir** amb els del document.
* Per crear elements dins d'un espai de noms: `createElementNS(uri, "prefix:nom")`.

### 14. Fitxers de configuració (`LectorConfiguracio`) ###

**Què mostra.** Com es llegeix la configuració d'una aplicació des d'un XML (com un `pom.xml`, un `web.xml` o un `log4j2.xml`) i es converteix en un record amb els tipus correctes (`int`, `boolean`, `Duration`, `List`). Patrons que s'hi veuen:

* cada paràmetre s'identifica amb un camí relatiu (`baseDades/url`);
* els paràmetres opcionals tenen un valor per defecte i els obligatoris donen un error clar si falten;
* si un valor no té el tipus correcte, l'error diu quin paràmetre falla i quin valor té;
* els valors es poden sobreescriure des de fora (variables d'entorn, línia d'ordres), com fan els frameworks professionals;
* el fitxer té un número de versió i el programa rebutja versions que no coneix.

## Quina tècnica he de fer servir? ##

| Necessitat | Tècnica |
|---|---|
| Llegir i modificar un fitxer petit o mitjà | DOM (exemples 1-3) |
| Consultar dades concretes d'un document | DOM + XPath (exemple 4) |
| Llegir un fitxer molt gran una sola vegada | SAX (exemple 5) o StAX (exemple 6) |
| Escriure un fitxer molt gran | StAX (exemple 6) |
| Treballar amb objectes Java en lloc de nodes | JAXB (exemple 7) |
| Comprovar que un XML que arriba de fora és correcte | XSD (exemple 9) |
| Convertir l'XML en un altre format (HTML, CSV, un altre XML) | XSLT (exemple 10) |
| Intercanviar dades amb una API web | JAXB + Jackson (exemple 11) |
| Processar XML que ve d'una font no fiable | Parsers segurs (exemple 12) |

## Bones pràctiques que es repeteixen ##

* **Parsers segurs** per a qualsevol XML que vingui de fora (exemple 12).
* **`BigDecimal` per als imports**: `double` no pot representar exactament 0,10 i els errors s'acumulen.
* **try-with-resources** per tancar sempre els fitxers, tant si va bé com si hi ha un error.
* **UTF-8 explícit** en llegir i escriure, i al `pom.xml`.
* **Variables a XPath** en lloc de concatenar text (exemple 4).
* **Reutilitzar el que és costós de crear**: `Templates` (XSLT), `JAXBContext`, `Schema` i `ObjectMapper` es poden crear un sol cop i compartir.
* **Excepcions amb informació**: fitxer, línia i columna, i l'excepció original com a causa.
* **Recursos al classpath** en lloc de rutes absolutes, perquè el programa funcioni en qualsevol ordinador.

## Tests i cobertura ##

Cada exemple té la seva classe de tests a `src/test/java`, amb el mateix paquet. Els tests fan servir:

* `@TempDir` per escriure fitxers en carpetes temporals que JUnit esborra en acabar;
* `@ParameterizedTest` per provar el mateix comportament amb diversos valors;
* `@Nested` per agrupar els tests d'un mateix tema;
* `@DisplayName` perquè els resultats es llegeixin en català.

```bash
mvn test                                   # tots els tests
mvn test -Dtest=ConsultesXPathTest         # els tests d'una classe
mvn test -Dtest="LecturaDomTest#comptaElsNodes"  # un sol test
mvn verify                                 # tests + jar + informe de cobertura
```

L'informe de cobertura es genera a `target/site/jacoco/index.html`. `mvn verify` falla si els tests no executen com a mínim el 85% de les línies i el 80% de les branques (`if`, `switch`...). Els valors es poden canviar a les propietats del `pom.xml`.

A cada `push` al repositori, GitHub Actions executa `mvn verify` (fitxer `.github/workflows/tests.yml`) i desa el jar generat.

## Problemes freqüents ##

**`mvn` no es reconeix com a ordre.** Maven no està instal·lat o no és al `PATH`. Comprova-ho amb `mvn -version`, que també mostra quina versió de Java fa servir.

**Error de l'Enforcer: "Cal el JDK 21 o posterior".** Maven fa servir un JDK antic. Revisa la variable `JAVA_HOME`.

**Els accents surten malament a la consola de Windows.** Executa `chcp 65001` abans del programa, o fes servir la terminal de VS Code o Windows Terminal.

**La declaració XML diu `standalone="no"`.** El `Transformer` del JDK l'afegeix quan el document no indica res. És correcte i no afecta la lectura.

**Visual Studio Code no es comporta com esperem.**

* Recarregar la finestra: Paleta de Comandes (**Ctrl+Maj+P**), "**Developer: Reload Window**".
* Netejar l'espai de treball: Paleta de Comandes (**Ctrl+Maj+P**), "**Java: Clean Java Language Server Workspace**".

## Llicència ##

GNU General Public License v3.0. Vegeu el fitxer `LICENSE`.
