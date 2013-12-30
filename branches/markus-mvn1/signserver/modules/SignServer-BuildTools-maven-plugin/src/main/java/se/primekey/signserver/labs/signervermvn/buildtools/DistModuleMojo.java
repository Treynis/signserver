/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package se.primekey.signserver.labs.signervermvn.buildtools;

import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

/**
 *
 * @author markus
 */ 
@Mojo( name = "dist-module", requiresDependencyResolution = ResolutionScope.TEST,
       defaultPhase = LifecyclePhase.INSTALL, threadSafe = true )
public class DistModuleMojo extends AbstractMojo {

    
    private String distJar;
    private String destDistJar;
    
    @Component
    protected MavenProject project;
    

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        distJar = project.getProperties().getProperty("dist.jar");
        destDistJar = project.getProperties().getProperty("dest.dist.jar", distJar);
        
        if (distJar != null) {
            try {
                File destFile = new File(project.getBasedir(), "../../lib/" + destDistJar);
                FileUtils.copyFile(new File(project.getBasedir() + "/target/" + distJar), destFile);
                getLog().info("Copied to " + destFile.getCanonicalPath());
            } catch (IOException ex) {
                throw new MojoExecutionException("Could not copy " + distJar + " to lib folder", ex);
            }
        } else {
            getLog().info("No dist.jar defined so not copying anything to lib");
        }
    }
    
}
