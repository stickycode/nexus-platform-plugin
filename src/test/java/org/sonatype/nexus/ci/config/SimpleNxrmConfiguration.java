package org.sonatype.nexus.ci.config;

import hudson.Extension;
import org.kohsuke.stapler.DataBoundConstructor;

public class SimpleNxrmConfiguration
    extends NxrmConfiguration
{
  @DataBoundConstructor
  public SimpleNxrmConfiguration(final String id, final String internalId, final String displayName, final String serverUrl,
                                 final String credentialsId)
  {
    super(id, internalId, displayName, serverUrl, credentialsId);
  }

  @Override
  public NxrmVersion getVersion() {
    return null;
  }

  @Extension
  public static class DescriptorImpl
      extends NxrmDescriptor
  {
    public DescriptorImpl() {
      super(SimpleNxrmConfiguration.class);
    }

    @Override
    public String getDisplayName() {
      return "Nexus Repository Manager Test Server";
    }
  }
}
