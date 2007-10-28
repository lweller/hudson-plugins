package org.jvnet.hudson.plugins.port_allocator;

import hudson.ExtensionPoint;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.Describable;
import hudson.model.BuildListener;

import java.io.IOException;

/**
 * Base class for different types of TCP port.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class PortType implements ExtensionPoint, Describable<PortType> {
    /**
     * Name that identifies {@link PortType} among other {@link PortType}s in the
     * same {@link PortAllocator}.
     */
    public final String name;

    protected PortType(String name) {
        // to avoid platform difference issue in case sensitivity of environment variables,
        // always use uppser case.
        this.name = name.toUpperCase();
    }

    /**
     * Allocates a new port for a given build.
     *
     * @param manager
     *      This can be used to assign a new TCP port number.
     * @param prefPort
 *      The port number allocated to this type the last time.
     * @param launcher
     * @param buildListener
     */
    public abstract Port allocate(AbstractBuild<?, ?> build, PortAllocationManager manager, int prefPort, Launcher launcher, BuildListener buildListener) throws IOException, InterruptedException;

    public abstract PortTypeDescriptor getDescriptor();
}
