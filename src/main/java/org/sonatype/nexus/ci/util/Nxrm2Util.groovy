package org.sonatype.nexus.ci.util

import com.sonatype.nexus.api.repository.v2.RepositoryInfo

import static org.sonatype.nexus.ci.config.GlobalNexusConfiguration.getGlobalNexusConfiguration
import static org.sonatype.nexus.ci.config.NxrmVersion.NEXUS_2
import static org.sonatype.nexus.ci.util.RepositoryManagerClientUtil.nexus2Client

class Nxrm2Util
{
  /**
   * Return Nexus repositories which are applicable for package upload. These are maven2 hosted repositories.
   */
  static List<RepositoryInfo> getApplicableRepositories(final String nexusInstanceId) {
    def configuration = globalNexusConfiguration.nxrmConfigs.find { it.id == nexusInstanceId }

    if (configuration.version != NEXUS_2) {
      throw new IllegalArgumentException('Specified nexus instance is not a 2.x server')
    }

    return getApplicableRepositories(configuration.serverUrl, configuration.credentialsId)
  }

  /**
   * Return Nexus repositories which are applicable for package upload. These are maven2 hosted repositories.
   */
  static List<RepositoryInfo> getApplicableRepositories(final String serverUrl, final String credentialsId) {
    def client = nexus2Client(serverUrl, credentialsId)
    return client.getRepositoryList().findAll {
      'maven2'.equalsIgnoreCase(it.format) &&
          'hosted'.equalsIgnoreCase(it.repositoryType) &&
          'release'.equalsIgnoreCase(it.repositoryPolicy)
    }
  }
}
