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
/**
 * @author Arnaud Héritier <aheritier@exoplatform.com>
 */
class AddonsManagerIT extends ITSpecification {

  def setupSpec() {
    startWebServer()
  }

  def cleanupSpec() {
    stopWebServer()
  }

  def "Test exit code"(String[] params, int expectedExitCode) {
    expect:
    println "Testing on ${plfHome().name}, expecting return code ${expectedExitCode} with params \"${params}\""
    expectedExitCode == launchAddonsManager(params)

    where:
    params                                                    | expectedExitCode
    [""]                                                      | AddonsManagerConstants.RETURN_CODE_INVALID_COMMAND_LINE_PARAMS // Without any param the program must return an error code 1
    ["--help"]                                                | AddonsManagerConstants.RETURN_CODE_OK // With --help param the program must display the help return 0
    ["list"]                                                  | AddonsManagerConstants.RETURN_CODE_OK // With list param the program must display the list of available add-ons and return 0
    ["list", "--catalog=${webServerRootUrl()}/catalog2.json"] | AddonsManagerConstants.RETURN_CODE_OK // List add-ons from another catalog [AM_CAT_02]
    ["list", "--no-cache"]                                    | AddonsManagerConstants.RETURN_CODE_OK // List add-ons without using local cache
    ["list", "--offline"]                                     | AddonsManagerConstants.RETURN_CODE_OK // List add-ons in offline mode (thus from data in cache)
    ["list", "--no-cache", "--offline"]                       | AddonsManagerConstants.RETURN_CODE_OK // List add-ons in offline mode with no cache (thus only the local catalog is used)
    ["install", "test-addon"]                                 | AddonsManagerConstants.RETURN_CODE_OK // Install an add-on
    ["uninstall", "test-addon"]                               | AddonsManagerConstants.RETURN_CODE_OK // Uninstall an add-on
    ["list", "--snapshots"]                                   | AddonsManagerConstants.RETURN_CODE_OK // With list --snapshots param the program must display the list of available add-ons and return 0
    ["install", "test-addon:1.0.0"]                           | AddonsManagerConstants.RETURN_CODE_OK // Install another extension with a given version
    ["install", "test-addon", "--snapshots"]                  | AddonsManagerConstants.RETURN_CODE_ADDON_ALREADY_INSTALLED // Install the same extension must fail
    ["install", "test-addon", "--snapshots", "--force"]       | AddonsManagerConstants.RETURN_CODE_OK // Install the same extension  must succeed if forced
    ["uninstall", "test-addon"]                               | AddonsManagerConstants.RETURN_CODE_OK // Uninstall it
    ["install", "test-addon", "--no-cache"]                   | AddonsManagerConstants.RETURN_CODE_OK // Install the same extension without cache must succeed
    ["uninstall", "test-addon"]                               | AddonsManagerConstants.RETURN_CODE_OK // Uninstall it
    ["install", "test-addon", "--offline"]                    | AddonsManagerConstants.RETURN_CODE_OK // Install the same extension in offline mode must succeed
    ["uninstall", "test-addon"]                               | AddonsManagerConstants.RETURN_CODE_OK // Uninstall it
    ["install", "unknown-addon"]                              | AddonsManagerConstants.RETURN_CODE_ADDON_NOT_FOUND // Install unknown add-on
    ["uninstall", "unknown-addon"]                            | AddonsManagerConstants.RETURN_CODE_ADDON_NOT_INSTALLED // Uninstall unknown add-on

  }

}