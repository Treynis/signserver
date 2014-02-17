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
package org.signserver.common.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.signserver.common.util.PropertiesParser;
import org.signserver.common.util.PropertiesParser.GlobalProperty;
import org.signserver.common.util.PropertiesParser.WorkerProperty;

import junit.framework.TestCase;

/**
 * Unit tests for the properties parser.
 * 
 * @author Marcus Lundblad
 * @version $Id$
 *
 */
public class PropertiesParserTest extends TestCase {
    
    private static String correctConfig =
            "# some comments...\n" +
            "\n" + // an empty line
            "GLOB.WORKER42.CLASSPATH = foo.bar.Worker\n" +
            "GLOB.WORKER42.SIGNERTOKEN.CLASSPATH = foo.bar.Token\n" +
            "WORKER42.FOOBAR = Some value\n" +
            "WORKERFOO.BAR = VALUE\n" +
            "-WORKER42.REMOVED = REMOVEDVALUE";

    private boolean containsGlobalProperty(final String scope, final String key,
                                            final String value,
                                            final Map<GlobalProperty, String> props) {
        final String foundValue = props.get(new GlobalProperty(scope, key));
        
        return foundValue != null && foundValue.equals(value);
    }
    
    private boolean containsWorkerProperty(final String workerIdOrName,
                                            final String key, final String value,
                                            final Map<WorkerProperty, String> props) {
        final String foundValue = props.get(new WorkerProperty(workerIdOrName, key));
        
        return foundValue != null && foundValue.equals(value);
    }
    
    private boolean containsWorkerProperty(final String workerIdOrName, final String key,
                                            final List<WorkerProperty> props) {
        return props.contains(new WorkerProperty(workerIdOrName, key));
    }
    
    public void testParsingCorrect() throws Exception {
        final Properties prop = new Properties();
        final PropertiesParser parser = new PropertiesParser();
        
        try {
            prop.load(new ByteArrayInputStream(correctConfig.getBytes()));
            parser.process(prop);
            
            final Map<GlobalProperty, String> globalProps = parser.getSetGlobalProperties();
            final Map<WorkerProperty, String> setWorkerProps = parser.getSetWorkerProperties();
            final List<WorkerProperty> removeWorkerProps = parser.getRemoveWorkerProperties();
            
            assertEquals("Number of global properties", 2, globalProps.size());
            assertEquals("Number of worker properties", 2, setWorkerProps.size());
            assertEquals("Number of removed worker properties", 1, removeWorkerProps.size());
            
            assertTrue("Should contain global property",
                    containsGlobalProperty("GLOB.", "WORKER42.CLASSPATH",
                            "foo.bar.Worker", globalProps));
            assertTrue("Should contain global property",
                    containsGlobalProperty("GLOB.", "WORKER42.SIGNERTOKEN.CLASSPATH",
                            "foo.bar.Token", globalProps));
            assertTrue("Should contain worker property",
                    containsWorkerProperty("42", "FOOBAR", "Some value",
                            setWorkerProps));
            assertTrue("Should contain worker property",
                    containsWorkerProperty("FOO", "BAR", "VALUE", setWorkerProps));
            assertTrue("Should contain worker property",
                    containsWorkerProperty("42", "REMOVED", removeWorkerProps));
            
        } catch (IOException e) {
            fail("Failed to parse properties");
        }
    }

}
