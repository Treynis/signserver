/************************************************************************
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER
 *
 * Copyright 2008 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at http://www.apache.org/licenses/LICENSE-2.0. You can also
 * obtain a copy of the License at http://odftoolkit.org/docs/license.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 ************************************************************************/

/*
 * This file is automatically generated.
 * Don't edit manually.
 */    

package org.odftoolkit.odfdom.dom.element.office;

import org.odftoolkit.odfdom.OdfName;
import org.odftoolkit.odfdom.OdfNamespace;
import org.odftoolkit.odfdom.OdfFileDom;
import org.odftoolkit.odfdom.dom.OdfNamespaceNames;
import org.odftoolkit.odfdom.OdfElement;

import org.odftoolkit.odfdom.dom.element.db.DbDataSourceElement;
import org.odftoolkit.odfdom.dom.element.db.DbFormsElement;
import org.odftoolkit.odfdom.dom.element.db.DbReportsElement;
import org.odftoolkit.odfdom.dom.element.db.DbQueriesElement;
import org.odftoolkit.odfdom.dom.element.db.DbTableRepresentationsElement;
import org.odftoolkit.odfdom.dom.element.db.DbSchemaDefinitionElement;

/**
 * DOM implementation of OpenDocument element  {@odf.element office:database}.
 *
 */
public abstract class OfficeDatabaseElement extends OdfElement
{        
    public static final OdfName ELEMENT_NAME = OdfName.get( OdfNamespace.get(OdfNamespaceNames.OFFICE), "database" );


	/**
	 * Create the instance of <code>OfficeDatabaseElement</code> 
	 *
	 * @param  ownerDoc     The type is <code>OdfFileDom</code>
	 */
	public OfficeDatabaseElement( OdfFileDom ownerDoc )
	{
		super( ownerDoc, ELEMENT_NAME	);
	}

	/**
	 * Get the element name 
	 *
	 * @return  return   <code>OdfName</code> the name of element {@odf.element office:database}.
	 */
	public OdfName getOdfName()
	{
		return ELEMENT_NAME;
	}


	/**
	 * Create child element {@odf.element db:data-source}.
	 *
	 * @return   return  the element {@odf.element db:data-source}
	 * DifferentQName 
	 */
	public DbDataSourceElement newDbDataSourceElement()
	{
		DbDataSourceElement  dbDataSource = ((OdfFileDom)this.ownerDocument).newOdfElement(DbDataSourceElement.class);
		this.appendChild( dbDataSource);
		return  dbDataSource;
	}                   
               
	/**
	 * Create child element {@odf.element db:forms}.
	 *
	 * @return   return  the element {@odf.element db:forms}
	 * DifferentQName 
	 */
	public DbFormsElement newDbFormsElement()
	{
		DbFormsElement  dbForms = ((OdfFileDom)this.ownerDocument).newOdfElement(DbFormsElement.class);
		this.appendChild( dbForms);
		return  dbForms;
	}                   
               
	/**
	 * Create child element {@odf.element db:reports}.
	 *
	 * @return   return  the element {@odf.element db:reports}
	 * DifferentQName 
	 */
	public DbReportsElement newDbReportsElement()
	{
		DbReportsElement  dbReports = ((OdfFileDom)this.ownerDocument).newOdfElement(DbReportsElement.class);
		this.appendChild( dbReports);
		return  dbReports;
	}                   
               
	/**
	 * Create child element {@odf.element db:queries}.
	 *
	 * @return   return  the element {@odf.element db:queries}
	 * DifferentQName 
	 */
	public DbQueriesElement newDbQueriesElement()
	{
		DbQueriesElement  dbQueries = ((OdfFileDom)this.ownerDocument).newOdfElement(DbQueriesElement.class);
		this.appendChild( dbQueries);
		return  dbQueries;
	}                   
               
	/**
	 * Create child element {@odf.element db:table-representations}.
	 *
	 * @return   return  the element {@odf.element db:table-representations}
	 * DifferentQName 
	 */
	public DbTableRepresentationsElement newDbTableRepresentationsElement()
	{
		DbTableRepresentationsElement  dbTableRepresentations = ((OdfFileDom)this.ownerDocument).newOdfElement(DbTableRepresentationsElement.class);
		this.appendChild( dbTableRepresentations);
		return  dbTableRepresentations;
	}                   
               
	/**
	 * Create child element {@odf.element db:schema-definition}.
	 *
	 * @return   return  the element {@odf.element db:schema-definition}
	 * DifferentQName 
	 */
	public DbSchemaDefinitionElement newDbSchemaDefinitionElement()
	{
		DbSchemaDefinitionElement  dbSchemaDefinition = ((OdfFileDom)this.ownerDocument).newOdfElement(DbSchemaDefinitionElement.class);
		this.appendChild( dbSchemaDefinition);
		return  dbSchemaDefinition;
	}                   
               
}
