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
 
package org.ejbca.util.dn;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.ejbca.util.CertTools;
import org.ietf.ldap.LDAPDN;


/**
 * A class used to retrieve different fields from a Distiguished Name or Subject Alternate Name 
 * or Subject Directory Attributes strings.
 *
 * @author Philip Vendil
 * @version $Id: DNFieldExtractor.java 7099 2009-03-12 08:20:18Z anatom $
 */
public class DNFieldExtractor implements java.io.Serializable {
    private static final Logger log = Logger.getLogger(DNFieldExtractor.class);
    // Public constants
    public static final int TYPE_SUBJECTDN = 0;
    public static final int TYPE_SUBJECTALTNAME = 1;
    public static final int TYPE_SUBJECTDIRATTR = 2;

    // Note, these IDs duplicate values in profilemappings.properties
    
    // Subject DN Fields.
    public static final int E = 0;
    public static final int UID = 1;
    public static final int CN = 2;
    public static final int SN = 3;
    public static final int GIVENNAME = 4;
    public static final int INITIALS = 5;
    public static final int SURNAME = 6;
    public static final int T = 7;
    public static final int OU = 8;
    public static final int O = 9;
    public static final int L = 10;
    public static final int ST = 11;
    public static final int DC = 12;
    public static final int C = 13;
    public static final int UNSTRUCTUREDADDRESS = 14;
    public static final int UNSTRUCTUREDNAME = 15;
    public static final int POSTALCODE = 32;
    public static final int BUSINESSCATEGORY = 33;
    public static final int DN = 34;
    public static final int POSTALADDRESS = 35;
    public static final int TELEPHONENUMBER = 36;
    public static final int PSEUDONYM = 37;
    public static final int STREET = 38;
    public static final int NAME = 55;
    
    // Subject Alternative Names.
    public static final int OTHERNAME = 16;
    public static final int RFC822NAME = 17;
    public static final int DNSNAME = 18;
    public static final int IPADDRESS = 19;
    public static final int X400ADDRESS = 20;
    public static final int DIRECTORYNAME = 21;
    public static final int EDIPARTNAME = 22;
    public static final int URI = 23;
    public static final int REGISTEREDID = 24;
    public static final int UPN = 25;
    public static final int GUID = 26;
    public static final int KRB5PRINCIPAL = 52;
    
    // Subject Directory Attributes
    public static final int DATEOFBIRTH  = 27;
    public static final int PLACEOFBIRTH = 28;
    public static final int GENDER       = 29;
    public static final int COUNTRYOFCITIZENSHIP = 30;
    public static final int COUNTRYOFRESIDENCE   = 31;
    
    /**
     * Creates a new instance of DNFieldExtractor
     *
     * @param dn DOCUMENT ME!
     * @param type DOCUMENT ME!
     */
    public DNFieldExtractor(String dn, int type) {
        dnfields = new HashMap();
        setDN(dn, type);
    }
    
    /** Fields that can be selected in Certificate profile and Publisher
     */
    public static Integer[] getUseFields(int type) {
    	if (type == DNFieldExtractor.TYPE_SUBJECTDN) {
    		return (Integer[])DnComponents.getDnDnIds().toArray(new Integer[0]);
    	} else if (type == DNFieldExtractor.TYPE_SUBJECTALTNAME) {
    		return (Integer[])DnComponents.getAltNameDnIds().toArray(new Integer[0]);
    	} else if (type == DNFieldExtractor.TYPE_SUBJECTDIRATTR) {
    		return (Integer[])DnComponents.getDirAttrDnIds().toArray(new Integer[0]);
    	} else {
    		return new Integer[0];
    	}
    }
    
    public static String getFieldComponent(int field, int type) {
    	if (type == DNFieldExtractor.TYPE_SUBJECTDN) {
    		String ret = DnComponents.getDnExtractorFieldFromDnId(field);
    		return ret;
    	} else if (type == DNFieldExtractor.TYPE_SUBJECTALTNAME) {
    		String ret = DnComponents.getAltNameExtractorFieldFromDnId(field);
    		return ret;
    	} else {
    		String ret = DnComponents.getDirAttrExtractorFieldFromDnId(field);
    		return ret;
    	}
    }

