package com.cflewis.hudson.jdepend;

import hudson.FilePath;
import hudson.Launcher;
import hudson.Extension;
import hudson.util.FormFieldValidator;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.Build;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.QueryParameter;
import org.xml.sax.SAXException;

import javax.servlet.ServletException;
import javax.xml.parsers.ParserConfigurationException;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import jdepend.xmlui.JDepend;

import org.apache.maven.reporting.MavenReportException;
import org.codehaus.doxia.sink.Sink;
import org.codehaus.mojo.jdepend.*;
import org.codehaus.mojo.jdepend.objects.*;

/**
 * JDependRecorder
 * 
 * Processes JDepend metrics after a build.
 *
 * @author Chris Lewis
 */

public class JDependRecorder extends Recorder 
{
    private PrintStream logger = System.out;

    @DataBoundConstructor
    public JDependRecorder() 
    {
    }

    protected void log(final String message)
    {
    	logger.println("[JDepend] " + message);
    }
    
    /**
     * Get a local location for the workspace data, copying it to a temp
     * directory if needed.
     * 
     * @author Chris Lewis
     */
    private FilePath copyToLocalWorkspace(FilePath currentWorkspace) throws
    	InterruptedException, IOException
    {
    	FilePath newSourceLocation = (new FilePath(new File(System.getProperty("java.io.tmpdir"))).createTempDir("hudson-jdepend", ""));
    	log("Copying remote data to " + newSourceLocation.toURI());
    	currentWorkspace.copyRecursiveTo(newSourceLocation);
    	log("Copy complete");

    	return newSourceLocation;
    }
    
    /**
     * Gets a location to output a JDepend file, the source to run JDepend
     * on, then returns a parsed version of the JDepend file
     * @param jDependOutputFile the temporary file that JDepend should
     * 							output to
     * @param sourcePath the path to the source that JDepend should analyze
     * @return a parsed version of the JDepend output
     * @throws IOException
     * @throws ParserConfigurationException
     * @throws SAXException
     */
    protected JDependParser getJDependParser(File jDependOutputFile, String sourcePath)
    	throws IOException, ParserConfigurationException, SAXException
    {
    	log("Starting JDepend file, outputting to " + jDependOutputFile.getAbsolutePath());
    	JDepend.main(getArgumentList("-file", jDependOutputFile.getAbsolutePath(), sourcePath)); //build.getProject().getWorkspace().getName()));
    	JDependParser xmlParser = new JDependParser(jDependOutputFile);
    	log("Found " + xmlParser.getTotalClasses() + " classes in " + xmlParser.getPackages().size() + " packages");
    	
    	return xmlParser; 
    }
    
    /**
     * The protected method of generating a JDepend report to allow
     * extensions to override the perform step to add pre/post-stages
     * @param build
     * @param launcher
     * @param listener
     * @return True if report generated successfully.
     */
    protected boolean generateJDependReport(AbstractBuild<?, ?> build, Launcher launcher, 
    		BuildListener listener)
    {
    	logger = listener.getLogger();
    	File jDependFile = null;
    	String sourcePath = ".";
    	FilePath sourceLocation = build.getProject().getWorkspace();
    	boolean copiedWorkspace = false;
    	
    	log("JDepend plugin is ready");

    	/**
    	 * Ready files by ensuring they're on the local machine, fail out
    	 * if we can't get the files locally.
    	 */
        try
        {
        	if (sourceLocation.isRemote())
        	{
        		sourceLocation = copyToLocalWorkspace(sourceLocation);
        		copiedWorkspace = true;
        	}
        	
        	jDependFile = File.createTempFile("jdepend", ".xml");
        }
        catch (Exception e)
        {
        	log("Unable to ready files: " + e);
        	return false;
        }
        
        /**
         * Make sure we actually have paths to where we want to go. Without
         * paths, we have to fail out.
         */
        try
        {
        	sourcePath = sourceLocation.toURI().getPath();
        }
        catch (Exception e)
        {
        	log("Unable to get workspace path: " + e);
        	return false;
        }
        
    	JDependParser p = null;
        	
        try
        {
        	p = getJDependParser(jDependFile, sourcePath);
        }
        catch (Exception e)
        {
        	log("Couldn't generate JDepend file " + e);
        }
        
        build.getActions().add(new JDependBuildAction(build, p));
        
        /**
         * Attempt to delete the temp files. If the source JDepend worked on
         * is not the actual workspace, then it's a temporary directory which 
         * can be safely deleted.
         */
        if (copiedWorkspace) 
        {
        	try
        	{
        		log("Temporary directory deletion disabled, " +
        				"due to lack of testing. " +
        				"Your OS should clean the directory later. " +
        				"If this is a problem, please submit a bug report.");
        		//log("Deleting temporary directory " + sourceLocation);
        		//sourceLocation.deleteRecursive();
        	}
        	catch (Exception e)
        	{
        		log("Unable to remove copied temp source directory at " + 
        				sourcePath + ": " + e);
        	}
        }
        
        if (!jDependFile.delete())
        {
        	log("Unable to remove temp JDepend file in " + 
        			jDependFile.getPath());
        }
        
        return true;
    }	
    
    /**
     * This runs a JDepend analysis job when the build is performed.
     * 
     * Gets the location of the workspace (copying to a temp local directory,
     * if needed), then creates the JDepend report and parses it. It then
     * attempts to delete the temporary files afterwards, but if it's not
     * possible, they should be removed by the host OS when it cleans it's
     * temp directories.
     * 
     * <b>Warning:</b> Usage on multiple machines, and the copying of data
     * thereof, has not been adequately tested yet.
     */
    public boolean perform(AbstractBuild<?,?> build, Launcher launcher, BuildListener listener) 
    {
    	return generateJDependReport(build, launcher, listener);
    }
    
    /**
     * Sets and get the arguments passed for the JDepend.
     *
     * @param argument   Accepts parameter with "-file" string.
     * @param reportFile Accepts the location of the generated JDepend xml report file.
     * @param classDir   Accepts the location of the classes.
     * @return String[]  Returns the array to be pass as parameters for JDepend.
     */
    private String[] getArgumentList(String argument, String reportFile, String classDir)
    {
        ArrayList<String> argList = new ArrayList<String>();
        argList.add(argument);
        argList.add(reportFile);
        argList.add(classDir);

        return (String[]) argList.toArray(new String[argList.size()]);
    }
    
    public Action getProjectAction(AbstractProject<?, ?> project) 
    {
        return new JDependProjectAction(project);
    }

    /**
     * Descriptor for {@link JDependRecorder}. Used as a singleton.
     * The class is marked as public so that it can be accessed from views.
     *
     * <p>
     * See <tt>views/hudson/plugins/hello_world/JDependRecorder/*.jelly</tt>
     * for the actual HTML fragment for the configuration screen.
     */
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> 
    {
        /**
         * To persist global configuration information,
         * simply store it in a field and call save().
         *
         * <p>
         * If you don't want fields to be persisted, use <tt>transient</tt>.
         */
        public DescriptorImpl() 
        {
            super(JDependRecorder.class);
        }
        
        @SuppressWarnings("unchecked")
        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType)
        {
        	return true;
        }
        
        

        /**
         * This human readable name is used in the configuration screen.
         */
        public String getDisplayName() {
            return "Report JDepend";
        }

        public boolean configure(StaplerRequest req, JSONObject o) throws FormException 
        {
        	return true;
        }
    }
}

