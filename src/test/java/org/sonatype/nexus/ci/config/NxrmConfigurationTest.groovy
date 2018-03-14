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

import org.sonatype.nexus.ci.config.SimpleNxrmConfiguration.DescriptorImpl
import org.sonatype.nexus.ci.util.FormUtil

import hudson.util.FormValidation.Kind
import org.junit.Rule
import org.jvnet.hudson.test.JenkinsRule
import spock.lang.Specification

class NxrmConfigurationTest
    extends Specification
{
  @Rule
  public JenkinsRule jenkins = new JenkinsRule()

  def 'it validates that display name is unique'() {
    setup:
      def globalConfiguration = GlobalNexusConfiguration.globalNexusConfiguration
      def nxrmConfiguration = new SimpleNxrmConfiguration('id', 'internalId', 'displayName', 'http://foo.com', 'credId')
      globalConfiguration.nxrmConfigs = []
      globalConfiguration.nxrmConfigs.add(nxrmConfiguration)
      globalConfiguration.save()

      def configuration = (DescriptorImpl) jenkins.getInstance().getDescriptor(SimpleNxrmConfiguration.class)

    when:
      "validating $displayName"
      def validation = configuration.doCheckDisplayName(displayName, 'otherId')

    then:
      "it returns $kind with message $message"
      validation.kind == kind
      validation.renderHtml().startsWith(message)

    where:
      displayName           | kind       | message
      'displayName'         | Kind.ERROR | 'Display Name must be unique'
      'Other Display Name'  | Kind.OK    | '<div/>'
  }

  def 'it validates that display name is required'() {
    setup:
      def configuration = (DescriptorImpl) jenkins.getInstance().getDescriptor(SimpleNxrmConfiguration.class)

    when:
      "validating $displayName"
      def validation = configuration.doCheckDisplayName(displayName, 'id')

    then:
      "it returns $kind with message $message"
      validation.kind == kind
      validation.renderHtml().startsWith(message)

    where:
      displayName              | kind       | message
      ''                       | Kind.ERROR | 'Display Name is required'
      null                     | Kind.ERROR | 'Display Name is required'
      'Other Display Name'     | Kind.OK    | '<div/>'
  }

  def 'it validates that id is unique'() {
    setup:
      def globalConfiguration = GlobalNexusConfiguration.globalNexusConfiguration
      def nxrmConfiguration = new SimpleNxrmConfiguration('id', 'internalId', 'displayName', 'http://foo.com', 'credId')
      globalConfiguration.nxrmConfigs = []
      globalConfiguration.nxrmConfigs.add(nxrmConfiguration)
      globalConfiguration.save()

      def configuration = (DescriptorImpl) jenkins.getInstance().getDescriptor(SimpleNxrmConfiguration.class)

    when:
      "validating $id"
      def validation = configuration.doCheckId(id, 'otherId')

    then:
      "it returns $kind with message $message"
      validation.kind == kind
      validation.renderHtml().startsWith(message)

    where:
      id          | kind       | message
      'id'        | Kind.ERROR | 'Server ID must be unique'
      'other_id'  | Kind.OK    | '<div/>'
  }

  def 'it validates that id is required'() {
    setup:
      def configuration = (DescriptorImpl) jenkins.getInstance().getDescriptor(SimpleNxrmConfiguration.class)

    when:
      "validating $id"
      def validation = configuration.doCheckId(id, 'id')

    then:
      "it returns $kind with message $message"
      validation.kind == kind
      validation.renderHtml().startsWith(message)

    where:
      id           | kind       | message
      ''           | Kind.ERROR | 'Server ID is required'
      null         | Kind.ERROR | 'Server ID is required'
      'other_id'   | Kind.OK    | '<div/>'
  }

  def 'it validates that id is contains no whitespace'() {
    setup:
      def configuration = (DescriptorImpl) jenkins.getInstance().getDescriptor(SimpleNxrmConfiguration.class)

    when:
      "validating $id"
      def validation = configuration.doCheckId(id, 'id')

    then:
      "it returns $kind with message $message"
      validation.kind == kind
      validation.renderHtml().startsWith(message)

    where:
      id            | kind       | message
      ' id'         | Kind.ERROR | 'Server ID must not contain whitespace'
      'i d'         | Kind.ERROR | 'Server ID must not contain whitespace'
      'id '         | Kind.ERROR | 'Server ID must not contain whitespace'
      'other_id'    | Kind.OK    | '<div/>'
  }

  def 'it validates the server url is a valid url'() {
    setup:
      def configuration = (DescriptorImpl) jenkins.getInstance().getDescriptor(SimpleNxrmConfiguration.class)

    when:
      "validating $url"
      def validation = configuration.doCheckServerUrl(url)

    then:
      "it returns $kind with message $message"
      validation.kind == kind
      validation.renderHtml().startsWith(message)

    where:
      url              | kind         | message
      'foo'            | Kind.ERROR   | 'Malformed url (no protocol: foo)'
      'http://foo.com' | Kind.OK      | '<div/>'
  }

  def 'it validates the server url is required'() {
    setup:
      def configuration = (DescriptorImpl) jenkins.getInstance().getDescriptor(SimpleNxrmConfiguration.class)

    when:
      "validating $url"
      def validation = configuration.doCheckServerUrl(url)

    then:
      "it returns $kind with message $message"
      validation.kind == kind
      validation.renderHtml().startsWith(message)

    where:
      url                | kind         | message
      ''                 | Kind.ERROR   | 'Server Url is required'
      null               | Kind.ERROR   | 'Server Url is required'
      'http://foo.com'   | Kind.OK      | '<div/>'
  }

  def 'it loads the credential items'() {
    setup:
      def configuration = (DescriptorImpl) jenkins.getInstance().getDescriptor(SimpleNxrmConfiguration)
      GroovyMock(FormUtil, global: true)

    when:
      configuration.doFillCredentialsIdItems("serverUrl", "credentialsId")

    then:
      1 * FormUtil.newCredentialsItemsListBoxModel("serverUrl", "credentialsId", null)
  }
}
