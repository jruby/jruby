<?xml version="1.0"?> 
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
  <xsl:output method="xml" indent="yes"/>

<xsl:template match="Class">
<BirdInfo>
	<xsl:apply-templates select="Order"/>
</BirdInfo>
</xsl:template>

<xsl:template match="Order">
Order is:  <xsl:value-of select="@Name"/>
	<xsl:apply-templates select="Family"/><xsl:text>
</xsl:text>
</xsl:template>

<xsl:template match="Family">
	Family is:  <xsl:value-of select="@Name"/>
	<xsl:apply-templates select="Species | SubFamily | text()"/>
</xsl:template>

<xsl:template match="SubFamily">
		SubFamily is <xsl:value-of select="@Name"/>
    <xsl:apply-templates select="Species | text()"/>
</xsl:template>

<xsl:template match="Species">
	<xsl:choose>
	  <xsl:when test="name(..)='SubFamily'">
		<xsl:text>	</xsl:text><xsl:value-of select="."/><xsl:text> </xsl:text><xsl:value-of select="@Scientific_Name"/>
	  </xsl:when>
	  <xsl:otherwise>
		<xsl:value-of select="."/><xsl:text> </xsl:text><xsl:value-of select="@Scientific_Name"/>
	  </xsl:otherwise>
	</xsl:choose>
</xsl:template>

</xsl:stylesheet>
