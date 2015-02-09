<?xml version="1.0" encoding="utf-8"?>
<!-- Created with Liquid XML 2013 Designer Edition 11.1.0.4725 (http://www.liquid-technologies.com) -->
<xsl:stylesheet version="2.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns="http://www.w3.org/1999/xhtml"
                xmlns:rs="http://restsql.org/schema">
    <xsl:output method="html"
                indent="yes"
                version="4.0"
                omit-xml-declaration="yes"
                exclude-result-prefixes="rs" />
    <xsl:template match="rs:sqlResourceMetaData">
        <html>
            <head>
                <style type="text/css">
                    h2 {
                    text-align: center;
                    }
                    .TableName {
                   background-color: #eef;
                    }
                </style>
            </head>
            <body>
                <h2>Description</h2>
                <xsl:value-of select="documentation/resource"/>
                <h2>Resource Attributes/Parameters</h2>
                <div>
                    <table border="1"
                           class="TableName"
                           align="center"
                           cellspacing="0"
                           cellpadding="0">
                        <thead align="center" style="bold;background-color:#885533">
                            <td>Name</td>
                            <td>Primary Key</td>
                            <td>Data Type</td>
                            <td>Read Only</td>
                            <td>Description</td>
                        </thead>
                        <xsl:for-each select="allReadColumns/column">
                            <xsl:variable name="qualifiedColumnName" select="."/>
                            <tr>
                                <td style=" ">
                                    <xsl:value-of select="/rs:sqlResourceMetaData/tables/table/columns/column[@qualifiedColumnName = $qualifiedColumnName]/@columnLabel" />
                                </td>
                                <td>
                                    <xsl:value-of select="/rs:sqlResourceMetaData/tables/table/columns/column[@qualifiedColumnName = $qualifiedColumnName]/@primaryKey" />
                                </td>
                                <td>
                                    <xsl:value-of select="/rs:sqlResourceMetaData/tables/table/columns/column[@qualifiedColumnName = $qualifiedColumnName]/@columnTypeName" />
                                </td>
                                <td>
                                    <xsl:value-of select="/rs:sqlResourceMetaData/tables/table/columns/column[@qualifiedColumnName = $qualifiedColumnName]/@readOnly" />
                                </td>
                                <td>
                                    <xsl:variable name="columnLabel" select="/rs:sqlResourceMetaData/tables/table/columns/column[@qualifiedColumnName = $qualifiedColumnName]/@columnLabel"/>
                                    <xsl:value-of select="/rs:sqlResourceMetaData/documentation/columns/column[@label=$columnLabel]/description" disable-output-escaping="yes"/>
                                </td>
                            </tr>
                        </xsl:for-each>
                    </table>
                </div>
            
                <h2>Examples</h2>
                <div>
                    <xsl:for-each select="documentation/examples/example">
                    
                    Request
                        Mehod: <xsl:value-of select="request/@method"/>
                        URI: <xsl:value-of select="request/@uri"/>
                    Response
                    <table border="1"
                           class="TableName"
                           align="center"
                           cellspacing="0"
                           cellpadding="0">
                        <tr><td><pre>
                            <xsl:value-of select="response/body" />
                        </pre></td></tr>
					</table>
                        
                        </xsl:for-each>
                </div>


            </body>
        </html>
    </xsl:template>
    <xsl:template match="description">
        <xsl:copy-of select="text() | *"/>
    </xsl:template>

</xsl:stylesheet>
