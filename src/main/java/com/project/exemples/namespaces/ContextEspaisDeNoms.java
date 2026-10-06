package com.project.exemples.namespaces;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import java.util.Iterator;
import java.util.Map;

/**
 * Indica a XPath quin URI correspon a cada prefix que es fa servir a les expressions.
 *
 * <p>Els prefixos de l'expressió <b>no</b> han de coincidir amb els del document: el que
 * compta és l'URI. El document pot dir {@code fac:factura} i l'expressió {@code f:factura}.</p>
 */
public final class ContextEspaisDeNoms implements NamespaceContext {

    private final Map<String, String> uriPerPrefix;

    /**
     * @param uriPerPrefix associació entre prefixos i URI
     */
    public ContextEspaisDeNoms(Map<String, String> uriPerPrefix) {
        this.uriPerPrefix = Map.copyOf(uriPerPrefix);
    }

    @Override
    public String getNamespaceURI(String prefix) {
        if (prefix == null) {
            throw new IllegalArgumentException("El prefix no pot ser null");
        }
        return switch (prefix) {
            case XMLConstants.XML_NS_PREFIX -> XMLConstants.XML_NS_URI;
            case XMLConstants.XMLNS_ATTRIBUTE -> XMLConstants.XMLNS_ATTRIBUTE_NS_URI;
            default -> uriPerPrefix.getOrDefault(prefix, XMLConstants.NULL_NS_URI);
        };
    }

    @Override
    public String getPrefix(String uri) {
        Iterator<String> prefixos = getPrefixes(uri);
        return prefixos.hasNext() ? prefixos.next() : null;
    }

    @Override
    public Iterator<String> getPrefixes(String uri) {
        return uriPerPrefix.entrySet().stream()
                .filter(entrada -> entrada.getValue().equals(uri))
                .map(Map.Entry::getKey)
                .sorted()
                .iterator();
    }
}
