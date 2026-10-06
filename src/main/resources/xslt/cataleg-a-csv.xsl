<?xml version="1.0" encoding="UTF-8"?>
<!--
    Transforma el catàleg en un fitxer CSV (el format que obre directament un full de càlcul).
    Mostra: sortida de text, paràmetres i plantilles amb nom recursives.
-->
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

    <xsl:output method="text" encoding="UTF-8"/>

    <!-- El punt i coma és el separador habitual als fulls de càlcul en català -->
    <xsl:param name="separador" select="';'"/>

    <xsl:variable name="salt" select="'&#10;'"/>

    <xsl:template match="/cataleg">
        <xsl:value-of select="concat('id', $separador, 'nom', $separador, 'categoria', $separador, 'preu', $separador, 'estoc', $salt)"/>
        <xsl:for-each select="producte">
            <xsl:value-of select="concat(@id, $separador)"/>
            <xsl:call-template name="camp-text">
                <xsl:with-param name="text" select="nom"/>
            </xsl:call-template>
            <xsl:value-of select="concat($separador, @categoria, $separador, preu, $separador, estoc, $salt)"/>
        </xsl:for-each>
    </xsl:template>

    <!-- Un camp de text va entre cometes i les cometes internes es dupliquen (norma del CSV) -->
    <xsl:template name="camp-text">
        <xsl:param name="text"/>
        <xsl:text>"</xsl:text>
        <xsl:call-template name="duplicar-cometes">
            <xsl:with-param name="text" select="$text"/>
        </xsl:call-template>
        <xsl:text>"</xsl:text>
    </xsl:template>

    <!-- XSLT 1.0 no té replace(): es fa amb una plantilla recursiva -->
    <xsl:template name="duplicar-cometes">
        <xsl:param name="text"/>
        <xsl:choose>
            <xsl:when test="contains($text, '&quot;')">
                <xsl:value-of select="substring-before($text, '&quot;')"/>
                <xsl:text>""</xsl:text>
                <xsl:call-template name="duplicar-cometes">
                    <xsl:with-param name="text" select="substring-after($text, '&quot;')"/>
                </xsl:call-template>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="$text"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
</xsl:stylesheet>
