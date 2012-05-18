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
package org.ejbca.util;

import java.util.ArrayList;
import java.util.List;

/** Tools used in command line handling
 * @version $Id: CliTools.java 8280 2009-11-09 14:56:21Z anatom $
 */
public class CliTools {

    /**
     * Using Arrays.asList will return an AbstractList that you cannot remove items from. This List is backed by a new ArrayList.
     * 
     * Example usage for parsing argument switches:
     *  public void method(String[] args) {
     *   List<String> argsList = getAsModifyableList(args);
     *   boolean switch1 = argsList.remove("-switch1");
     *   boolean switch2 = argsList.remove("-switch2");
     *   args = argsList.toArray(new String[0]);
     *   ...
     *  }
     */
    public static List<String> getAsModifyableList(String[] stringArray) {
    	List<String> list = new ArrayList<String>();
    	for (String string : stringArray) {
    		list.add(string);
    	}
    	return list;
    }
}
