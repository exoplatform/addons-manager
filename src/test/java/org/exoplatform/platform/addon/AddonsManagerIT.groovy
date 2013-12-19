/**
 * Copyright (C) 2003-2013 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.exoplatform.platform.addon

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

  @Parameters(name = "{index}: exitValue(productHome={0},params={1})={2}")
  static Collection<Object[]> data() {
    def testedArtifactPath = System.getProperty("testedArtifactPath")
    assertNotNull("Tested artifact path mustn't be null", testedArtifactPath)
    def integrationTestsDirPath = System.getProperty("integrationTestsDirPath")
    assertNotNull("Integration tests directory path mustn't be null", integrationTestsDirPath)
    def File integrationTestsDir = new File(integrationTestsDirPath);
    assertTrue("Integration tests directory must be a directory", integrationTestsDir.isDirectory())
    def data = new ArrayList<Object[]>()
    integrationTestsDir.eachDir { directory ->
      // Without any param the program must return an error code 1
      data.add([directory, [""] as String[], CLI.RETURN_CODE_KO] as Object[])
      // With --help param the program must display the help return 0
      data.add([directory, ["--help"] as String[], CLI.RETURN_CODE_OK] as Object[])
      // With --list param the program must display the list of available add-ons and return 0
      data.add([directory, ["--list"] as String[], CLI.RETURN_CODE_OK] as Object[])
      // Install an extension
      data.add([directory, ["--install", "exo-chat-extension"] as String[], CLI.RETURN_CODE_OK] as Object[])
      // Uninstall an extension
      data.add([directory, ["--uninstall", "exo-chat-extension"] as String[], CLI.RETURN_CODE_OK] as Object[])
      // With --list --snapshots param the program must display the list of available add-ons and return 0
      data.add([directory, ["--list", "--snapshots"] as String[], CLI.RETURN_CODE_OK] as Object[])
      // Install another extension with a given version
      data.add([directory, ["--install", "exo-sirona:1.0.0"] as String[], CLI.RETURN_CODE_OK] as Object[])
      // Install the same extension must fail
      data.add([directory, ["--install", "exo-sirona", "--snapshots"] as String[], CLI.RETURN_CODE_KO] as Object[])
      // Install the same extension must succeed if forced
      data.add([directory, ["--install", "exo-sirona", "--snapshots", "--force"] as String[], CLI.RETURN_CODE_OK] as Object[])
      // Uninstall it
      data.add([directory, ["--uninstall", "exo-sirona"] as String[], CLI.RETURN_CODE_OK] as Object[])
      // Install unknown add-on
      data.add([directory, ["--install", "unknown-addon"] as String[], CLI.RETURN_CODE_KO] as Object[])
      // Uninstall unknown add-on
      data.add([directory, ["--uninstall", "unknown-addon"] as String[], CLI.RETURN_CODE_KO] as Object[])
    }
    return data
  }

  @Test
  void testExitValue() {
    def testedArtifactPath = System.getProperty("testedArtifactPath")
    assertNotNull("Tested artifact path mustn't be null", testedArtifactPath)
    println "Testing on ${productHome.name}"
    def commandToExecute = [
        "${System.getProperty('java.home')}/bin/java",
        "-Dproduct.home=${productHome.absolutePath}",
        "-jar", "${testedArtifactPath}",
        "-v"]
    commandToExecute.addAll(params)
    println "Command launched : ${commandToExecute.join(' ')}"
    def process = commandToExecute.execute()
    process.waitFor() // Wait for the command to finish
    // Obtain status and output
    println "return code: ${process.exitValue()}"
    println "stderr: ${process.err.text}"
    println "stdout: ${process.in.text}" // *out* from the external program is *in* for groovy
    assertEquals("Invalid exit value", exitCode, process.exitValue())
  }

}