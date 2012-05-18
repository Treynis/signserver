/*************************************************************************
 *                                                                       *
 *  EJBCA: The OpenSource Certificate Authority                          *
 *                                                                       *
 *  This software is free software; you can redistribute it and/or       *
 *  modify it under the terms of the GNU Lesser General Public           *
 *  License as published by the Free Software Foundation; either         *
 *  version 2.1 of the License, or any later version.                    *
 *                                                                       *
 *  See terms of license at gnu.org.                                     *
 *                                                                       *
 *************************************************************************/
 
package org.ejbca.ui.web.admin.configuration;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ServletContext;

import org.apache.log4j.Logger;
import org.ejbca.core.model.InternalResources;
import org.ejbca.core.model.ra.raadmin.GlobalConfiguration;


/**
 * An class interpreting the language properties files. I contains one method getText that returns
 * the presented text in the users preferred language.
 *
 * @author  Philip Vendil
 * @version $Id: WebLanguages.java 7722 2009-06-12 11:43:15Z anatom $
 */
public class WebLanguages implements java.io.Serializable {
    private static final Logger log = Logger.getLogger(WebLanguages.class);

    /** Internal localization of logs and errors */
    private static final InternalResources intres = InternalResources.getInstance();

    /** Constructor used to load static content. An instance must be declared with this constructor before
     *  any WebLanguage object can be used. */
    /** Special constructor used by Ejbca web bean */
    private void init(ServletContext servletContext, GlobalConfiguration globalconfiguration) throws IOException {
        if(languages == null){
            // Get available languages.
            availablelanguages=null;
            
            String availablelanguagesstring = globalconfiguration.getAvailableLanguagesAsString();
            availablelanguages =  availablelanguagesstring.split(",");
            for(int i=0; i < availablelanguages.length;i++){
                availablelanguages[i] =  availablelanguages[i].trim().toUpperCase();
            }
            // Load available languages
            languages = new LanguageProperties[availablelanguages.length];
            for(int i = 0; i < availablelanguages.length; i++){
                languages[i] = new LanguageProperties();
                String propsfile = "/" + globalconfiguration.getLanguagePath() + "/"
                + globalconfiguration.getLanguageFilename() + "."
                + availablelanguages[i].toLowerCase() +".properties";
                
                InputStream is = null;
                if (servletContext != null) {
                	is = servletContext.getResourceAsStream(propsfile);
                } else {
                    is = this.getClass().getResourceAsStream(propsfile);                	
                }
                if(is==null) {
                    //if not available as stream, try it as a file
                    is = new FileInputStream("/tmp"+propsfile);
                }
                if (log.isDebugEnabled()) {
                	log.debug("Loading language from file: "+propsfile);
                }
                languages[i].load(is);
            }
        }
    }

    public WebLanguages(ServletContext servletContext, GlobalConfiguration globalconfiguration, int preferedlang, int secondarylang) throws IOException {
        init(servletContext, globalconfiguration);
        this.userspreferedlanguage=preferedlang;
        this.userssecondarylanguage=secondarylang;
    }


    /** The main method that looks up the template text in the users preferred language. */
    public  String getText(String template){
      String returnvalue = null;
      try{
        returnvalue= languages[userspreferedlanguage].getProperty(template);
        if(returnvalue == null){
          returnvalue= languages[userssecondarylanguage].getProperty(template);
        }
        if(returnvalue == null){
            returnvalue= intres.getLocalizedMessage(template);
        }        
      }catch(java.lang.NullPointerException e){}
      if(returnvalue == null)
        returnvalue= template;
      return returnvalue;
    }

    /* Returns a text string containing the available languages */
    public String[] getAvailableLanguages(){
      return availablelanguages;
    }


    // Protected fields
    private int userspreferedlanguage;
    private int userssecondarylanguage;

    private String[] availablelanguages;
    private LanguageProperties[] languages = null;

}
