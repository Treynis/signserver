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

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import javax.xml.ws.soap.SOAPFaultException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.log4j.Logger;
import org.signserver.common.AuthorizationRequiredException;
import org.signserver.common.CryptoTokenOfflineException;
import org.signserver.common.IllegalRequestException;
import org.signserver.common.SignServerException;

/**
 * Command Line Interface (CLI) for signing MRTD SODs.
 *
 * @author Markus Kilås
 * @version $Id$
 */
public class SODSignerCLI {

    /** Logger for this class. */
    private static final Logger LOG = Logger.getLogger(SODSignerCLI.class);

    /** ResourceBundle with internationalized StringS. */
    private static final ResourceBundle TEXTS = ResourceBundle.getBundle(
            "org/signserver/client/cli/ResourceBundle");

    /** System-specific new line characters. **/
    private static final String NL = System.getProperty("line.separator");

    /** The name of this command. */
    private static final String COMMAND = "signdatagroups";

    /** Option WORKERID. */
    public static final String WORKERID = "workerid";

    /** Option WORKERNAME. */
    public static final String WORKERNAME = "workername";

    /** Option DATA. */
    public static final String DATA = "data";

    /** Option ENCODING. */
    public static final String ENCODING = "encoding";

    /** Option HOST. */
    public static final String HOST = "host";

    /** Option PORT. */
    public static final String PORT = "port";

    public static final String SERVLET = "servlet";

    /** Option PROTOCOL. */
    public static final String PROTOCOL = "protocol";

    /** Option USERNAME. */
    public static final String USERNAME = "username";

    /** Option PASSWORD. */
    public static final String PASSWORD = "password";

    /** Option REPEAT. */
    public static final String REPEAT = "repeat";

    /** The command line options. */
    private static final Options OPTIONS;

    /**
     * Protocols that can be used for accessing SignServer.
     */
    public static enum Protocol {
        /** The HTTP interface. */
        HTTP
    }

    static {
        OPTIONS = new Options();
        OPTIONS.addOption(WORKERID, true,
                TEXTS.getString("WORKERID_DESCRIPTION"));
        OPTIONS.addOption(WORKERNAME, true,
                TEXTS.getString("WORKERNAME_DESCRIPTION"));
        OPTIONS.addOption(DATA, true,
                TEXTS.getString("DATA_DESCRIPTION"));
        OPTIONS.addOption(ENCODING, true,
                TEXTS.getString("ENCODING_DESCRIPTION"));
        OPTIONS.addOption(HOST, true,
                TEXTS.getString("HOST_DESCRIPTION"));
        OPTIONS.addOption(PORT, true,
                TEXTS.getString("PORT_DESCRIPTION"));
        OPTIONS.addOption(SERVLET, true,
                TEXTS.getString("SERVLET_SOD_DESCRIPTION"));
        OPTIONS.addOption(USERNAME, true, TEXTS.getString("USERNAME_DESCRIPTION"));
        OPTIONS.addOption(PASSWORD, true, TEXTS.getString("PASSWORD_DESCRIPTION"));
        OPTIONS.addOption(REPEAT, true, TEXTS.getString("REPEAT_DESCRIPTION"));
        for (Option option : KeyStoreOptions.getKeyStoreOptions()) {
            OPTIONS.addOption(option);
        }
    }

    /** ID of worker who should perform the operation. */
    private int workerId;

    /** Name of worker who should perform the operation. */
    private String workerName;

    /** Data to sign. */
    private String data;

    /** Encoding of the data, if the data should be decoded before signing. */
    private String encoding;

    /** Hostname or IP address of the SignServer host. */
    private String host;

    /** TCP port number of the SignServer host. */
    private Integer port;

    private String servlet = "/signserver/sod";

    /** File to read the data from. */
    private File inFile;  //NOPMD //TODO Add support for reading from file

    /** Protocol to use for contacting SignServer. */
    private Protocol protocol; //NOPMD //TODO Add support for choosing protocol

    private String username;

    private String password;

    private Map<Integer,byte[]> dataGroups;

    private int repeat = 1;

    private KeyStoreOptions keyStoreOptions = new KeyStoreOptions();

    /**
     * Creates an instance of DocumentSignerCLI.
     *
     * @param args Command line arguments
     */
    public SODSignerCLI(final String[] args) {
        try {
            // Parse the command line
            parseCommandLine(new GnuParser().parse(OPTIONS, args));
        } catch (ParseException ex) {
            throw new IllegalArgumentException(ex.getLocalizedMessage(), ex);
        }
        validateOptions();
    }

