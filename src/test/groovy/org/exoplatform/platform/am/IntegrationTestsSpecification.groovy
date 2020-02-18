/*
 * Copyright (C) 2003-2014 eXo Platform SAS.
 *
 * This file is part of eXo Platform - Add-ons Manager.
 *
 * eXo Platform - Add-ons Manager is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * eXo Platform - Add-ons Manager software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with eXo Platform - Add-ons Manager; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.platform.am

import groovy.util.slurpersupport.GPathResult
import org.exoplatform.platform.am.settings.AddonsManagerSettings
import org.exoplatform.platform.am.settings.EnvironmentSettings
import org.exoplatform.platform.am.settings.PlatformSettings
import org.exoplatform.platform.am.utils.FileUtils
import spock.lang.Shared
import spock.lang.Specification

import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertTrue

/**
 * This class offers various helpers methods to write integration tests
 * @author Arnaud Héritier <aheritier@exoplatform.com>
 */
abstract class IntegrationTestsSpecification extends Specification {

  public static final String IT_SYSPROP_VERBOSE = "it.verbose"
  public static final String IT_SYSPROP_WEB_SERVER_HTTP_PORT = "it.webServerHttpPort"
  public static final String IT_SYSPROP_JACOCO_AGENT = 'it.jacocoAgent'
  public static final String IT_SYSPROP_TESTS_WORKING_DIR_PATH = "it.workingDirPath"
  public static final String IT_SYSPROP_TESTS_DATA_DIR_PATH = "it.dataDirPath"
  public static final String IT_SYSPROP_TESTED_ARTIFACT_PATH = "it.testedArtifactPath"
  public static final String JAVA_HOME = 'java.home'

  final static Map<String, String[]> FOO_ADDON_42_CONTENT = [
      libraries: ["foo-addon-42.jar"],
      webapps  : ["foo-addon-42.war"]
  ]

  final static Map<String, String[]> OTHER_FILES_ADDON_42_CONTENT = [
      libraries  : ["other-files-addon-42.jar"],
      webapps    : ["other-files-addon-42.war"],
      othersFiles: ["conf/other-files-addon/misc1.txt", "conf/other-files-addon/misc2.txt"]
  ]

  final static Map<String, String[]> README_ADDON_42_CONTENT = [
      libraries: ["readme-addon-42.jar"],
      webapps  : ["readme-addon-42.war"]
  ]

  final static Map<String, String[]> FOO_ADDON_43_RC1_CONTENT = [
      libraries: ["foo-addon-43-RC1.jar"],
      webapps  : ["foo-addon-43-RC1.war"]
  ]

  final static Map<String, String[]> FOO_ADDON_43_SNAPSHOT_CONTENT = [
      libraries: ["foo-addon-43-SNAPSHOT.jar"],
      webapps  : ["foo-addon-43-SNAPSHOT.war"]
  ]

  final static Map<String, String[]> PROP_FILES_ADDON_42_CONTENT = [
      libraries  : ["libs/properties-files-addon-42.jar"],
      webapps    : ["webapps/properties-files-addon-42.war"],
      properties : ["conf/configuration1.properties", "conf/properties-files-addon/configuration2.properties"]
  ]

  final static Map<String, String[]> PROP_FILES_PATCH_52_CONTENT = [
          libraries  : ["libs/foo.jar"],
          webapps    : ["webapps/foo.war"]
  ]

  @Shared
  private File _testedArtifact

  /**
   * @return the artifact (Jar File) to test
   */
  File getTestedArtifact() {
    if (!_testedArtifact) {
      assertNotNull("Tested artifact path mustn't be null", System.getProperty(IT_SYSPROP_TESTED_ARTIFACT_PATH))
      _testedArtifact = new File(System.getProperty(IT_SYSPROP_TESTED_ARTIFACT_PATH))
      assertTrue("Tested artifact must exist", _testedArtifact.exists())
    }
    _testedArtifact
  }

  @Shared
  private File _testDataDir

  /**
   * @return the directory where data used for tests are stored (add-ons, catalogs...)
   */
  File getTestDataDir() {
    if (!_testDataDir) {
      assertNotNull("Path to tests data mustn't be null", System.getProperty(IT_SYSPROP_TESTS_DATA_DIR_PATH))
      _testDataDir = new File(System.getProperty(IT_SYSPROP_TESTS_DATA_DIR_PATH))
      assertTrue("Path to tests data must exist", _testDataDir.exists())
      assertTrue("Path to tests data must be a directory", _testDataDir.directory)
    }
    _testDataDir
  }

  @Shared
  private File _plfHome

