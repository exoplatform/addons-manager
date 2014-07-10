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

import org.exoplatform.platform.am.settings.AddonsManagerSettings
import org.exoplatform.platform.am.settings.EnvironmentSettings
import org.exoplatform.platform.am.settings.PlatformSettings
import spock.lang.Shared
import spock.lang.Specification

import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertTrue

/**
 * This class offers various helpers methods to write integration tests
 * @author Arnaud Héritier <aheritier@exoplatform.com>
 */
abstract class IntegrationTestsSpecification extends Specification {

  @Shared
  private File _testedArtifact

  /**
   * @return the artifact (Jar File) to test
   */
  File getTestedArtifact() {
    if (!_testedArtifact) {
      assertNotNull("Tested artifact path mustn't be null", System.getProperty("testedArtifactPath"))
      _testedArtifact = new File(System.getProperty("testedArtifactPath"))
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
      assertNotNull("Path to tests data mustn't be null", System.getProperty("testDataPath"))
      _testDataDir = new File(System.getProperty("testDataPath"))
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
      assertNotNull("Integration tests directory path mustn't be null", System.getProperty("integrationTestsDirPath"))
      def integrationTestsDir = new File(System.getProperty("integrationTestsDirPath"))
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
    assertNotNull("System property testWebServerHttpPort must be set", Integer.getInteger("testWebServerHttpPort"))
    Integer.getInteger("testWebServerHttpPort")
  }

  /**
   * @return The root URL of the web server used for tests
   */
  String getWebServerRootUrl() {
    return "http://localhost:${getWebServerPort()}"
  }

  /**
   * Helper method used to execute the add-ons manager
   * @param params Command line parameters to pass to the add-ons manager
   * @return The process result
   */
  ProcessResult launchAddonsManager(List<String> params) {
    launchAddonsManager(params, null)
  }

  /**
   * Helper method used to execute the add-ons manager
   * @param params Command line parameters to pass to the add-ons manager
   * @param inputs inputs to pass to the process
   * @return The process result
   */
  ProcessResult launchAddonsManager(List<String> params, List<String> inputs) {
    List<String> commandToExecute = ["${System.getProperty('java.home')}/bin/java"]
    // If Jacoco Agent is used, let's pass it to the forked VM
    if (System.getProperty('jacocoAgent') != null) {
      commandToExecute << "${System.getProperty('jacocoAgent')}"
    }
    commandToExecute << "-D${PlatformSettings.PLATFORM_HOME_SYS_PROP}=${getPlatformHome().absolutePath}"
    commandToExecute << "-D${AddonsManagerSettings.PROPERTY_PREFIX}.remoteCatalogUrl=${getWebServerRootUrl()}/catalog.json"
    commandToExecute << "-jar" << getTestedArtifact().absolutePath
    commandToExecute.addAll(params)
    println "Command launched : ${commandToExecute.join(' ')}"
    ProcessResult result = new ProcessResult(commandToExecute.execute())
    if (inputs) {
      result.process.withWriter { writer ->
        inputs.each { writer << "${it}\n" }
      }
    }
    result.waitFor() // Wait for the command to finish
    // Obtain status and output
    println "return code: ${result.exitValue()}"
    println "stderr: ${result.stderrText}"
    println "stdout: ${result.stdoutText}" // *out* from the external program is *in* for groovy
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