    /**
     * Reads all the options from the command line.
     *
     * @param line The command line to read from
     */
    private void parseCommandLine(final CommandLine line) {
        if (line.hasOption(WORKERID)) {
                workerId = Integer.parseInt(line.getOptionValue(
                    WORKERID, null));
        }
        if (line.hasOption(WORKERNAME)) {
            workerName = line.getOptionValue(WORKERNAME, null);
        }
        if (line.hasOption(WORKERID)) {
            workerId = Integer.parseInt(line.getOptionValue(WORKERID, null));
        }
        host = line.getOptionValue(HOST, KeyStoreOptions.DEFAULT_HOST);
        if (line.hasOption(PORT)) {
            port = Integer.parseInt(line.getOptionValue(PORT));
        }
        if (line.hasOption(SERVLET)) {
            servlet = line.getOptionValue(SERVLET, null);
        }
        if (line.hasOption(DATA)) {
            data = line.getOptionValue(DATA, "");

            dataGroups = new HashMap<Integer, byte[]>();

            String[] groups = data.split("\\&");
            for(String group : groups) {
                String[] entry = group.split("=");
                dataGroups.put(new Integer(entry[0]), entry[1].getBytes());
            }
        }
        if (line.hasOption(PROTOCOL)) {
            protocol = Protocol.valueOf(line.getOptionValue(
                    PROTOCOL, null));
        }
        if (line.hasOption(USERNAME)) {
            username = line.getOptionValue(USERNAME, null);
        }
        if (line.hasOption(PASSWORD)) {
            password = line.getOptionValue(PASSWORD, null);
        }
        if (line.hasOption(ENCODING)) {
            encoding = line.getOptionValue(ENCODING, null);
        }
        if (line.hasOption(REPEAT)) {
            repeat = Integer.parseInt(line.getOptionValue(REPEAT));
        }
        keyStoreOptions.parseCommandLine(line);
    }

    /**
     * Checks that all mandatory options are given.
     */
    private void validateOptions() {
        if (workerName == null && workerId == 0) {
            throw new IllegalArgumentException(
                    "Missing -workername or -workerid");
        } else if (data == null) {
            throw new IllegalArgumentException("Missing -data");
        }
        keyStoreOptions.validateOptions();
    }

    /**
     * Creates a DocumentSigner using the chosen protocol.
     *
     * @return a DocumentSigner using the chosen protocol
     * @throws MalformedURLException in case an URL can not be constructed
     * using the given host and port
     */
    private SODSigner createSigner() throws MalformedURLException {
        final SODSigner signer;
        
        final String workerIdOrName;
        if (workerId == 0) {
            workerIdOrName = workerName;
        } else {
            workerIdOrName = String.valueOf(workerId);
        }

        keyStoreOptions.setupHTTPS();

        if (port == null) {
            if (keyStoreOptions.isUsePrivateHTTPS()) {
                port = KeyStoreOptions.DEFAULT_PRIVATE_HTTPS_PORT;
            } else if (keyStoreOptions.isUseHTTPS()) {
                port = KeyStoreOptions.DEFAULT_PUBLIC_HTTPS_PORT;
            } else {
                port = KeyStoreOptions.DEFAULT_HTTP_PORT;
            }
        }

        LOG.debug("Using HTTP as procotol");
        signer = new HTTPSODSigner(
            new URL(keyStoreOptions.isUseHTTPS() ? "https" : "http", host, port,
            servlet), workerIdOrName, username, password);

        return signer;
    }

    /**
     * Execute the signing operation.
     */
    public final void run() {
        try {
            final int NUM_WORKERS = 1;
            Worker workers[] = new Worker[NUM_WORKERS];
            for(int i = 0; i < NUM_WORKERS; i++) {
                workers[i] = new Worker("Worker " + i, createSigner(),
                        dataGroups, encoding, repeat);
            }

            // Start workers
            for(Worker worker : workers) {
                worker.start();
            }

            // Wait for worker
            for(Worker worker : workers) {
                System.err.println("Waiting for " +  worker);
                try {
                    worker.join();
                } catch (InterruptedException ex) {
                    System.err.println("Interrupted!");
                }
            }

            System.err.println("Done");

        } catch (SOAPFaultException ex) {
            if (ex.getCause() instanceof AuthorizationRequiredException) {
                final AuthorizationRequiredException authEx =
                        (AuthorizationRequiredException) ex.getCause();
                LOG.error("Authorization required: " + authEx.getMessage());
            }
            LOG.error(ex);
        } catch (IOException ex) {
            LOG.error(ex);
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(final String[] args) {
        try {
            final SODSignerCLI cli = new SODSignerCLI(args);
            cli.run();
        } catch (IllegalArgumentException ex) {
            LOG.error(ex);
            final HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("signdatagroups <options>", OPTIONS);
            System.out.println(new StringBuilder()
                .append(NL)
                .append("Sample usages:").append(NL)
                .append("a) ").append(COMMAND).append(" -workername MRTDSODSigner -data \"1=value1&2=value2&3=value3\"").append(NL)
                .toString());
            System.exit(-1);
        }
    }

    private static class Worker extends Thread {

        private SODSigner signer;
        private Map<Integer,byte[]> dataGroups;
        private String encoding;
        private int repeat;

        public Worker(String name, SODSigner signer, Map<Integer,byte[]> dataGroups,
                String encoding, int repeat) {
            super(name);
            this.signer = signer;
            this.dataGroups = dataGroups;
            this.encoding = encoding;
            this.repeat = repeat;
        }

        @Override
        public void run() {
            try {
                for (int i = 0; i < repeat || repeat == -1; i++) {
                    signer.sign(dataGroups, encoding);
                }
            } catch (IOException ex) {
                LOG.error(ex);
            } catch (IllegalRequestException ex) {
                LOG.error(ex);
            } catch (CryptoTokenOfflineException ex) {
                LOG.error(ex);
            } catch (SignServerException ex) {
                LOG.error(ex);
            }
            LOG.info("Finished");
        }
    }
}
