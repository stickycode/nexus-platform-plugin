/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */
package org.sonatype.nexus.ci.config

import com.sonatype.nexus.api.exception.RepositoryManagerException
import com.sonatype.nexus.api.repository.v3.RepositoryManagerV3Client

import org.sonatype.nexus.ci.config.Nxrm3Configuration.DescriptorImpl
import org.sonatype.nexus.ci.util.Nxrm3Util
import org.sonatype.nexus.ci.util.RepositoryManagerClientUtil

import hudson.util.FormValidation
import hudson.util.FormValidation.Kind
import org.junit.ClassRule
import org.jvnet.hudson.test.JenkinsRule
import spock.lang.Shared
import spock.lang.Specification

class Nxrm3ConfigurationTest
    extends Specification
{
  @ClassRule
  @Shared
  public JenkinsRule jenkins = new JenkinsRule()

  RepositoryManagerV3Client client

  DescriptorImpl configuration

  def setup() {
    client = Mock(RepositoryManagerV3Client.class)
    configuration = (DescriptorImpl) jenkins.getInstance().getDescriptor(Nxrm3Configuration.class)
    GroovyMock(RepositoryManagerClientUtil.class, global: true)
    RepositoryManagerClientUtil.nexus3Client(_, _, _) >> client
  }

  def 'it tests valid server credentials'() {
    when:
      client.getRepositories() >> repositories

    and:
      FormValidation validation = configuration.doVerifyCredentials(serverUrl, credentialsId, anonymousAccess)

    then:
      validation.kind == Kind.OK
      validation.message == "Nexus Repository Manager 3.x connection succeeded (2 hosted release Maven 2 repositories)"

    where:
      serverUrl << ['serverUrl']
      credentialsId << ['credentialsId']
      anonymousAccess << [true]
      repositories << [
          [
              [
                  url   : 'maven-releases',
                  name  : 'Maven Releases',
                  format: 'maven2',
                  type  : 'hosted'
              ],
              [
                  url   : 'maven1-releases',
                  name  : 'Maven 1 Releases',
                  format: 'maven1',
                  type  : 'hosted'
              ],
              [
                  url   : 'maven-snapshots',
                  name  : 'Maven Snapshots',
                  format: 'maven2',
                  type  : 'hosted'
              ],
              [
                  url   : 'maven-proxy',
                  name  : 'Maven Proxy',
                  format: 'maven2',
                  type  : 'proxy'
              ]
          ]
      ]
  }

  def 'it tests invalid server credentials'() {
    when:
      client.getRepositories() >> { throw new RepositoryManagerException("something went wrong") }

    and:
      FormValidation validation = configuration.doVerifyCredentials(serverUrl, credentialsId, anonymousAccess)

    then:
      validation.kind == Kind.ERROR
      validation.message.startsWith("Nexus Repository Manager 3.x connection failed")

    where:
      serverUrl << ['serverUrl']
      credentialsId << ['credentialsId']
      anonymousAccess << [true]
  }

  def 'automatically assumes anonymous access with no credentials'() {
    when:
      GroovySpy(Nxrm3Util.class, global: true)
      client.getRepositories() >> []
    and:
      configuration.doVerifyCredentials(serverUrl, credentialsId, anonymousAccess)

    then:
      1 * Nxrm3Util.getApplicableRepositories(serverUrl, null, true)

    where:
      serverUrl << ['serverUrl']
      credentialsId << [null]
      anonymousAccess << [false]
  }
}
