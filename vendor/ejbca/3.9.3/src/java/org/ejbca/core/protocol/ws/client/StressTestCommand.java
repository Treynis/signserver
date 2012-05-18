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
package org.ejbca.core.protocol.ws.client;

import java.io.ByteArrayInputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Iterator;
import java.util.List;

import org.bouncycastle.asn1.DERSet;
import org.bouncycastle.jce.PKCS10CertificationRequest;
import org.bouncycastle.util.encoders.Base64;
import org.ejbca.core.model.ra.UserDataConstants;
import org.ejbca.core.protocol.ws.client.gen.Certificate;
import org.ejbca.core.protocol.ws.client.gen.CertificateResponse;
import org.ejbca.core.protocol.ws.client.gen.EjbcaWS;
import org.ejbca.core.protocol.ws.client.gen.UserDataVOWS;
import org.ejbca.core.protocol.ws.common.CertificateHelper;
import org.ejbca.ui.cli.ErrorAdminCommandException;
import org.ejbca.ui.cli.IAdminCommand;
import org.ejbca.ui.cli.IllegalAdminCommandException;
import org.ejbca.util.CertTools;
import org.ejbca.util.PerformanceTest;
import org.ejbca.util.PerformanceTest.Command;
import org.ejbca.util.PerformanceTest.CommandFactory;
import org.ejbca.util.query.BasicMatch;

/**
 * @author Lars Silven, PrimeKey Solutions AB
 * @version $Id: StressTestCommand.java 7637 2009-06-02 10:19:43Z primelars $
 */
public class StressTestCommand extends EJBCAWSRABaseCommand implements IAdminCommand {

