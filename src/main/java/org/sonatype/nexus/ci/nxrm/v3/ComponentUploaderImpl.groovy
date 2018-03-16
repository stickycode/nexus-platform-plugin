package org.sonatype.nexus.ci.nxrm.v3

import com.sonatype.nexus.api.exception.RepositoryManagerException
import com.sonatype.nexus.api.repository.v3.DefaultAsset
import com.sonatype.nexus.api.repository.v3.DefaultComponent
import com.sonatype.nexus.api.repository.v3.RepositoryManagerV3Client
import com.sonatype.nexus.api.repository.v3.formats.maven.MavenComponentBuilder

import org.sonatype.nexus.ci.config.Nxrm2Configuration
import org.sonatype.nexus.ci.config.Nxrm3Configuration
import org.sonatype.nexus.ci.nxrm.BaseComponentUploader
import org.sonatype.nexus.ci.nxrm.MavenCoordinate

import hudson.EnvVars
import hudson.FilePath

import static org.sonatype.nexus.ci.util.RepositoryManagerClientUtil.nexus3Client

@SuppressWarnings(['CatchThrowable', 'AbcMetric', 'MethodSize'])
class ComponentUploaderImpl
    extends BaseComponentUploader
{
  private Nxrm3Configuration nx3Config

  ComponentUploaderImpl(final Nxrm3Configuration nexusConfig, final FilePath baseDir,
                        final EnvVars environment, final PrintStream logger)
  {
    super(nexusConfig, baseDir, environment, logger)
    nx3Config = nexusConfig
  }

  @Override
  void doUpload(final String repository, final Map<MavenCoordinate, List<RemoteMavenAsset>> components)
      throws RepositoryManagerException
  {
    RepositoryManagerV3Client nx3Client = null
    try {
      nx3Client = nexus3Client(nx3Config.serverUrl, nx3Config.credentialsId, nx3Config.anonymousAccess)
    }
    catch (Throwable t) {
      logger.println('Failing build due to error creating RepositoryManagerClient')
      throw t
    }

    components.each { uploadComponent(nx3Client, repository, it.key, it.value) }
  }

  private void uploadComponent(RepositoryManagerV3Client nx3Client,
                               String repository,
                               MavenCoordinate coordinate,
                               List<RemoteMavenAsset> remoteMavenAssets)
      throws RepositoryManagerException
  {
    def groupId = coordinate.groupId?.trim() ? environment.expand(coordinate.groupId) : null
    def artifactId = coordinate.artifactId?.trim() ? environment.expand(coordinate.artifactId) : null
    def version = coordinate.version?.trim() ? environment.expand(coordinate.version) : null

    def uploadBuilder = MavenComponentBuilder.create()
    uploadBuilder.withGroupId(groupId).withArtifactId(artifactId).withVersion(version)
    if (coordinate.packaging?.trim()) {
      uploadBuilder.withPackaging(environment.expand(coordinate.packaging))
    }

    remoteMavenAssets.each { remoteAsset ->
      def mavenAsset = remoteAsset.asset
      def extension = mavenAsset.extension?.trim() ? environment.expand(mavenAsset.extension) : null
      def classifier = mavenAsset.classifier?.trim() ? environment.expand(mavenAsset.classifier) : null

      switch (extension) {
        case 'pom':
          uploadBuilder.withPom(remoteAsset.remotePath)
          break
        case 'jar':
        case '':
        case null:
          uploadBuilder.withJar(remoteAsset.remotePath)
          break
        default:
          uploadBuilder.
              withAsset(new DefaultAsset(localFile.name, new FileInputStream(localFile.absolutePath)), extension)
          break
      }

    }

    try {
      def mavenComponent = new DefaultComponent('maven2')
      mavenComponent.addAttribute('groupId', groupId)
      mavenComponent.addAttribute('artifactId', artifactId)
      mavenComponent.addAttribute('version', version)
      if (coordinate.packaging?.trim()) {
        mavenComponent.addAttribute('packaging', coordinate.packaging)
      }

      remoteMavenAssets.eachWithIndex { remoteAsset, idx ->
        def mavenAsset = remoteAsset.asset
        def extension = mavenAsset.extension?.trim() ? environment.expand(mavenAsset.extension) : null
        def classifier = mavenAsset.classifier?.trim() ? environment.expand(mavenAsset.classifier) : null
        /*
        def localFile = createTempFile(remoteAsset.remotePath.name, 'tmp')
        remoteAsset.remotePath.copyTo(new FilePath(localFile))
        localFiles.add(localFile)
        */
        def asset = new DefaultAsset("asset${idx}", mavenAsset.filePath, remoteAsset.remotePath.read())
        asset.addAttribute('extension', extension)
        if (classifier) {
          asset.addAttribute('classifier', classifier)
        }
        mavenComponent.addAsset(asset)
        /*
        switch (extension) {
          case 'pom':
            uploadBuilder.withPom(localFile.absolutePath, classifier)
            break
          case 'jar':
          case '':
          case null:
            uploadBuilder.withJar(localFile.absolutePath, classifier)
            break
          default:
            uploadBuilder.
                withAsset(new MavenAsset(localFile.name, new FileInputStream(localFile.absolutePath), extension))
            break
        }
        */
      }

      /*
      try {
        mavenUpload = uploadBuilder.build()
      }
      catch (Throwable t) {
        logger.println('Failing build due to invalid upload configuration')
        throw new RepositoryManagerException('Invalid upload configuration', t)
      }
      */

      logger.println()
      logger.println("Uploading maven component with coordinates: ${coordinate}, and assets:")
      remoteMavenAssets.each { logger.println("--${it.asset}") }
      logger.println("to repository: ${repository}")
      logger.println()

      try {
        nx3Client.upload(repository, mavenComponent)
        logger.println("Successfully uploaded ${remoteMavenAssets.size()} assets")
        logger.println()
      }
      catch (Throwable t) {
        def message = "Upload of ${coordinate} failed"
        logger.println(message)
        logger.println('Failing build due to failure to upload component to Nexus Repository Manager Publisher')
        throw (t.class == RepositoryManagerException) ? t : new RepositoryManagerException(message, t)
      }
    }
    finally {
      localFiles.each { it.delete() }
    }
  }
}
