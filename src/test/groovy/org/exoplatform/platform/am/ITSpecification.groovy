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

import org.apache.catalina.startup.Tomcat
import org.exoplatform.platform.am.settings.AddonsManagerSettings
import org.exoplatform.platform.am.settings.PlatformSettings
import spock.lang.Shared
import spock.lang.Specification

import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertTrue

/**
 * This class offers various helpers methods to write integration tests
 * @author Arnaud Héritier <aheritier@exoplatform.com>
 */
class ITSpecification extends Specification {

  @Shared
  private File _testedArtifact

  /**
   * @return the artifact (Jar File) to test
   */
  File testedArtifact() {
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
   * @return the directory where data used for tests are stored (addons, catalogs...)
   */
  File testDataDir() {
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
  File plfHome() {
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
  private PlatformSettings _plfSettings

  /**
   * @return The PLF Settings for the instance to test
   */
  PlatformSettings plfSettings() {
    if (!plfSettings()) {
      _plfSettings = new PlatformSettings(plfHome())
    }
    _plfSettings
  }

  @Shared
  private Tomcat tomcat

  /**
   * @return The HTTP port on which the test serer must serve its content
   */
  Integer webServerPort() {
    assertNotNull("System property testWebServerHttpPort must be set", Integer.getInteger("testWebServerHttpPort"))
    Integer.getInteger("testWebServerHttpPort")
  }

  /**
   * @return The root URL of the web server used for tests
   */
  String webServerRootUrl() {
    return "http://localhost:${webServerPort()}"
  }

  /**
   * Starts the test web server exposing on / the content of {@link #testDataDir()}.
   * The server is running on localhost : {@link #webServerPort()}
   */
  void startWebServer() {
    if (!tomcat) {
      tomcat = new Tomcat();
      // Let's start a web server to serve test data
      tomcat.setPort(Integer.getInteger("testWebServerHttpPort"));
      tomcat.addWebapp("/", testDataDir().absolutePath);
      tomcat.start();
    }
  }

  /**
   * Stops the web server
   */
  void stopWebServer() {
    if (tomcat) {
      // Let's stop the web server
      tomcat.stop()
      // Tomcat doesn't remove this working directory :(
      new File(System.getProperty("user.dir"), "tomcat.${webServerPort()}").deleteDir()
    }
  }

  /**
   * Helper method used to execute the addons manager
   * @param params Command line parameters to pass to the addons manager
   * @return The process return code
   */
  def launchAddonsManager(String[] params) {
    def commandToExecute = ["${System.getProperty('java.home')}/bin/java"]
    // If Jacoco Agent is used, let's pass it to the forked VM
    if (System.getProperty('jacocoAgent') != null) {
      commandToExecute << "${System.getProperty('jacocoAgent')}"
    }
    commandToExecute << "-D${PlatformSettings.PLATFORM_HOME_SYS_PROP}=${plfHome().absolutePath}"
    commandToExecute << "-D${AddonsManagerSettings.PROPERTY_PREFIX}.centralCatalogUrl=http://localhost:${tomcat.connector.localPort}/catalog.json"
    commandToExecute << "-jar" << testedArtifact().absolutePath
    commandToExecute.addAll(params)
    commandToExecute << "-v"
    println "Command launched : ${commandToExecute.join(' ')}"
    Process process = commandToExecute.execute()
    process.waitFor() // Wait for the command to finish
    // Obtain status and output
    println "return code: ${process.exitValue()}"
    println "stderr: ${process.err.text}"
    println "stdout: ${process.in.text}" // *out* from the external program is *in* for groovy
    return process.exitValue()
  }

}