    final PerformanceTest performanceTest;
    enum TestType {
        BASIC,
        REVOKE,
        REVOKEALOT
    }
    private class MyCommandFactory implements CommandFactory {
        final String caName;
        final String endEntityProfileName;
        final String certificateProfileName;
        final TestType testType;
        MyCommandFactory( String _caName, String _endEntityProfileName, String _certificateProfileName,
                          TestType _testType ) {
            this.testType = _testType;
            this.caName = _caName;
            this.endEntityProfileName = _endEntityProfileName;
            this.certificateProfileName = _certificateProfileName;
        }
        public Command[] getCommands() throws Exception {
            final KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(1024);
            final EjbcaWS ejbcaWS = getEjbcaRAWSFNewReference();
            final JobData jobData = new JobData();
            switch (this.testType) {
            case BASIC:
                return new Command[]{
                                     new EditUserCommand(ejbcaWS, this.caName, this.endEntityProfileName, this.certificateProfileName, jobData, true),
                                     new Pkcs10RequestCommand(ejbcaWS, kpg.generateKeyPair(), jobData) };
            case REVOKE:
                return new Command[]{
                                     new EditUserCommand(ejbcaWS, this.caName, this.endEntityProfileName, this.certificateProfileName, jobData, true),
                                     new Pkcs10RequestCommand(ejbcaWS, kpg.generateKeyPair(), jobData),
                                     new FindUserCommand(ejbcaWS, jobData),
                                     new ListCertsCommand(ejbcaWS, jobData),
                                     new RevokeCertCommand(ejbcaWS, jobData),
                                     new EditUserCommand(ejbcaWS, this.caName, this.endEntityProfileName, this.certificateProfileName, jobData, false),
                                     new Pkcs10RequestCommand(ejbcaWS, kpg.generateKeyPair(), jobData) };
            case REVOKEALOT:
                return new Command[]{
                					 new MultipleCertsRequestsForAUserCommand(ejbcaWS, this.caName, this.endEntityProfileName, this.certificateProfileName, jobData, kpg),
                                     new FindUserCommand(ejbcaWS, jobData),
                                     new ListCertsCommand(ejbcaWS, jobData),
                                     new RevokeCertCommand(ejbcaWS, jobData)//,
                					 };
            default:
                return null;
            }
        }
    }
    class JobData {
        String userName;
        String passWord;
        X509Certificate userCertsToBeRevoked[];
        String getDN() {
            return "CN="+this.userName;
        }
        @Override
        public String toString() {
            return "Username '"+this.userName+"' with password '"+this.passWord+"'."; 
        }
    }
    private class BaseCommand {
        final protected JobData jobData;
        BaseCommand(JobData _jobData) {
            this.jobData = _jobData;
        }
        @Override
        public String toString() {
            return "Class \'" +this.getClass().getCanonicalName()+"' with this job data: "+ this.jobData.toString();
        }
    }
    private class Pkcs10RequestCommand extends BaseCommand implements Command {
        final private EjbcaWS ejbcaWS;
        final private PKCS10CertificationRequest pkcs10;
        Pkcs10RequestCommand(EjbcaWS _ejbcaWS, KeyPair keys, JobData _jobData) throws Exception {
            super(_jobData);
            this.pkcs10 = new PKCS10CertificationRequest("SHA1WithRSA", CertTools.stringToBcX509Name("CN=NOUSED"), keys.getPublic(), new DERSet(), keys.getPrivate());
            this.ejbcaWS = _ejbcaWS;
        }
        public boolean doIt() throws Exception {
            final CertificateResponse certificateResponse = this.ejbcaWS.pkcs10Request(this.jobData.userName, this.jobData.passWord,
                                                                                       new String(Base64.encode(this.pkcs10.getEncoded())),null,CertificateHelper.RESPONSETYPE_CERTIFICATE);
            final Iterator<X509Certificate> i = (Iterator<X509Certificate>)CertificateFactory.getInstance("X.509").generateCertificates(new ByteArrayInputStream(Base64.decode(certificateResponse.getData()))).iterator();
            X509Certificate cert = null;
            while ( i.hasNext() )
                cert = i.next();
            if ( cert==null ) {
                StressTestCommand.this.performanceTest.getLog().error("no certificate generated for user "+this.jobData.userName);
                return false;
            }
            final String commonName = CertTools.getPartFromDN(cert.getSubjectDN().getName(), "CN");
            if ( commonName.equals(this.jobData.userName) ) {
                StressTestCommand.this.performanceTest.getLog().info("Cert created. Subject DN: \""+cert.getSubjectDN()+"\".");
                StressTestCommand.this.performanceTest.getLog().result(CertTools.getSerialNumber(cert));
                return true;
            }
            StressTestCommand.this.performanceTest.getLog().error("Cert not created for right user. Username: \""+this.jobData.userName+"\" Subject DN: \""+cert.getSubjectDN()+"\".");
            return false;
        }
        public String getJobTimeDescription() {
            return "Relative time spent signing certificates";
        }
    }
    private class MultipleCertsRequestsForAUserCommand extends BaseCommand implements Command {
        final EjbcaWS ejbcaWS;
        final String caName;
        final String endEntityProfileName;
        final String certificateProfileName;
        final KeyPairGenerator kpg;
        MultipleCertsRequestsForAUserCommand(EjbcaWS _ejbcaWS, String _caName, String _endEntityProfileName, String _certificateProfileName, JobData _jobData, KeyPairGenerator _kpg) throws Exception {
            super(_jobData);
            this.caName = _caName;
            this.endEntityProfileName = _endEntityProfileName;
            this.certificateProfileName = _certificateProfileName;
            this.kpg = _kpg;
            this.ejbcaWS = _ejbcaWS;
        }
        public boolean doIt() throws Exception {
            boolean createUser = true;
            for (int i=0; i<50; i++) {
                EditUserCommand editUserCommand = new EditUserCommand(this.ejbcaWS, this.caName, this.endEntityProfileName, this.certificateProfileName, this.jobData, createUser);
                if (!editUserCommand.doIt()) {
                    StressTestCommand.this.performanceTest.getLog().error("MultiplePkcs10RequestsCommand failed for "+this.jobData.userName);
                    return false;
                }
                createUser = false;
                Pkcs10RequestCommand pkcs10RequestCommand = new Pkcs10RequestCommand(this.ejbcaWS, this.kpg.generateKeyPair(), this.jobData);
                if (!pkcs10RequestCommand.doIt()) {
                    StressTestCommand.this.performanceTest.getLog().error("MultiplePkcs10RequestsCommand failed for "+this.jobData.userName);
                    return false;
                }
            }
            return true;
        }
        public String getJobTimeDescription() {
            return "Relative time spent creating a lot of certificates";
        }
    }
    private class FindUserCommand extends BaseCommand implements Command {
        final private EjbcaWS ejbcaWS;
        FindUserCommand(EjbcaWS _ejbcaWS, JobData _jobData) throws Exception {
            super(_jobData);
            this.ejbcaWS = _ejbcaWS;
        }
        public boolean doIt() throws Exception {
            final org.ejbca.core.protocol.ws.client.gen.UserMatch match = new org.ejbca.core.protocol.ws.client.gen.UserMatch();
            match.setMatchtype(BasicMatch.MATCH_TYPE_EQUALS);
            match.setMatchvalue(this.jobData.getDN());
            match.setMatchwith(org.ejbca.util.query.UserMatch.MATCH_WITH_DN);
            final List<UserDataVOWS> result = this.ejbcaWS.findUser(match);
            if (result.size()<1) {
                StressTestCommand.this.performanceTest.getLog().error("No users found for DN \""+this.jobData.getDN()+"\"");
                return false;
            }
            final Iterator<UserDataVOWS> i = result.iterator();
            while ( i.hasNext() ) {
                final String userName = i.next().getUsername();
                if( !userName.equals(this.jobData.userName) ) {
                    StressTestCommand.this.performanceTest.getLog().error("wrong user name \""+userName+"\" for certificate with DN \""+this.jobData.getDN()+"\"");
                    return false;
                }
            }
            return true;
        }
        public String getJobTimeDescription() {
            return "Relative time spent looking for user";
        }
    }
    private class ListCertsCommand extends BaseCommand implements Command {
        final private EjbcaWS ejbcaWS;
        ListCertsCommand(EjbcaWS _ejbcaWS, JobData _jobData) throws Exception {
            super(_jobData);
            this.ejbcaWS = _ejbcaWS;
        }
        public boolean doIt() throws Exception {
            final List<Certificate> result = this.ejbcaWS.findCerts(this.jobData.userName, true);
            final Iterator<Certificate> i = result.iterator();
            this.jobData.userCertsToBeRevoked = new X509Certificate[result.size()];
            for( int j=0; i.hasNext(); j++ )
                this.jobData.userCertsToBeRevoked[j] = (X509Certificate)CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(Base64.decode(i.next().getCertificateData())));
            if ( this.jobData.userCertsToBeRevoked.length < 1 ) {
                StressTestCommand.this.performanceTest.getLog().error("no cert found for user "+this.jobData.userName);
                return false;
            }

