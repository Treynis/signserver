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
package org.signserver.client.cli;

import java.io.*;
import java.util.ArrayList;
import java.util.Properties;
import org.apache.log4j.Logger;
import org.ejbca.ui.cli.util.ConsolePasswordReader;
import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;
import org.signserver.cli.spi.CommandFailureException;
import org.signserver.cli.spi.IllegalCommandArgumentsException;
import org.signserver.client.cli.defaultimpl.SignDocumentCommand;
import org.signserver.common.SignServerUtil;
import org.signserver.ejb.interfaces.IWorkerSession;
import org.signserver.testutils.ModulesTestCase;
import org.signserver.testutils.TestingSecurityManager;
import org.junit.Test;
import org.signserver.cli.spi.CommandContext;
import org.signserver.cli.spi.CommandFactoryContext;

/**
 * Tests for the signdocument command of Client CLI.
 *
 * @author Markus Kilås
 * @version $Id$
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class DocumentSignerTest extends ModulesTestCase {

    /** Logger for this class. */
    private static final Logger LOG = Logger.getLogger(DocumentSignerTest.class);

    /** WORKERID used in this test case as defined in 
     * junittest-part-config.properties for XMLSigner. */
    private static final int WORKERID = 5676;

    /** WORKERID used in this test case as defined in 
     * junittest-part-config.properties for PDFSigner. */
    private static final int WORKERID2 = 5675;

    /** Worker ID for the dummy metadata echo signer. */
    private static final int WORKERID3 = 6676;

    private static final int[] WORKERS = new int[] {5676, 5679, 5681, 5682, 5683, 5802, 5803, 6676};

    private static String signserverhome;
    
    private final IWorkerSession workerSession = getWorkerSession();
    
    @Before
    public void setUp() throws Exception {
        SignServerUtil.installBCProvider();
        TestingSecurityManager.install();
        signserverhome = System.getenv("SIGNSERVER_HOME");
        LOG.info("HOME:"+signserverhome);
        assertNotNull("Please set SIGNSERVER_HOME environment variable", signserverhome);
        setupSSLKeystores();
    }

    @After
    public void tearDown() throws Exception {
        TestingSecurityManager.remove();
    }	
	
    @Test
    public void test00SetupDatabase() throws Exception {
        // Worker 1
        setProperties(new File(signserverhome, "res/test/test-xmlsigner-configuration.properties"));
        workerSession.reloadConfiguration(WORKERID);
        
        // Worker 2
        setProperties(new File(signserverhome, "res/test/test-pdfsigner-configuration.properties"));
        workerSession.reloadConfiguration(WORKERID2);
        
        // Worker 3 (dummy signer echoing request metadata)
        setProperties(new File(signserverhome, "res/test/test-echometadata-configuration.properties"));
        workerSession.reloadConfiguration(WORKERID3);
    }

    @Test
    public void test01missingArguments() throws Exception {
        try {
            execute("signdocument");
            fail("Should have thrown exception about missing arguments");
        } catch (IllegalCommandArgumentsException expected) {} // NOPMD
    }

    /**
     * Tests the sample use case a from the documentation.
     * <pre>
     * a) signdocument -workername XMLSigner -data "&lt;root/&gt;"
     * </pre>
     * @throws Exception
     */
    @Test
    public void test02signDocumentFromParameter() throws Exception {
        try {
            String res =
                    new String(execute("signdocument", "-workername", "TestXMLSigner", "-data", "<root/>"));
            assertTrue("contains signature tag: "
                    + res, res.contains("<root><Signature"));
        } catch (IllegalCommandArgumentsException ex) {
            LOG.error("Execution failed", ex);
            fail(ex.getMessage());
        }
    }

    /**
     * Tests the sample use case b from the documentation.
     * <pre>
     * b) signdocument -workername XMLSigner -infile /tmp/document.xml
     * </pre>
     * @throws Exception
     */
    @Test
    public void test02signDocumentFromFile() throws Exception {
        try {
            final File doc = File.createTempFile("test.xml", null);
            FileOutputStream out = null;
            try {
                out = new FileOutputStream(doc);
                out.write("<tag/>".getBytes());
                out.close();
            } finally {
                if (out != null) {
                    out.close();
                }
            }

            String res =
                    new String(execute("signdocument", "-workername", 
                    "TestXMLSigner", "-infile", doc.getAbsolutePath()));
            assertTrue("contains signature tag: "
                    + res, res.contains("<tag><Signature"));
        } catch (IllegalCommandArgumentsException ex) {
            LOG.error("Execution failed", ex);
            fail(ex.getMessage());
        }
    }

    /**
     * Test for the "-pdfpassword" argument.
     * signdocument -workername TestPDFSigner -infile $SIGNSERVER_HOME/res/test/pdf/sample-open123.pdf
     * @throws Exception
     */
    @Test
    public void test03signPDFwithPasswordOverHTTP() throws Exception {
        try {

            byte[] res = execute("signdocument", "-workername", 
                    "TestPDFSigner", "-infile", signserverhome + "/res/test/pdf/sample-open123.pdf",
                    "-pdfpassword", "open123");
            assertNotNull("No result", res);
            assertNotSame("Empty result", 0, res.length);
        } catch (IllegalCommandArgumentsException ex) {
            LOG.error("Execution failed", ex);
            fail(ex.getMessage());
        }
    }
    
    /**
     * Test for the "-pdfpassword" argument.
     * signdocument -workername TestPDFSigner -infile $SIGNSERVER_HOME/res/test/pdf/sample-open123.pdf -protocol WEBSERVICES
     * @throws Exception
     */
    @Test
    public void test04signPDFwithPasswordOverWebservices() throws Exception {
        try {
            
            byte[] res = execute("signdocument", "-workername", 
                    "TestPDFSigner", "-infile", signserverhome + "/res/test/pdf/sample-open123.pdf",
                    "-pdfpassword", "open123", "-protocol", "WEBSERVICES",
                    "-truststore", signserverhome + "/p12/truststore.jks", "-truststorepwd", "changeit",
                    "-host", getHTTPHost(), "-port", String.valueOf(getPublicHTTPSPort()));
            assertNotNull("No result", res);
            assertNotSame("Empty result", 0, res.length);
        } catch (IllegalCommandArgumentsException ex) {
            LOG.error("Execution failed", ex);
            fail(ex.getMessage());
        }
    }
    
    /**
     * Test for the "-pdfpassword" argument.
     * signdocument -workername TestPDFSigner -infile $SIGNSERVER_HOME/res/test/pdf/sample-open123.pdf -protocol CLIENTWS
     * @throws Exception
     */
    @Test
    public void test04signPDFwithPasswordOverClientWS() throws Exception {
        try {
            
            byte[] res = execute("signdocument", "-workername", 
                    "TestPDFSigner", "-infile", signserverhome + "/res/test/pdf/sample-open123.pdf",
                    "-pdfpassword", "open123", "-protocol", "CLIENTWS",
                    "-truststore", signserverhome + "/p12/truststore.jks", "-truststorepwd", "changeit",
                    "-host", getHTTPHost(), "-port", String.valueOf(getPublicHTTPSPort()));
            assertNotNull("No result", res);
            assertNotSame("Empty result", 0, res.length);
        } catch (IllegalCommandArgumentsException ex) {
            LOG.error("Execution failed", ex);
            fail(ex.getMessage());
        }
    }

    /**
     * Test signing over webservices with the -servlet argument set as SignServerWSService/SignServerWS
     * @throws Exception
     */
    @Test
    public void test05signPDFOverWebservicesServletArg() throws Exception {
        try {
            final String res = new String(execute("signdocument", "-workername", "TestXMLSigner",
            		"-data", "<root/>", "-protocol", "WEBSERVICES",
            		"-servlet", "/signserver/SignServerWSService/SignServerWS?wsdl",
            		"-truststore", signserverhome + "/p12/truststore.jks", "-truststorepwd", "changeit",
                        "-host", getHTTPHost(), "-port", String.valueOf(getPublicHTTPSPort())));
            assertTrue("contains signature tag: "
                    + res, res.contains("<root><Signature"));
        } catch (IllegalCommandArgumentsException ex) {
            LOG.error("Execution failed", ex);
            fail(ex.getMessage());
        }
    }
    
    /**
     * Test signing over webservices with the -servlet argument set as ClientWSService/ClientWS
     * @throws Exception
     */
    @Test
    public void test05signPDFOverClientWSServletArg() throws Exception {
        try {
            final String res = new String(execute("signdocument", "-workername", "TestXMLSigner",
            		"-data", "<root/>", "-protocol", "CLIENTWS",
            		"-servlet", "/signserver/ClientWSService/ClientWS?wsdl",
            		"-truststore", signserverhome + "/p12/truststore.jks", "-truststorepwd", "changeit",
                        "-host", getHTTPHost(), "-port", String.valueOf(getPublicHTTPSPort())));
            assertTrue("contains signature tag: "
                    + res, res.contains("<root><Signature"));
        } catch (IllegalCommandArgumentsException ex) {
            LOG.error("Execution failed", ex);
            fail(ex.getMessage());
        }
    }
    
    /**
     * Test signing over webservices with the -servlet argument set as signserverws/signserverws
     * @throws Exception
     */
    @Test
    public void test06signPDFOverWebservicesServletArg2() throws Exception {
        try {
            final String res = new String(execute("signdocument", "-workername", "TestXMLSigner",
                        "-data", "<root/>", "-protocol", "WEBSERVICES",
                        "-servlet", "/signserver/signserverws/signserverws?wsdl",
                        "-truststore", signserverhome + "/p12/truststore.jks", "-truststorepwd", "changeit",
                        "-host", getHTTPHost(), "-port", String.valueOf(getPublicHTTPSPort())));
            assertTrue("contains signature tag: "
                    + res, res.contains("<root><Signature"));
        } catch (IllegalCommandArgumentsException ex) {
            LOG.error("Execution failed", ex);
            fail(ex.getMessage());
        }
    }
    
    /**
     * Test signing over webservices with the -servlet argument set as an invalid WS servlet URL
     * @throws Exception
     */
    @Test
    public void test07signPDFOverWebservicesServletArgInvalid() throws Exception {
        try {
            final String res = new String(execute("signdocument", "-workername", "TestXMLSigner",
                        "-data", "<root/>", "-protocol", "WEBSERVICES",
                        "-servlet", "/signserver/nonexistant/wsurl",
                        "-truststore", signserverhome + "/p12/truststore.jks", "-truststorepwd", "changeit",
                        "-host", getHTTPHost(), "-port", String.valueOf(getPublicHTTPSPort())));
            fail("Should not accept invalid WS -servlet argument");
        } catch (IllegalCommandArgumentsException ex) {
            LOG.error("Execution failed", ex);
            fail(ex.getMessage());
        } catch (Exception ex) {
            // this is expected for the invalid URL
        }
    }
    
    /**
     * Test signing over webservices with the -servlet argument set as an invalid WS servlet URL
     * @throws Exception
     */
    @Test
    public void test07signPDFOverClientWSServletArgInvalid() throws Exception {
        try {
            final String res = new String(execute("signdocument", "-workername", "TestXMLSigner",
                        "-data", "<root/>", "-protocol", "CLIENTWS",
                        "-servlet", "/signserver/nonexistant/wsurl",
                        "-truststore", signserverhome + "/p12/truststore.jks", "-truststorepwd", "changeit",
                        "-host", getHTTPHost(), "-port", String.valueOf(getPublicHTTPSPort())));
            fail("Should not accept invalid WS -servlet argument");
        } catch (IllegalCommandArgumentsException ex) {
            LOG.error("Execution failed", ex);
            fail(ex.getMessage());
        } catch (Exception ex) {
            // this is expected for the invalid URL
        }
    }

    /**
     * Test signing a document supplying an additional metadata parameter.
     * 
     * @throws Exception
     */
    @Test
    public void test08signDocumentWithMetadata() throws Exception {
        try {
            String res =
                    new String(execute("signdocument", "-workername", "EchoRequestMetadataSigner", "-data", "<root/>",
                            "-metadata", "foo=bar"));
            assertTrue("contains metadata parameter: "
                    + res, res.contains("foo=bar"));
        } catch (IllegalCommandArgumentsException ex) {
            LOG.error("Execution failed", ex);
            fail(ex.getMessage());
        }
    }
    
    /**
     * Test signing a document supplying additional metadata parameters (more than one occurance).
     * 
     * @throws Exception
     */
    @Test
    public void test09signDocumentWithMetadataMultipleParams() throws Exception {
        try {
            String res =
                    new String(execute("signdocument", "-workername", "EchoRequestMetadataSigner", "-data", "<root/>",
                            "-metadata", "foo=bar", "-metadata", "foo2=bar2"));
            assertTrue("contains metadata parameter: "
                    + res, res.contains("foo=bar"));
            assertTrue("contains metadata parameter: "
                    + res, res.contains("foo2=bar2"));
        } catch (IllegalCommandArgumentsException ex) {
            LOG.error("Execution failed", ex);
            fail(ex.getMessage());
        }
    }
    
    /**
     * Test signing a document using webservices supplying additional metadata.
     * 
     * @throws Exception
     */
    @Test
    public void test10signDocumentWithMetadataWebservices() throws Exception {
        try {
            String res =
                    new String(execute("signdocument", "-workername", "EchoRequestMetadataSigner", "-data", "<root/>",
                            "-protocol", "WEBSERVICES", "-metadata", "foo=bar", "-metadata", "foo2=bar2",
                            "-truststore", signserverhome + "/p12/truststore.jks", "-truststorepwd", "changeit",
                            "-host", getHTTPHost(), "-port", String.valueOf(getPublicHTTPSPort())));
            assertTrue("contains metadata parameter: "
                    + res, res.contains("foo=bar"));
            assertTrue("contains metadata parameter: "
                    + res, res.contains("foo2=bar2"));
        } catch (IllegalCommandArgumentsException ex) {
            LOG.error("Execution failed", ex);
            fail(ex.getMessage());
        }
    }
    
    /**
     * Test signing a document using client-authenticated webservices supplying additional metadata.
     * 
     * @throws Exception
     */
    @Test
    public void test11signDocumentWithMetadataClientWS() throws Exception {
        try {
            String res =
                    new String(execute("signdocument", "-workername", "EchoRequestMetadataSigner", "-data", "<root/>",
                            "-protocol", "CLIENTWS", "-metadata", "foo=bar", "-metadata", "foo2=bar2",
                            "-truststore", signserverhome + "/p12/truststore.jks", "-truststorepwd", "changeit",
                            "-host", getHTTPHost(), "-port", String.valueOf(getPublicHTTPSPort())));
            assertTrue("contains metadata parameter: "
                    + res, res.contains("foo=bar"));
            assertTrue("contains metadata parameter: "
                    + res, res.contains("foo2=bar2"));
        } catch (IllegalCommandArgumentsException ex) {
            LOG.error("Execution failed", ex);
            fail(ex.getMessage());
        }
    }
    
    /**
     * Test that passing a malformed metadata argument is rejected.
     * 
     * @throws Exception
     */
    @Test
    public void test12signDocumentInvalidMetadata() throws Exception {
        try {
            execute("signdocument", "-workername", "EchoRequestMetadataSigner", "-data", "<root/>",
                    "-protocol", "HTTP", "-metadata", "bogus");
            fail("Should throw an IllegalCommandArgumentsException");
        } catch (IllegalCommandArgumentsException ex) {
            // expected
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getClass().getName() + ": " + e.getMessage());
        }
    }

    /**
     * Tests that when not specifying any truststore password on the command
     * line the code for prompting for the password is called once.
     * @throws Exception 
     */
    @Test
    public void test13promptForTruststorePassword() throws Exception {
        // Override the password reading
        final ArrayList<Boolean> called = new ArrayList<Boolean>();
        SignDocumentCommand instance = new SignDocumentCommand() {
            @Override
            protected ConsolePasswordReader createConsolePasswordReader() {
                return new ConsolePasswordReader() {
                    @Override
                    public char[] readPassword() {
                        called.add(true);
                        return "changeit".toCharArray();
                    }
                };
            }
        };
        
        // Sign anything and check that the readPassword was called once
        try {
            String res =
                    new String(execute(instance, "signdocument", "-workername", "TestXMLSigner", "-data", "<root/>",
                            "-truststore", signserverhome + "/p12/truststore.jks"));
            assertEquals("calls to readPassword", 1, called.size());
            assertTrue("contains signature tag: "
                    + res, res.contains("<root><Signature"));
        } catch (IllegalCommandArgumentsException ex) {
            LOG.error("Execution failed", ex);
            fail(ex.getMessage());
        }
    }
    
    /**
     * Tests that when providing a username but not a password the code for
     * prompting for password is called once.
     * @throws Exception 
     */
    @Test
    public void test13promptForUserPassword() throws Exception {
        // Override the password reading
        final ArrayList<Boolean> called = new ArrayList<Boolean>();
        SignDocumentCommand instance = new SignDocumentCommand() {
            @Override
            protected ConsolePasswordReader createConsolePasswordReader() {
                return new ConsolePasswordReader() {
                    @Override
                    public char[] readPassword() {
                        called.add(true);
                        return "foo123".toCharArray();
                    }
                };
            }
        };
        
        // Sign anything and check that the readPassword was called once
        try {
            String res =
                    new String(execute(instance, "signdocument", "-workername", "TestXMLSigner", "-data", "<root/>",
                            "-username", "user1"));
            assertEquals("calls to readPassword", 1, called.size());
            assertTrue("contains signature tag: "
                    + res, res.contains("<root><Signature"));
        } catch (IllegalCommandArgumentsException ex) {
            LOG.error("Execution failed", ex);
            fail(ex.getMessage());
        }
    }
    
    /**
     * Tests that when not specifying any keystore password on the command
     * line the code for prompting for the password is called once.
     * @throws Exception 
     */
    @Test
    public void test13promptForKeystorePassword() throws Exception {
        // Override the password reading
        final ArrayList<Boolean> called = new ArrayList<Boolean>();
        SignDocumentCommand instance = new SignDocumentCommand() {
            @Override
            protected ConsolePasswordReader createConsolePasswordReader() {
                return new ConsolePasswordReader() {
                    @Override
                    public char[] readPassword() {
                        called.add(true);
                        return "foo123".toCharArray();
                    }
                };
            }
        };
        
        // The test might not have been setup to work with client cert auth
        // so we will not be checking that signing works, just that the prompt
        // gets called
        try {
            execute(instance, "signdocument", "-workername", "TestXMLSigner", "-data", "<root/>",
                            "-keystore", "/tmp/any-keystore-file-we-dont-care");
        } catch (RuntimeException expected) { // XXX: The method throwing this RunTimeException should be refactored
            // OK as the keystore does not exist
        }
        assertEquals("calls to readPassword", 1, called.size());
    }
    
    /**
     * Tests that when not specifying any of user and truststore password they
     * are both prompted for.
     * @throws Exception 
     */
    @Test
    public void test13promptForUserAndTruststore() throws Exception {
        // Override the password reading
        final ArrayList<Boolean> called = new ArrayList<Boolean>();
        SignDocumentCommand instance = new SignDocumentCommand() {
            @Override
            protected ConsolePasswordReader createConsolePasswordReader() {
                return new ConsolePasswordReader() {
                    @Override
                    public char[] readPassword() {
                        called.add(true);
                        return "changeit".toCharArray();
                    }
                };
            }
        };
        
        // Sign anything and check that the readPassword was called twice
        try {
            String res =
                    new String(execute(instance, "signdocument", "-workername", "TestXMLSigner", "-data", "<root/>",
                            "-username", "user1",
                            "-truststore", signserverhome + "/p12/truststore.jks"));
            assertTrue("contains signature tag: "
                    + res, res.contains("<root><Signature"));
        } catch (IllegalCommandArgumentsException ex) {
            LOG.error("Execution failed", ex);
            fail(ex.getMessage());
        }
        assertEquals("calls to readPassword", 2, called.size());
    }
    
    @Test
    public void test99TearDownDatabase() throws Exception {
        removeWorker(WORKERID2);
        for (int workerId : WORKERS) {
            removeWorker(workerId);
        }
    }

    private byte[] execute(String... args) throws IOException, IllegalCommandArgumentsException, CommandFailureException {
        return execute(new SignDocumentCommand(), args);
    }
    
    private byte[] execute(SignDocumentCommand instance, String... args) throws IOException, IllegalCommandArgumentsException, CommandFailureException {
        byte[] output;
        final ByteArrayOutputStream bout = new ByteArrayOutputStream();
        final PrintStream out = new PrintStream(bout);
        System.setOut(out);
        instance.init(new CommandContext("group1", "signdocument", new CommandFactoryContext(new Properties(), out, System.err)));
        try {
            instance.execute(args);
        } finally {
            output = bout.toByteArray();
            System.setOut(System.out);
            System.out.write(output);
        }
        return output;
    }
}
