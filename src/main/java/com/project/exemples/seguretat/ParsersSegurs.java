package com.project.exemples.seguretat;

import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.validation.SchemaFactory;

/**
 * Fàbriques de parsers configurades per processar XML que ve de fora (d'un usuari, d'una API,
 * d'un fitxer pujat...) sense córrer riscos.
 *
 * <p>Els parsers XML de Java, per defecte, resolen entitats externes: un document pot incloure
 * el contingut d'un fitxer del servidor o fer peticions a altres màquines (atac XXE). També
 * accepten entitats niades que s'expandeixen de manera exponencial ("billion laughs").
 * La configuració d'aquesta classe segueix les recomanacions de l'OWASP.</p>
 *
 * <p>La regla més simple i efectiva: <b>si no necessites DTD, prohibeix el DOCTYPE</b>.</p>
 */
public final class ParsersSegurs {

    private static final String PROHIBIR_DOCTYPE = "http://apache.org/xml/features/disallow-doctype-decl";
    private static final String ENTITATS_GENERALS_EXTERNES = "http://xml.org/sax/features/external-general-entities";
    private static final String ENTITATS_PARAMETRE_EXTERNES = "http://xml.org/sax/features/external-parameter-entities";
    private static final String CARREGAR_DTD_EXTERNA = "http://apache.org/xml/features/nonvalidating/load-external-dtd";

    private ParsersSegurs() {
    }

    /**
     * Fàbrica DOM segura.
     *
     * @return fàbrica nova, a punt per crear {@code DocumentBuilder}
     */
    public static DocumentBuilderFactory fabricaDomSegura() {
        DocumentBuilderFactory fabrica = DocumentBuilderFactory.newInstance();
        try {
            // 1. Prohibeix qualsevol DOCTYPE: elimina d'arrel els atacs XXE i "billion laughs"
            fabrica.setFeature(PROHIBIR_DOCTYPE, true);
            // 2. Defensa en profunditat, per si algun dia s'ha de permetre el DOCTYPE
            fabrica.setFeature(ENTITATS_GENERALS_EXTERNES, false);
            fabrica.setFeature(ENTITATS_PARAMETRE_EXTERNES, false);
            fabrica.setFeature(CARREGAR_DTD_EXTERNA, false);
            // 3. Activa els límits de processament segur del JDK (expansions, mida dels noms...)
            fabrica.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException("El parser DOM no admet la configuració segura", e);
        }
        // 4. Cap accés a DTD ni esquemes externs (cadena buida = cap protocol permès)
        fabrica.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        fabrica.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        fabrica.setXIncludeAware(false);
        fabrica.setExpandEntityReferences(false);
        return fabrica;
    }

    /**
     * Fàbrica DOM tal com ve per defecte. Només es fa servir per comparar-la amb la segura.
     *
     * @return fàbrica nova sense cap configuració
     */
    public static DocumentBuilderFactory fabricaDomPerDefecte() {
        return DocumentBuilderFactory.newInstance();
    }

    /**
     * Fàbrica SAX segura.
     *
     * @return fàbrica nova, a punt per crear {@code SAXParser}
     */
    public static SAXParserFactory fabricaSaxSegura() {
        SAXParserFactory fabrica = SAXParserFactory.newInstance();
        try {
            fabrica.setFeature(PROHIBIR_DOCTYPE, true);
            fabrica.setFeature(ENTITATS_GENERALS_EXTERNES, false);
            fabrica.setFeature(ENTITATS_PARAMETRE_EXTERNES, false);
            fabrica.setFeature(CARREGAR_DTD_EXTERNA, false);
            fabrica.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        } catch (ParserConfigurationException | SAXException e) {
            throw new IllegalStateException("El parser SAX no admet la configuració segura", e);
        }
        return fabrica;
    }

    /**
     * Fàbrica StAX segura: sense DTD ni entitats externes.
     *
     * @return fàbrica nova, a punt per crear lectors StAX
     */
    public static XMLInputFactory fabricaStaxSegura() {
        XMLInputFactory fabrica = XMLInputFactory.newInstance();
        fabrica.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        fabrica.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        return fabrica;
    }

    /**
     * Fàbrica de transformadors (Transformer i XSLT) que no accedeix a recursos externs.
     *
     * @return fàbrica nova
     */
    public static TransformerFactory fabricaTransformerSegura() {
        TransformerFactory fabrica = TransformerFactory.newInstance();
        try {
            fabrica.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        } catch (TransformerConfigurationException e) {
            throw new IllegalStateException("La fàbrica de transformadors no admet el processament segur", e);
        }
        fabrica.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        fabrica.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
        return fabrica;
    }

    /**
     * Fàbrica d'esquemes XSD que no carrega DTD ni esquemes externs.
     *
     * @return fàbrica nova
     */
    public static SchemaFactory fabricaEsquemesSegura() {
        SchemaFactory fabrica = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        try {
            fabrica.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            fabrica.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            fabrica.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        } catch (SAXException e) {
            throw new IllegalStateException("La fàbrica d'esquemes no admet la configuració segura", e);
        }
        return fabrica;
    }
}