    /**
     * DOCUMENT ME!
     *
     * @param dn DOCUMENT ME!
     * @param type DOCUMENT ME!
     */
    public void setDN(String dn, int type) {    	
        this.type = type;
        ArrayList ids;
        if (type == TYPE_SUBJECTDN) {
        	ids = DnComponents.getDnDnIds();
        } else if (type == TYPE_SUBJECTALTNAME){
        	ids = DnComponents.getAltNameDnIds();
        } else if (type == TYPE_SUBJECTDIRATTR){
        	ids = DnComponents.getDirAttrDnIds();
        } else {
        	ids = new ArrayList();
        }
        fieldnumbers = new HashMap();
        Iterator it = ids.iterator();
        while (it.hasNext()) {
        	Integer id = (Integer)it.next();
            fieldnumbers.put(id, new Integer(0));
        }

        if ((dn != null) && !dn.equalsIgnoreCase("null")) {
            dnfields = new HashMap();

            try {
                String[] dnexploded = LDAPDN.explodeDN(dn, false);

                for (int i = 0; i < dnexploded.length; i++) {
                    boolean exists = false;       
                    Iterator iter = ids.iterator();
                    while (iter.hasNext()) {
                    	Integer id = (Integer)iter.next();
                    	Integer number = (Integer)fieldnumbers.get(id);
                    	String field;
                        if (type == TYPE_SUBJECTDN) {
                        	field = DnComponents.getDnExtractorFieldFromDnId(id.intValue());
                        } else if (type == TYPE_SUBJECTALTNAME){
                        	field = DnComponents.getAltNameExtractorFieldFromDnId(id.intValue());
                        } else {
                        	field = DnComponents.getDirAttrExtractorFieldFromDnId(id.intValue());
                        }
                        String dnex = dnexploded[i].toUpperCase();
                        if (id.intValue() == DNFieldExtractor.URI) {
                            // Fix up URI, which can have several forms
                            if (dnex.indexOf(CertTools.URI.toUpperCase()+"=") > -1) {
                            	field = CertTools.URI.toUpperCase()+"=";
                            }
                            if (dnex.indexOf(CertTools.URI1.toUpperCase()+"=") > -1) {
                            	field = CertTools.URI1.toUpperCase()+"=";
                            }                        	
                        }
                        if (dnex.startsWith(field)) {
                            exists = true;
                            String rdn = LDAPDN.unescapeRDN(dnexploded[i]);
                            // We don't want the CN= (or whatever) part of the RDN
                            if (rdn.toUpperCase().startsWith(field)) {
                                rdn = rdn.substring(field.length(),rdn.length());                                
                            }

                            if (type == TYPE_SUBJECTDN) {
                                dnfields.put(new Integer((id.intValue() * BOUNDRARY) + number.intValue()), rdn);
                            } else if (type == TYPE_SUBJECTALTNAME) {
                                dnfields.put(new Integer((id.intValue() * BOUNDRARY) + number.intValue()), rdn);
                            } else if (type == TYPE_SUBJECTDIRATTR) {
                                dnfields.put(new Integer((id.intValue() * BOUNDRARY) + number.intValue()), rdn);
                            }
                            number = new Integer(number.intValue()+1);
                            fieldnumbers.put(id, number);
                        }
                    }
                    if (!exists) {
                        existsother = true;
                    }
                }
            } catch (Exception e) {
            	log.error("setDN: ", e);
				illegal = true;
                if (type == TYPE_SUBJECTDN) {
                    dnfields.put(new Integer((CN * BOUNDRARY)), "Illegal DN : " + dn);
                } else if (type == TYPE_SUBJECTALTNAME){
                    dnfields.put(new Integer((RFC822NAME * BOUNDRARY)),
                        "Illegal Subjectaltname : " + dn);
                } else if (type == TYPE_SUBJECTDIRATTR){
                    dnfields.put(new Integer((PLACEOFBIRTH * BOUNDRARY)),
                        "Illegal Subjectdirectory attribute : " + dn);
                }
            }
        }
    }

    /**
     * Returns the value of a certain DN component.
     *
     * @param field the DN component, one of the constants DNFieldExtractor.CN, ...
     * @param number the number of the component if several entries for this component exists, normally 0 fir the first
     *
     * @return A String for example "PrimeKey" if DNFieldExtractor.O and 0 was passed, "PrimeKey" if DNFieldExtractor.DC and 0 was passed 
     *         or "com" if DNFieldExtractor.DC and 1 was passed. 
     *         Returns an empty String "", if no such field with the number exists.    
     */
    public String getField(int field, int number) {
        String returnval;
        returnval = (String) dnfields.get(new Integer((field * BOUNDRARY) + number));

        if (returnval == null) {
            returnval = "";
        }

        return returnval;
    }

    /** Returns a string representation of a certain DN component
     * 
     * @param field the DN component, one of the constants DNFieldExtractor.CN, ...
     * @return A String for example "CN=Tomas Gustavsson" if DNFieldExtractor.CN was passed, "DC=PrimeKey,DC=com" if DNFieldExtractor.DC was passed.    
     */
    public String getFieldString(int field){
        String retval = "";
        String fieldname = DnComponents.getDnExtractorFieldFromDnId(field);
        if(type != TYPE_SUBJECTDN){
        	fieldname = DnComponents.getAltNameExtractorFieldFromDnId(field);
        }
        int num = getNumberOfFields(field);
        for(int i=0;i<num;i++){
        	if(retval.length() == 0)
        	  retval += fieldname + getField(field,i);
        	else
        	  retval += "," + fieldname + getField(field,i);	
        }    
        return retval;      	
    }
    

    /**
     * Function that returns true if non standard DN field exists in dn string.
     *
     * @return true if non standard DN field exists, false otherwise
     */
    public boolean existsOther() {
        return existsother;
    }

    /**
     * Returns the number of one kind of dn field.
     *
     * @param field the DN component, one of the constants DNFieldExtractor.CN, ...
     *
     * @return number of componenets available for a fiels, for example 1 if DN is "dc=primekey" and 2 if DN is "dc=primekey,dc=com"
     */
    public int getNumberOfFields(int field) {
        Integer ret = (Integer)fieldnumbers.get(new Integer(field));
        if (ret == null) {
        	log.error("Not finding fieldnumber value for "+field);
        	return 0;
        }
        return ret.intValue();
    }

    /**
     * Returns the complete array determining the number of DN components of the various types 
     * (i.e. if there are two CNs but 0 Ls etc) 
     *
     * @return DOCUMENT ME!
     */
    public HashMap getNumberOfFields() {
        return fieldnumbers;
    }

    public boolean isIllegal(){
    	return illegal;
    }

    private static final int BOUNDRARY = 100;
    // Mapping dnid to number of occurances in this DN
    private HashMap fieldnumbers;
    private HashMap dnfields;
    private boolean existsother = false;
    private boolean illegal = false;
    private int type;
}