            return true;
        }
        public String getJobTimeDescription() {
            return "Relative time spent finding certs for user.";
        }
    }
    private class RevokeCertCommand extends BaseCommand implements Command {
        final private EjbcaWS ejbcaWS;
        RevokeCertCommand(EjbcaWS _ejbcaWS, JobData _jobData) throws Exception {
            super(_jobData);
            this.ejbcaWS = _ejbcaWS;
        }
        public boolean doIt() throws Exception {
            for (int i=0; i<this.jobData.userCertsToBeRevoked.length; i++)
                this.ejbcaWS.revokeCert(this.jobData.userCertsToBeRevoked[i].getIssuerDN().getName(),
                                        this.jobData.userCertsToBeRevoked[i].getSerialNumber().toString(16),
                                        REVOKATION_REASON_UNSPECIFIED);
            return true;
        }
        public String getJobTimeDescription() {
            return "Relative time spent revoking certificates.";
        }
    }
    private class EditUserCommand extends BaseCommand implements Command {
        final private EjbcaWS ejbcaWS;
        final private UserDataVOWS user;
        final private boolean doCreateNewUser;
        EditUserCommand(EjbcaWS _ejbcaWS, String caName, String endEntityProfileName, String certificateProfileName, JobData _jobData, boolean _doCreateNewUser) {
            super(_jobData);
            this.doCreateNewUser = _doCreateNewUser;
            this.ejbcaWS = _ejbcaWS;
            this.user = new UserDataVOWS();
            this.user.setClearPwd(true);
            this.user.setCaName(caName);
            this.user.setEmail(null);
            this.user.setSubjectAltName(null);
            this.user.setStatus(UserDataConstants.STATUS_NEW);
            this.user.setTokenType(org.ejbca.core.protocol.ws.objects.UserDataVOWS.TOKEN_TYPE_USERGENERATED);
            this.user.setEndEntityProfileName(endEntityProfileName);
            this.user.setCertificateProfileName(certificateProfileName);
        }
        public boolean doIt() throws Exception {
            if ( this.doCreateNewUser ) {
                this.jobData.passWord = "foo123";
                this.jobData.userName = "WSTESTUSER"+StressTestCommand.this.performanceTest.nextLong();
            }
            this.user.setSubjectDN(this.jobData.getDN());
            this.user.setUsername(this.jobData.userName);
            this.user.setPassword(this.jobData.passWord);
            this.ejbcaWS.editUser(this.user);
            return true;
        }
        public String getJobTimeDescription() {
            if ( this.doCreateNewUser )
                return "Relative time spent registring new users";

            return "Relative time spent setting status of user to NEW";
        }
    }
    /**
     * @param args
     */
    public StressTestCommand(String[] _args) {
        super(_args);
        this.performanceTest = new PerformanceTest();
    }

    /* (non-Javadoc)
     * @see org.ejbca.core.protocol.ws.client.EJBCAWSRABaseCommand#usage()
     */
    @Override
    protected void usage() {
        getPrintStream().println("Command used to perform a \"stress\" test of EJBCA.");
        getPrintStream().println("The command will start up a number of threads.");
        getPrintStream().println("Each thread will continuously add new users to EJBCA. After adding a new user the thread will fetch a certificate for it.");
        getPrintStream().println();
        getPrintStream().println("Usage : stress <caname> <nr of threads> <max wait time in ms to fetch cert after adding user> [<end entity profile name>] [<certificate profile name>] [<type of test>]");
        getPrintStream().println();
        getPrintStream().println("Here is an example of how the test could be started:");
        getPrintStream().println("./ejbcawsracli.sh stress AdminCA1 20 5000");
        getPrintStream().println("20 threads is started. After adding a user the thread waits between 0-500 ms before requesting a certificate for it. The certificates will all be signed by the CA AdminCA1.");
        getPrintStream().print("Types of stress tests:");
        TestType testTypes[] = TestType.values(); 
        for ( TestType testType : testTypes )
            getPrintStream().print(" " + testType);
        getPrintStream().println();
    }

    /* (non-Javadoc)
     * @see org.ejbca.ui.cli.IAdminCommand#execute()
     */
    public void execute() throws IllegalAdminCommandException, ErrorAdminCommandException {

        try {
            if(this.args.length <  2){
                usage();
                System.exit(-1);
            }
            final int numberOfThreads = this.args.length>2 ? Integer.parseInt(this.args[2]) : 1;
            final int waitTime = this.args.length>3 ? Integer.parseInt(this.args[3]) : -1;
            final String caName = this.args[1];
            final String endEntityProfileName = this.args.length>4 ? this.args[4] : "EMPTY";
            final String certificateProfileName = this.args.length>5 ? this.args[5] : "ENDUSER";
            final TestType testType = this.args.length>6 ? TestType.valueOf(this.args[6]) : TestType.BASIC;
            this.performanceTest.execute(new MyCommandFactory(caName, endEntityProfileName, certificateProfileName, testType),
                                         numberOfThreads, waitTime, getPrintStream());
            getPrintStream().println("A test key for each thread is generated. This could take some time if you have specified many threads and long keys.");
            synchronized(this) {
                wait();
            }
        } catch( InterruptedException e) {
            // do nothing since user wants to exit.
        } catch( Exception e) {
            throw new ErrorAdminCommandException(e);
        }finally{
            this.performanceTest.getLog().close();
        }
    }
}
