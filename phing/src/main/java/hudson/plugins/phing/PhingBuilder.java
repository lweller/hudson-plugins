package hudson.plugins.phing;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.plugins.phing.console.PhingConsoleAnnotator;
import hudson.tasks.Builder;
import hudson.util.ArgumentListBuilder;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;

import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Phing Builder Plugin.
 *
 * @author Seiji Sogabe
 */
public final class PhingBuilder extends Builder {

    @Extension
    public static final PhingDescriptor DESCRIPTOR = new PhingDescriptor();

    /**
     * Set to true for debugging.
     */
    private static final boolean DEBUG = false;

    /**
     * Optional build script.
     */
    private final String buildFile;

    /**
     * Identifies {@link PhingInstallation} to be used.
     */
    private final String name;

    /**
     * List of Phing targets to be invoked.
     * If not specified, use "build.xml".
     */
    private final String targets;

    /**
     * Optional properties to be passed to Phing.
     * Follow {@link Properties} syntax.
     */
    private final String properties;

    public String getBuildFile() {
        return buildFile;
    }

    public String getName() {
        return name;
    }

    public String getTargets() {
        return targets;
    }

    public String getProperties() {
        return properties;
    }

    @DataBoundConstructor
    public PhingBuilder(final String name, final String buildFile, final String targets, final String properties) {
        super();
        this.name = Util.fixEmptyAndTrim(name);
        this.buildFile = Util.fixEmptyAndTrim(buildFile);
        this.targets = Util.fixEmptyAndTrim(targets);
        this.properties = Util.fixEmptyAndTrim(properties);
    }

    public PhingInstallation getPhing() {
        for (final PhingInstallation inst : DESCRIPTOR.getInstallations()) {
            if (name != null && name.equals(inst.getName())) {
                return inst;
            }
        }
        return null;
    }

    @Override
    public Descriptor<Builder> getDescriptor() {
        return DESCRIPTOR;
    }

    @Override
    public boolean perform(final AbstractBuild<?, ?> build, final Launcher launcher, final BuildListener listener)
            throws InterruptedException, IOException {

        ArgumentListBuilder args = new ArgumentListBuilder();
        final EnvVars env = build.getEnvironment(listener);

        final PhingInstallation pi = getPhing();

        // PHP Command
        if (pi != null) {
            final String phpCommand = pi.getPhpCommand();
            if (phpCommand != null) {
                env.put("PHP_COMMAND", phpCommand);
            }
        }

        // Phing Command
        if (pi == null) {
            args.add(PhingInstallation.getExecName(launcher));
        } else {
            args.add(pi.getExecutable(launcher));
        }

        // Build script
        FilePath buildFilePath;
        if (buildFile == null) {
            buildFilePath = build.getModuleRoot().child("build.xml");
        } else {
            final boolean absolute = new File(buildFile).isAbsolute();
            if (absolute) {
                buildFilePath = new FilePath(new File(buildFile));
            } else {
                buildFilePath = build.getModuleRoot().child(buildFile);
            }
            args.add("-buildfile", buildFilePath.getName());
        }

        // Targets
        if (targets != null) {
            final String normalizedTargets = targets.replaceAll("[\t\r\n]+", " ");
            args.addTokenized(normalizedTargets);
        }

        // Properties
        if (properties != null) {
            final Properties props = loadProperties();
            for (final Entry<Object, Object> entry : props.entrySet()) {
                args.add("-D" + entry.getKey() + "=" + entry.getValue());
            }
        }

        args.addKeyValuePairs("-D", build.getBuildVariables());

        // avoid printing esc sequence
        args.add("-logger", "phing.listener.NoBannerLogger");

        // Environment variables
        if (pi != null && pi.getPhingHome() != null) {
            env.put("PHING_HOME", pi.getPhingHome());
            env.put("PHING_CLASSPATH", pi.getPhingHome() + File.separator + "classes");
        }

        if (!launcher.isUnix()) {
            // TODO
            // args = args.toWindowsCommand();
            args.add("&&", "exit", "%%ERRORLEVEL%%");
            args = new ArgumentListBuilder().add("cmd.exe", "/C").addQuoted(args.toStringWithQuote());
        }

        if (DEBUG) {
            final PrintStream logger = listener.getLogger();
            for (final Map.Entry<String, String> entry : env.entrySet()) {
                logger.println("(DEBUG) env: key= " + entry.getKey() + " value= " + entry.getValue());
            }
        }

        final long startTime = System.currentTimeMillis();
        try {
            PhingConsoleAnnotator pca = new PhingConsoleAnnotator(listener.getLogger(), build.getCharset());
            int result;
            try {
               result = launcher.launch().cmds(args).envs(env).stdout(pca).pwd(buildFilePath.getParent()).join();
            } finally {
                pca.forceEol();
            }
            return result == 0;
        } catch (final IOException e) {
            Util.displayIOException(e, listener);
            final long processingTime = System.currentTimeMillis() - startTime;
            final String errorMessage = buildErrorMessage(pi, processingTime);
            e.printStackTrace(listener.fatalError(errorMessage));
            return false;
        }
    }

    private String buildErrorMessage(final PhingInstallation pi, final long processingTime) {
        final StringBuffer msg = new StringBuffer();
        msg.append(Messages.Phing_ExecFailed());
        if (pi == null && processingTime < 1000) {
            if (DESCRIPTOR.getInstallations() == null) {
                msg.append(Messages.Phing_GlocalConfigNeeded());
            } else {
                msg.append(Messages.Phing_ProjectConfigNeeded());
            }
        }
        return msg.toString();
    }

    private Properties loadProperties() throws IOException {
        final Properties props = new Properties();
        try {
            // JavaSE 6.0
            props.load(new StringReader(properties));
        } catch (final NoSuchMethodError e) {
            // J2SE 5.0
            props.load(new ByteArrayInputStream(properties.getBytes()));
        }
        return props;
    }
}
