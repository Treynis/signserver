/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package se.primekey.signserver.labs.signervermvn.buildtools;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;
import java.util.Set;
import org.apache.commons.io.FileUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.dependency.fromDependencies.BuildClasspathMojo;
import org.apache.maven.plugin.dependency.utils.DependencyUtil;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.shared.artifact.filter.collection.ArtifactsFilter;
import org.codehaus.plexus.util.IOUtil;

/**
 *
 * @author markus
 */ 
@Mojo( name = "create-module-descriptor", requiresDependencyResolution = ResolutionScope.TEST,
       defaultPhase = LifecyclePhase.GENERATE_RESOURCES, threadSafe = true )
public class ModuleDescriptorMojo extends BuildClasspathMojo {

    private static final String LOCAL_PREFIX = "lib/";
    private static final String EXTERNAL_PREFIX = "lib/ext/";
    
    private static boolean stripVersion;
    private final boolean stripClassifier = false;
    
    private String distJar;
    private String destDistJar;
    
    
    @Override
    protected ArtifactsFilter getMarkedArtifactFilter() {
        return null;
    }

    @Override
    protected void doExecute() throws MojoExecutionException {
        
        this.includeScope = "runtime";
        this.excludeScope = "compile";
//        stripVersion = Boolean.parseBoolean(project.getProperties().getProperty("stripVersion", "false"));
        
        final String modName = project.getProperties().getProperty("module.name", project.getName());
        
        distJar = project.getProperties().getProperty("dist.jar");
        destDistJar = project.getProperties().getProperty("dest.dist.jar", distJar);
        
        
//        System.out.println("Properties: " + project.getProperties());
        
        String toLib = getDependencies(true, false, LOCAL_PREFIX, EXTERNAL_PREFIX, true, ",");
        String toRoot = getDependencies(false, true, LOCAL_PREFIX, EXTERNAL_PREFIX, true, ",");
        
        // Get depencies for root and for lib with prefix in lib and not including self
        String classpath = getDependencies(true, true, "", "ext/", false, " ");
        
        
        
        final String modulePriority = project.getProperties().getProperty("module.priority");
        final String staticDescriptor = project.getProperties().getProperty("staticDescriptor");
        
        if (modulePriority != null) {
            Properties properties = new Properties();
            properties.setProperty("module.name", modName);
            properties.setProperty("module.type", moduleTypeFromPackaging(project.getPackaging()));
            properties.setProperty("to.lib", toLib);
            properties.setProperty("to.root", toRoot);
            
            final String toConfig = project.getProperties().getProperty("to.config");
            if (toConfig != null) {
                properties.setProperty("to.config", toConfig);
            }

            final String type = project.getArtifact().getType();
            if (type.equals("ejb") || type.equals("war")) {
                properties.setProperty("module." + type, project.getProperties().getProperty("dist.jar"));
            }

            for (String propertyName : project.getProperties().stringPropertyNames()) {
                if (propertyName.startsWith("postprocess.") || propertyName.startsWith("module.web.")) {
                    properties.setProperty(propertyName, project.getProperties().getProperty(propertyName));
                }
            }

            File descriptorDir = new File(project.getBasedir(), "../../mods-available/");
            if (!descriptorDir.exists()) {
                descriptorDir.mkdir();
            }

            File file = new File(descriptorDir, modulePriority + "_" + modName + ".properties");
            OutputStream out = null;
            try {
                out = new FileOutputStream(file);
                properties.store(out, null);
                getLog().info("Wrote module descriptor '" + file.getAbsolutePath());
            } catch (IOException ex) {
                throw new MojoExecutionException("Unable to write descriptor file: " + file, ex);
            } finally {
                IOUtil.close(out);
            }
        } else if (staticDescriptor != null) {
            File descriptorDir = new File(project.getBasedir(), "../../mods-available/");
            if (!descriptorDir.exists()) {
                descriptorDir.mkdir();
            }
            
            File inFile = new File(project.getBasedir(), "src/" + staticDescriptor);
            File file = new File(descriptorDir, staticDescriptor);
            try {
                FileUtils.copyFile(inFile, file);
                getLog().info("Copied module descriptor '" + file.getAbsolutePath());
            } catch (IOException ex) {
                throw new MojoExecutionException("Unable to copy static descriptor file: " + staticDescriptor, ex);
            }
        } else {
            getLog().info("No module.priority defined so not an module");
        }
        
        // classpath.properties
        File targetDir = new File(project.getBasedir(), "target");
        if (!targetDir.exists()) {
            targetDir.mkdir();
        }
        File classpathFile = new File(targetDir, "classpath.properties");

        Properties properties = new Properties();
        properties.setProperty("jar.classpath", createManifestEntryValue("Class-path: ".length(), classpath));

        OutputStream out = null;
        try {
            out = new FileOutputStream(classpathFile);
            properties.store(out, null);
            getLog().info("Wrote classpath file: " + classpathFile.getAbsolutePath());
        } catch (IOException ex) {
            throw new MojoExecutionException("Could not output classpath.properties: " + ex.getMessage(), ex);
        } finally {
            IOUtil.close(out);
        }
        
    }
    
