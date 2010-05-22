<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:fn="http://www.w3.org/2005/xpath-functions">
	<xsl:output method="xml" version="1.0" encoding="UTF-8" indent="yes"/>

	<xsl:template match="results">
	
		<xsl:element name="checkstyle">		 
			<xsl:attribute name="xmlns_xsi">
				<xsl:text>http://www.w3.org/2001/XMLSchema-instance</xsl:text>
			</xsl:attribute>			
			<xsl:attribute name="version">
				<xsl:text>4.4</xsl:text>
			</xsl:attribute>
		
			<xsl:for-each-group select="error" group-by="@file">
			
				<xsl:element name="file">
					<xsl:attribute name="name">
						<xsl:value-of select="@file"/>
					</xsl:attribute>
						
					<xsl:for-each select="current-group()">
				
						<xsl:element name="error">
							<xsl:attribute name="line">
								<xsl:value-of select="@line"/>
							</xsl:attribute>
							<xsl:attribute name="severity">
								<xsl:value-of select="@severity"/>
							</xsl:attribute>
							<xsl:attribute name="message">
								<xsl:value-of select="@msg"/>
							</xsl:attribute>
							<xsl:attribute name="source">
								<xsl:value-of select="@id"/>
							</xsl:attribute>
						</xsl:element>
					
    				</xsl:for-each>
				</xsl:element>

			</xsl:for-each-group>
		</xsl:element>
	</xsl:template>
</xsl:stylesheet>
