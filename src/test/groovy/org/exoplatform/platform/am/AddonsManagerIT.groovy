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

import org.exoplatform.platform.am.settings.PlatformSettings
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameter
import org.junit.runners.Parameterized.Parameters

import static org.junit.Assert.*

@RunWith(Parameterized.class)
class AddonsManagerIT {

  @Parameter(0)
  public File productHome;

  @Parameter(1)
  public String[] params;

  @Parameter(2)
  public int exitCode;

  @Parameters(name = "{index}: exitValue(plfHome={0},params={1})={2}")
  static Collection<Object[]> data() {
    String testedArtifactPath = System.getProperty("testedArtifactPath")
    assertNotNull("Tested artifact path mustn't be null", testedArtifactPath)
    String integrationTestsDirPath = System.getProperty("integrationTestsDirPath")
    assertNotNull("Integration tests directory path mustn't be null", integrationTestsDirPath)
    File integrationTestsDir = new File(integrationTestsDirPath);
    assertTrue("Integration tests directory must be a directory", integrationTestsDir.isDirectory())
    def data = new ArrayList<Object[]>()
    integrationTestsDir.eachDir { directory ->
      // Without any param the program must return an error code 1
      data.add([directory, [""] as String[], AddonsManagerConstants.RETURN_CODE_INVALID_COMMAND_LINE_PARAMS] as Object[])
      // With --help param the program must display the help return 0
      data.add([directory, ["--help"] as String[], AddonsManagerConstants.RETURN_CODE_OK] as Object[])
      // With list param the program must display the list of available add-ons and return 0
      data.add([directory, ["list"] as String[], AddonsManagerConstants.RETURN_CODE_OK] as Object[])
      // Install an extension
      data.add([directory, ["install", "exo-chat-extension"] as String[], AddonsManagerConstants.RETURN_CODE_OK] as Object[])
      // Uninstall an extension
      data.add([directory, ["uninstall", "exo-chat-extension"] as String[], AddonsManagerConstants.RETURN_CODE_OK] as Object[])
      // With list --snapshots param the program must display the list of available add-ons and return 0
      data.add([directory, ["list", "--snapshots"] as String[], AddonsManagerConstants.RETURN_CODE_OK] as Object[])
      // Install another extension with a given version
      data.add([directory, ["install", "exo-sirona:1.0.0"] as String[], AddonsManagerConstants.RETURN_CODE_OK] as Object[])
      // Install the same extension must fail
      data.add(
          [directory, ["install", "exo-sirona", "--snapshots"] as String[], AddonsManagerConstants.RETURN_CODE_ADDON_ALREADY_INSTALLED] as Object[])
      // Install the same extension must succeed if forced
      data.add(
          [directory, ["install", "exo-sirona", "--snapshots", "--force"] as String[], AddonsManagerConstants.RETURN_CODE_OK] as Object[])
      // Uninstall it
      data.add([directory, ["uninstall", "exo-sirona"] as String[], AddonsManagerConstants.RETURN_CODE_OK] as Object[])
      // Install unknown add-on
      data.add([directory, ["install", "unknown-addon"] as String[], AddonsManagerConstants.RETURN_CODE_ADDON_NOT_FOUND] as Object[])
      // Uninstall unknown add-on
      data.add([directory, ["uninstall", "unknown-addon"] as String[], AddonsManagerConstants.RETURN_CODE_ADDON_NOT_INSTALLED] as Object[])
    }
    return data
  }

  @Test
  void testExitValue() {
    String testedArtifactPath = System.getProperty("testedArtifactPath")
    assertNotNull("Tested artifact path mustn't be null", testedArtifactPath)
    println "Testing on ${productHome.name}, expecting return code ${exitCode} with params ${params.join(" ")}"
    def commandToExecute = ["${System.getProperty('java.home')}/bin/java"]
    // If Jacoco Agent is used, let's pass it to the forked VM
    if (System.getProperty('jacocoAgent') != null) {
      commandToExecute << "${System.getProperty('jacocoAgent')}"
    }
    commandToExecute << "-D${PlatformSettings.PLATFORM_HOME_SYS_PROP}=${productHome.absolutePath}"
    commandToExecute << "-jar"
    commandToExecute << "${testedArtifactPath}"
    commandToExecute << "-v"
    commandToExecute.addAll(params)
    println "Command launched : ${commandToExecute.join(' ')}"
    Process process = commandToExecute.execute()
    process.waitFor() // Wait for the command to finish
    // Obtain status and output
    println "return code: ${process.exitValue()}"
    println "stderr: ${process.err.text}"
    println "stdout: ${process.in.text}" // *out* from the external program is *in* for groovy
    assertEquals("Invalid exit value", exitCode, process.exitValue())
  }

}