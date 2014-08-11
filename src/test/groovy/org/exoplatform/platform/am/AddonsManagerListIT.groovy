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

import static org.exoplatform.platform.am.cli.CommandLineParameters.*

/**
 * @author Arnaud Héritier <aheritier@exoplatform.com>
 */
class AddonsManagerListIT extends IntegrationTestsSpecification {

  def cleanup() {
    // After each test we remove the content of the add-ons directory to be safe
    getEnvironmentSettings().getAddonsDirectory().deleteDir()
  }

  /**
   * list each Add-on of the Catalog which have at least 1 stable version
   * for each listed Add-on, list all the stable versions
   * for each listed Add-on, never list the development and unstable versions
   * don't list add-ons which contains only development or unstable versions
   */
  def "[AM_LIST_01] addon(.bat) list"() {
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_OK == launchAddonsManager([LIST_CMD]).exitValue()
  }

  /**
   * list each add-on of the Catalog (stable and development versions)
   * for each listed Add-on, list all the versions (stable and development)
   */
  def "[AM_LIST_02] addon(.bat) list --snapshots"() {
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_OK == launchAddonsManager([LIST_CMD, SNAPSHOTS_LONG_OPT]).exitValue()
  }

  /**
   * List stable and unstable add-ons
   */
  def "[AM_LIST_03] addon(.bat) list --unstable"() {
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_OK == launchAddonsManager([LIST_CMD, UNSTABLE_LONG_OPT]).exitValue()
  }

  /**
   * list each add-on in the catalog at http://example.org/list.json
   */
  def "[AM_LIST_04] add-on.(sh|bat) list --catalog=http://example.org/list.json"() {
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_OK == launchAddonsManager(
        [LIST_CMD, "${CATALOG_LONG_OPT}=${getWebServerRootUrl()}/catalog2.json"]).exitValue()
  }

  /**
   * same as list without arguments, but does not use the cached catalog
   */
  def "[AM_LIST_05] add-on.(sh|bat) list --no-cache"() {
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_OK == launchAddonsManager([LIST_CMD, NO_CACHE_LONG_OPT]).exitValue()
  }

  /**
   * same as list without arguments,only using the local + cached catalogs
   */
  def "[AM_LIST_06] addon(.bat) list --offline"() {
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_OK == launchAddonsManager([LIST_CMD, OFFLINE_LONG_OPT]).exitValue()
  }

  /**
   * same as list without arguments, only using the local catalog
   */
  def "[AM_LIST_07] add-on.(sh|bat) list --offline --no-cache"() {
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_OK == launchAddonsManager([LIST_CMD, NO_CACHE_LONG_OPT, OFFLINE_LONG_OPT]).exitValue()
  }

  /**
   * list each add-on installed locally (stable and development versions)
   */
  def "[AM_LIST_08] add-on.(sh|bat) list --installed"() {
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_OK == launchAddonsManager([LIST_CMD, INSTALLED_LONG_OPT]).exitValue()
  }

  /**
   * List installed stable add-ons for which a newer version is available based aether generic version order
   */
  def "[AM_LIST_09] addon(.bat) list --outdated"() {
    setup:
    // Install it first
    launchAddonsManager([INSTALL_CMD, "foo-addon:40"])
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_OK == launchAddonsManager([LIST_CMD, OUTDATED_LONG_OPT]).exitValue()
    cleanup:
    // Uninstall it
    launchAddonsManager([UNINSTALL_CMD, "foo-addon"])
  }

  /**
   * List installed stable and unstable add-ons for which a newer version is available based aether generic version order
   */
  def "[AM_LIST_09a] addon(.bat) list --outdated --unstable"() {
    setup:
    // Install it first
    launchAddonsManager([INSTALL_CMD, "foo-addon:40"])
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_OK == launchAddonsManager([LIST_CMD, OUTDATED_LONG_OPT, UNSTABLE_LONG_OPT]).exitValue()
    cleanup:
    // Uninstall it
    launchAddonsManager([UNINSTALL_CMD, "foo-addon"])
  }

  /**
   * List installed stable and snapshots add-ons for which a newer version is available based aether generic version order
   */
  def "[AM_LIST_09b] addon(.bat) list --outdated --snapshots"() {
    setup:
    // Install it first
    launchAddonsManager([INSTALL_CMD, "foo-addon:40"])
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_OK == launchAddonsManager([LIST_CMD, OUTDATED_LONG_OPT, SNAPSHOTS_LONG_OPT]).exitValue()
    cleanup:
    // Uninstall it
    launchAddonsManager([UNINSTALL_CMD, "foo-addon"])
  }

  /**
   * Unless --no-compat option is passed, the list is filtered to match the app server with supportedApplicationServers,
   * supportedDistributions, compatibility.
   */
  def "[AM_LIST_10] [AM_LIST_11] [AM_LIST_12] addon(.bat) list --no-compat"() {
    setup:
    ProcessResult process = launchAddonsManager([LIST_CMD, NO_COMPAT_LONG_OPT])
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_OK == process.exitValue()
    // Verify the output
    process.stdoutText =~ "incompatible-foo-addon"
  }

}