  /**
   * @return The directory where is installed the PLF instance to test
   */
  File getPlatformHome() {
    if (!_plfHome) {
      assertNotNull("Integration tests directory path mustn't be null", System.getProperty(IT_SYSPROP_TESTS_WORKING_DIR_PATH))
      def integrationTestsDir = new File(System.getProperty(IT_SYSPROP_TESTS_WORKING_DIR_PATH))
      assertTrue("Integration tests directory (${integrationTestsDir}) must be a directory",
                 integrationTestsDir.isDirectory())
      _plfHome = integrationTestsDir.listFiles(
          [accept: { file -> file.directory }] as FileFilter).first()
      assertTrue("PLF_HOME (${_plfHome}) must be a directory", _plfHome.isDirectory())
    }
    _plfHome
  }

  @Shared
  private EnvironmentSettings _envSettings

  /**
   * @return The Environment Settings
   */
  EnvironmentSettings getEnvironmentSettings() {
    if (!_envSettings) {
      _envSettings = new EnvironmentSettings(new AddonsManagerSettings(), new PlatformSettings(getPlatformHome()))
    }
    _envSettings
  }

  /**
   * @return The PLF Settings for the instance to test
   */
  PlatformSettings getPlatformSettings() {
    getEnvironmentSettings().platform
  }

  /**
   * @return The Add-ons Manager Settings
   */
  AddonsManagerSettings getAddonsManagerSettings() {
    getEnvironmentSettings().manager
  }

  /**
   * @return The HTTP port on which the test serer must serve its content
   */
  Integer getWebServerPort() {
    assertNotNull("System property ${IT_SYSPROP_WEB_SERVER_HTTP_PORT} must be set",
                  Integer.getInteger(IT_SYSPROP_WEB_SERVER_HTTP_PORT))
    Integer.getInteger(IT_SYSPROP_WEB_SERVER_HTTP_PORT)
  }

  /**
   * @return The root URL of the web server used for tests
   */
  String getWebServerRootUrl() {
    "http://localhost:${getWebServerPort()}"
  }

  /**
   * @return true if verbose mode must be used while executing tests
   */
  boolean isVerbose() {
    Boolean.getBoolean(IT_SYSPROP_VERBOSE)
  }

  /**
   * Helper method to check that an add-on is correctly installed
   * @param addonContent a Map describing the add-on content. It may have 3 keys and for each a list of paths in the archive.
   * @{code libraries} gives the list of libraries (*.jar) in the archive. @{code webapps} gives the list of webapps (*.war)
   * in the archive. @{code othersFiles} gives the list of other files.
   */
  void verifyAddonContentPresent(Map<String, String[]> addonContent) {
    if (addonContent.libraries) {
      addonContent.libraries.each { library ->
        assert new File(getPlatformSettings().librariesDirectory, FileUtils.extractFilename(library)).exists()
      }
    }
    if (addonContent.webapps) {
      addonContent.webapps.each { webapp ->
        String fileName = FileUtils.extractFilename(webapp)
        assert new File(getPlatformSettings().webappsDirectory, fileName).exists()
        if (PlatformSettings.AppServerType.JBOSS == getPlatformSettings().appServerType) {
          // Verify the application.xml
          GPathResult applicationXmlContent = new XmlSlurper(false, false).parseText(
              new File(getPlatformSettings().webappsDirectory, "META-INF/application.xml").text)
          assert applicationXmlContent.depthFirst().findAll {
            (it.name() == 'module') &&
                (it.'web'.'context-root'.text() == fileName.substring(0, fileName.size() - 4)) &&
                (it.'web'.'web-uri'.text() == fileName)
          }.size() == 1
        }
      }
    }
    if (addonContent.properties) {
      addonContent.properties.each { properties ->
        assert new File(getPlatformSettings().propertiesDirectory, FileUtils.extractParentAndFilename(properties)).exists()      
      }
    }
    if (addonContent.othersFiles) {
      addonContent.othersFiles.each { otherFile ->
        assert new File(getPlatformSettings().homeDirectory, otherFile).exists()
      }
    }
  }

