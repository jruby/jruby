<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns="http://www.w3.org/1999/xhtml" version="1.0">
  <xsl:output cdata-section-elements="style" encoding="UTF-8" method="xml" indent="yes" media-type="text/html" version="1.0" doctype-public="-//W3C//DTD XHTML 1.0 Strict//EN" doctype-system="http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd"/>
  <!-- tobiasreif@pinkjuice.com y2001m10d07 -->
  <xsl:strip-space elements="*"/>
  <xsl:template match="/">
    <html>
      <head>
        <title>Birds</title>
        <style type="text/css">
          body {font: 16px arial}
         .subtitle {font-size: 20px}
        </style>
      </head>
      <body>
        <xsl:apply-templates/>
      </body>
    </html>
  </xsl:template>
  <xsl:template match="Class">
    <div>
      <xsl:apply-templates select="Order"/>
    </div>
  </xsl:template>
  <xsl:template match="Order">
    <span class="subtitle">Order: <xsl:value-of select="@Name"/>
    </span>
    <ul>
      <li>
        <xsl:apply-templates select="Family"/>
      </li>
    </ul>
  </xsl:template>
  <xsl:template match="Family">
    <span>Family: <xsl:value-of select="@Name"/>
    </span>
    <ul>
      <xsl:apply-templates select="Species"/>
    </ul>
  </xsl:template>
  <xsl:template match="Species">
    <li>
      <xsl:value-of select="."/>
      <xsl:value-of select="@Scientific_Name"/>
    </li>
  </xsl:template>
</xsl:stylesheet>
