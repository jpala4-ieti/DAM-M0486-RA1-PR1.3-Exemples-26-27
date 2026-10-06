<?xml version="1.0" encoding="UTF-8"?>
<!--
    Transforma el catàleg en una pàgina HTML.
    Mostra: paràmetres, plantilles, ordenació, condicions, bucles i format de números.
-->
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

    <xsl:output method="html" encoding="UTF-8" indent="yes"/>

    <!-- Format de números a la catalana: coma per als decimals i punt per als milers -->
    <xsl:decimal-format name="ca" decimal-separator="," grouping-separator="."/>

    <!-- Paràmetre que es pot passar des de Java amb Transformer.setParameter -->
    <xsl:param name="titol" select="'Catàleg'"/>

    <xsl:template match="/cataleg">
        <html lang="ca">
            <head>
                <title><xsl:value-of select="$titol"/></title>
            </head>
            <body>
                <h1><xsl:value-of select="$titol"/></h1>
                <p class="resum">
                    <xsl:value-of select="count(producte)"/>
                    <xsl:text> productes, actualitzat el </xsl:text>
                    <xsl:value-of select="@actualitzat"/>
                </p>
                <!-- Els productes es mostren del més barat al més car -->
                <xsl:apply-templates select="producte">
                    <xsl:sort select="preu" data-type="number" order="ascending"/>
                </xsl:apply-templates>
            </body>
        </html>
    </xsl:template>

    <xsl:template match="producte">
        <article id="{@id}">
            <xsl:attribute name="class">
                <xsl:choose>
                    <xsl:when test="estoc = 0">producte sense-estoc</xsl:when>
                    <xsl:otherwise>producte</xsl:otherwise>
                </xsl:choose>
            </xsl:attribute>
            <h2><xsl:value-of select="nom"/></h2>
            <p class="preu"><xsl:value-of select="format-number(preu, '#.##0,00', 'ca')"/> €</p>
            <xsl:if test="descripcio">
                <p class="descripcio"><xsl:value-of select="descripcio"/></p>
            </xsl:if>
            <xsl:if test="etiquetes/etiqueta">
                <ul class="etiquetes">
                    <xsl:for-each select="etiquetes/etiqueta">
                        <li><xsl:value-of select="."/></li>
                    </xsl:for-each>
                </ul>
            </xsl:if>
        </article>
    </xsl:template>
</xsl:stylesheet>