  /**
   * Helper method to check that an add-on is not installed
   * @param addonContent a Map describing the add-on content. It may have 3 keys and for each a list of paths in the archive.
   * @{code libraries} gives the list of libraries (*.jar) in the archive. @{code webapps} gives the list of webapps (*.war)
   * in the archive. @{code othersFiles} gives the list of other files.
   */
  void verifyAddonContentNotPresent(Map<String, String[]> addonContent) {
    if (addonContent.libraries) {
      addonContent.libraries.each { library ->
        assert !new File(getPlatformSettings().librariesDirectory, FileUtils.extractFilename(library)).exists()
      }
    }
    if (addonContent.webapps) {
      addonContent.webapps.each { webapp ->
        assert !new File(getPlatformSettings().webappsDirectory, FileUtils.extractFilename(webapp)).exists()
        if (PlatformSettings.AppServerType.JBOSS == getPlatformSettings().appServerType) {
          // Verify the application.xml
          GPathResult applicationXmlContent = new XmlSlurper(false, false).parseText(
              new File(getPlatformSettings().webappsDirectory, "META-INF/application.xml").text)
          assert applicationXmlContent.depthFirst().findAll {
            (it.name() == 'module') &&
                ((it.'web'.'context-root'.text() == webapp.substring(0, webapp.size() - 4)) ||
                    (it.'web'.'web-uri'.text() == webapp))
          }.size() == 0
        }

      }
    }
    if (addonContent.properties) {
      addonContent.properties.each { properties ->
        assert !new File(getPlatformSettings().propertiesDirectory, FileUtils.extractParentAndFilename(properties)).exists()
      }
    }
    if (addonContent.othersFiles) {
      addonContent.othersFiles.each { otherFile ->
        assert !new File(getPlatformSettings().homeDirectory, otherFile).exists()
      }
    }
  }

  /**
   * Helper method used to execute the add-ons manager
   * @param params Command line parameters to pass to the add-ons manager
   * @return The process result
   */
  ProcessResult launchAddonsManager(List<String> params) {
    launchAddonsManager(params, null, false)
  }

  /**
   * Helper method used to execute the add-ons manager without any output (and no result to use)
   * @param params Command line parameters to pass to the add-ons manager
   */
  void launchAddonsManagerSilently(List<String> params) {
    launchAddonsManager(params, null, true)
  }

  /**
   * Helper method used to execute the add-ons manager
   * @param params Command line parameters to pass to the add-ons manager
   * @param inputs inputs to pass to the process
   * @return The process result
   */
  ProcessResult launchAddonsManager(List<String> params, List<String> inputs) {
    launchAddonsManager(params, inputs, false)
  }

  /**
   * Helper method used to execute the add-ons manager without any output (and no result to use)
   * @param params Command line parameters to pass to the add-ons manager
   * @param inputs inputs to pass to the process
   */
  void launchAddonsManagerSilently(List<String> params, List<String> inputs) {
    launchAddonsManager(params, inputs, true)
  }

  /**
   * Helper method used to execute the add-ons manager
   * @param params Command line parameters to pass to the add-ons manager
   * @param inputs inputs to pass to the process
   * @param silently should we display the command line launched and the outputs ?
   * @return The process result
   */
  ProcessResult launchAddonsManager(List<String> params, List<String> inputs, boolean silently) {
    List<String> commandToExecute = ["${System.getProperty(JAVA_HOME)}/bin/java"]
    // If Jacoco Agent is used, let's pass it to the forked VM
    if (System.getProperty(IT_SYSPROP_JACOCO_AGENT) != null) {
      commandToExecute << "${System.getProperty(IT_SYSPROP_JACOCO_AGENT)}"
    }
    commandToExecute << "-D${PlatformSettings.PLATFORM_HOME_SYS_PROP}=${getPlatformHome().absolutePath}"
    commandToExecute << "-D${AddonsManagerSettings.PROPERTY_PREFIX}.remoteCatalogUrl=${getWebServerRootUrl()}/catalog.json"
    commandToExecute << "-jar" << getTestedArtifact().absolutePath
    commandToExecute.addAll(params)
    if (verbose) {
      commandToExecute << "--verbose"
    }
    // Launch in non-interactive mode if we don't have inputs
    if (!inputs) {
      commandToExecute << "--batch-mode"
    }
    if (!silently) {
      println "Command launched : ${commandToExecute.join(' ')}"
    }
    ProcessResult result = new ProcessResult(commandToExecute.execute())
    if (inputs) {
      result.process.withWriter { writer ->
        inputs.each { writer << "${it}\n" }
      }
    }
    result.waitFor() // Wait for the command to finish
    if (!silently) {
      // Obtain status and output
      println "return code: ${result.exitValue()}"
      println "stderr: ${result.stderrText}"
      println "stdout: ${result.stdoutText}"
    }
    return result
  }

  class ProcessResult {
    @Delegate
    private final Process process
    private String _outText
    private String _errText

    ProcessResult(Process process) {
      this.process = process
    }

    Process getProcess() {
      return process
    }

    String getStdoutText() {
      if (!_outText) {
        // *out* from the external program is *in* for groovy
        _outText = process.in.text
      }
      return _outText
    }

    String getStderrText() {
      if (!_errText) {
        _errText = process.err.text
      }
      return _errText
    }
  }

}
