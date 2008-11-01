/*************************************************************************
 *                                                                       *
 *  SignServer: The OpenSource Automated Signing Server                  *
 *                                                                       *
 *  This software is free software; you can redistribute it and/or       *
 *  modify it under the terms of the GNU Lesser General Public           *
 *  License as published by the Free Software Foundation; either         *
 *  version 2.1 of the License, or any later version.                    *
 *                                                                       *
 *  See terms of license at gnu.org.                                     *
 *                                                                       *
 *************************************************************************/
 
package org.signserver.testutils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;





/**
 * Class containing utility methods used to simplify testing.
 * 
 * 
 * @author Philip Vendil 21 okt 2007
 *
 * @version $Id: TestUtils.java,v 1.1 2007-10-28 12:27:54 herrvendil Exp $
 */

public class TestUtils {
	
	/**
	 * A simple grep util that searches a large string if the substring exists.
	 * @param inString the input data
	 * @param searchstring the text to search for.
	 * @return true if searchstring exists
	 */
   public static boolean grep(String inString, String searchstring){
	   Pattern p = Pattern.compile(searchstring);
       // Create a matcher with an input string
       Matcher m = p.matcher(inString);
       return m.find();
   }
   
   /**
    * Method to see if the matchString is a subset of all the output
    * in the temporary system output buffer. 
    * @param matchString the string to search for
    * @return true if it exists.
    */
   public static boolean grepTempOut(String matchString){
	   return grep(new String(tempOutputStream.toByteArray()), matchString);
   }
   
   /**
    * Method used to redirect OutputStream to a temporate buffer
    * so it is possible to search for matching values later.
    */
   public static void redirectToTempOut(){
	   stdOut = System.out;
	   tempOutputStream = new ByteArrayOutputStream();	   
	   System.setOut(new PrintStream(tempOutputStream));
   }
   private static ByteArrayOutputStream tempOutputStream;
   private static PrintStream stdOut;
   
   /**
    * Method used to clear the current content of the
    * temporary output stream.
 * @throws IOException 
    */
   public static void flushTempOut() {
	   tempOutputStream = new ByteArrayOutputStream();
	   System.setOut(new PrintStream(tempOutputStream));
   }
   
   /**
    * Method to see if the matchString is a subset of all the output
    * in the temporary system error buffer. 
    * @param matchString the string to search for
    * @return true if it exists.
    */
   public static boolean grepTempErr(String matchString){
	   return grep(new String(tempErrorStream.toByteArray()), matchString);
   }
   
   /**
    * Method used to redirect error stream to a temporary buffer
    * so it is possible to search for matching values later.
    */
   public static void redirectToTempErr(){
	   //stdErr = System.err;
	   tempErrorStream = new ByteArrayOutputStream();	   
	   System.setErr(new PrintStream(tempErrorStream));
   }
   private static ByteArrayOutputStream tempErrorStream;
   //private static PrintStream stdErr;
   
   /**
    * Method used to clear the current content of the
    * temporary error stream.
    * @throws IOException 
    */
   public static void flushTempErr(){
	   tempErrorStream = new ByteArrayOutputStream();
	   System.setErr(new PrintStream(tempErrorStream));
   }
   
   /**
    * Method used to print the contents in TempOut to System.out
    */
   public static void printTempOut(){
	   stdOut.print(tempOutputStream);
   }
   
   /**
    * Method used to print the contents in TempErr to System.out
    */
   public static void printTempErr(){
	   stdOut.print(tempErrorStream);
   }

}
