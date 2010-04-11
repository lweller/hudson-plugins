package hudson.plugins.virtualbox;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.tasks.BuildWrapper;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;

/**
 * @author Evgeny Mandrikov
 */
public class VirtualBoxBuildWrapper extends BuildWrapper {
  private String hostName;
  private String virtualMachineName;

  @DataBoundConstructor
  public VirtualBoxBuildWrapper() {
    super();
  }

  @Override
  public Environment setUp(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
    // TODO implement me
    throw new UnsupportedOperationException("Not implemented");
  }

  public String getHostName() {
    return hostName;
  }

  public String getVirtualMachineName() {
    return virtualMachineName;
  }

  @Extension
  public static final class DescriptorImpl extends Descriptor<BuildWrapper> {
    @Override
    public String getDisplayName() {
      return Messages.VirtualBoxBuildWrapper_displayName();
    }
  }
}