    private String moduleTypeFromPackaging(final String packaging) {
        final String result;
        if ("jar".equals(packaging)) {
            result = "lib";
        } else if ("war".equals(packaging)) {
            result = "war";
        } else if ("ejb".equals(packaging)) {
            result = "ejb";
        } else {
            throw new IllegalArgumentException("Unsupported packaging type: " + packaging);
        }
        return result;
    }
    
    protected void appendArtifactPath( Artifact art, StringBuilder sb, String prefix) {
        sb.append( prefix );
        sb.append( File.separator );
        sb.append( DependencyUtil.getFormattedFileName( art, this.stripVersion, this.prependGroupId, this.useBaseVersion, this.stripClassifier ) );
    }

    // TODO: Exclude scope "test" etc
    private String getDependencies(final boolean includeLib, final boolean includeRoot, final String localPrefix, final String externalPrefix, final boolean self, final String separator) {
        final StringBuilder result = new StringBuilder();
        
        Set<Artifact> artifacts = project.getArtifacts();
//        System.out.println("Artifact: " + project.getArtifact());
        boolean first = true;
        for (Artifact a : artifacts) {
//            System.out.println(a.getArtifactId() + " is of type " + a.getType());
            if (!a.getScope().equals("provided") && !a.getScope().equals("test")) {
                if ((includeRoot && !isTypeForLib(a.getType())) || (includeLib && isTypeForLib(a.getType()))) {
                    if (!first) {
                        result.append(separator);
                    }
                    first = false;
                    if (project.getGroupId().equals(a.getGroupId())) {
                        result.append(localPrefix);
                    } else {
                        result.append(externalPrefix);
                    }
    //                System.out.println("file: " + a.getFile());
                    result.append(a.getFile().getName());
                    
                }
            }
        }
        
        if (self) {
            System.out.println("distJar: " + distJar);
            if (distJar != null) {
                System.out.println("this type: " + project.getArtifact().getType());
                if ((includeRoot && !isTypeForLib(project.getArtifact().getType())) || (includeLib && isTypeForLib(project.getArtifact().getType()))) {
                    result.append(separator);
                    if (project.getGroupId().equals(project.getArtifact().getGroupId())) {
                        result.append(localPrefix);
                    } else {
                        result.append(externalPrefix);
                    }
                    result.append(destDistJar);
                }
            }
        }
        
        return result.toString();
    }
    
    private boolean isTypeForLib(String type) {
        System.out.println("type: " + type);
        return type.equals("jar");
    }

    private String createManifestEntryValue(int nameLength, String value) {
        final StringBuilder result = new StringBuilder();
        int row = nameLength;
        for (int i = 0; i < value.length(); i++) {
            if (row < 100) {
                row++;
            } else {
                result.append("\n ");
                row = 0;
            }
            result.append(value.charAt(i));
        }
        System.out.println("Result:\n" + result.toString());
        return result.toString();
    }
    
}
