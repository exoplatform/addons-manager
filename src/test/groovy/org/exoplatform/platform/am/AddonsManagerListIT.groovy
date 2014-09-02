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

import spock.lang.Issue
import spock.lang.Subject

import static org.exoplatform.platform.am.cli.CommandLineParameters.*

/**
 * @author Arnaud Héritier <aheritier@exoplatform.com>
 */
@Subject(AddonsManager)
class AddonsManagerListIT extends IntegrationTestsSpecification {

  def cleanup() {
    // After each test we remove the content of the add-ons directory to be safe
    getEnvironmentSettings().getAddonsDirectory().deleteDir()
  }

  /**
   * List each add-on of the catalog which have at least 1 stable version
   * For each add-on, display *only the last* stable version
   * For each listed Add-on, never display the development and unstable versions
   * Don't list add-ons which contains only development or unstable versions
   */
  @Issue("https://jira.exoplatform.org/browse/AM-22")
  def "[AM_LIST_01] addon(.bat) list"() {
    setup:
    ProcessResult process = launchAddonsManager([LIST_CMD])
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_OK == process.exitValue()
    // For each add-on, display *only the last* stable version
    process.stdoutText.contains("Latest stable version")
    // Never display the development and unstable versions
    !process.stdoutText.contains("Latest development version")
    !process.stdoutText.contains("Latest unstable version")
  }

  /**
   * List add-ons that have stable or development versions
   * For each add-on, display the last stable and the last development version
   */
  @Issue("https://jira.exoplatform.org/browse/AM-23")
  def "[AM_LIST_02] addon(.bat) list --snapshots"() {
    setup:
    ProcessResult process = launchAddonsManager([LIST_CMD, SNAPSHOTS_LONG_OPT])
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_OK == process.exitValue()
    // For each add-on, display the last stable and the last development version
    process.stdoutText.contains("Latest stable version")
    process.stdoutText.contains("Latest development version")
    // Don't display unstable versions
    !process.stdoutText.contains("Latest unstable version")
  }

  /**
   * List add-ons that have stable or unstable versions
   * For each add-on, display the last stable and the last unstable version
   */
  @Issue("https://jira.exoplatform.org/browse/AM-24")
  def "[AM_LIST_03] addon(.bat) list --unstable"() {
    setup:
    ProcessResult process = launchAddonsManager([LIST_CMD, UNSTABLE_LONG_OPT])
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_OK == process.exitValue()
    // For each add-on, display the last stable and the last unstable version
    process.stdoutText.contains("Latest stable version")
    process.stdoutText.contains("Latest unstable version")
    // Don't display development versions
    !process.stdoutText.contains("Latest development version")
  }

  /**
   * List add-ons by sourcing the remote catalog at http://example.org/remote.json (see AM_CAT_02)
   */
  @Issue("https://jira.exoplatform.org/browse/AM-25")
  def "[AM_LIST_04] add-on.(sh|bat) list --catalog=http://example.org/remote.json"() {
    setup:
    ProcessResult process = launchAddonsManager([LIST_CMD, "${CATALOG_LONG_OPT}=${getWebServerRootUrl()}/catalog2.json"])
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_OK == process.exitValue()
  }

  /**
   * List add-ons by re-downloading the remote catalog
   */
  @Issue("https://jira.exoplatform.org/browse/AM-26")
  def "[AM_LIST_05] add-on.(sh|bat) list --no-cache"() {
    setup:
    ProcessResult process = launchAddonsManager([LIST_CMD, NO_CACHE_LONG_OPT])
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_OK == process.exitValue()
  }

  /**
   * List add-ons without downloading the remote catalog (using only the cached and local catalogs)
   */
  @Issue("https://jira.exoplatform.org/browse/AM-27")
  def "[AM_LIST_06] addon(.bat) list --offline"() {
    setup:
    // Let's fill the cache
    launchAddonsManager([LIST_CMD])
    ProcessResult process = launchAddonsManager([LIST_CMD, OFFLINE_LONG_OPT])
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_OK == process.exitValue()
  }

  /**
   * List add-ons without redownloading the remote catalog and the without using the cached catalog (using only the local catalog)
   */
  @Issue("https://jira.exoplatform.org/browse/AM-28")
  def "[AM_LIST_07] add-on.(sh|bat) list --offline --no-cache"() {
    setup:
    // Let's fill the cache
    launchAddonsManager([LIST_CMD])
    ProcessResult process = launchAddonsManager([LIST_CMD, NO_CACHE_LONG_OPT, OFFLINE_LONG_OPT])
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_OK == process.exitValue()
    // Verify error message
    process.stdoutText =~"No remote catalog cache and offline mode activated"
  }

  /**
   * List each add-on installed locally (stable, unstable and development versions)
   */
  @Issue("https://jira.exoplatform.org/browse/AM-29")
  def "[AM_LIST_08] add-on.(sh|bat) list --installed"() {
    setup:
    ProcessResult process = launchAddonsManager([LIST_CMD, INSTALLED_LONG_OPT])
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_OK == process.exitValue()
  }

  /**
   * List installed stable add-ons for which a newer version is available based aether generic version order
   */
  @Issue("https://jira.exoplatform.org/browse/AM-30")
  def "[AM_LIST_09] addon(.bat) list --outdated"() {
    setup:
    // Install it first
    launchAddonsManager([INSTALL_CMD, "foo-addon:40"])
    ProcessResult process = launchAddonsManager([LIST_CMD, OUTDATED_LONG_OPT])
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_OK == process.exitValue()
    cleanup:
    // Uninstall it
    launchAddonsManager([UNINSTALL_CMD, "foo-addon"])
  }

  /**
   * List installed stable and unstable add-ons for which a newer version is available based aether generic version order
   */
  @Issue("https://jira.exoplatform.org/browse/AM-30")
  def "[AM_LIST_09a] addon(.bat) list --outdated --unstable"() {
    setup:
    // Install it first
    launchAddonsManager([INSTALL_CMD, "foo-addon:40"])
    ProcessResult process = launchAddonsManager([LIST_CMD, OUTDATED_LONG_OPT, UNSTABLE_LONG_OPT])
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_OK == process.exitValue()
    cleanup:
    // Uninstall it
    launchAddonsManager([UNINSTALL_CMD, "foo-addon"])
  }

  /**
   * List installed stable and snapshots add-ons for which a newer version is available based aether generic version order
   */
  @Issue("https://jira.exoplatform.org/browse/AM-30")
  def "[AM_LIST_09b] addon(.bat) list --outdated --snapshots"() {
    setup:
    // Install it first
    launchAddonsManager([INSTALL_CMD, "foo-addon:40"])
    ProcessResult process = launchAddonsManager([LIST_CMD, OUTDATED_LONG_OPT, SNAPSHOTS_LONG_OPT])
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_OK == process.exitValue()
    cleanup:
    // Uninstall it
    launchAddonsManager([UNINSTALL_CMD, "foo-addon"])
  }

  /**
   * Unless --no-compat option is passed, the list is filtered to match the app server  with supportedApplicationServers.
   * Example : if supportedApplicationServers contains tomcat and the app server is JBoss, the entry will not be listed
   * Unless --no-compat option is passed, the list is filtered to match the eXo Platform edition with supportedDistributions.
   * Example : if supportedDistributions contains enterprise, the entry will not be listed on a eXo Platform Community Edition.
   * Unless --no-compat option is passed, the list is filtered to match the eXo Platform version with compatibility.
   * Example : if compatibility contains [4.1-M2,), the entry will not be listed on a eXo Platform 4.0
   */
  def "[AM_LIST_10] [AM_LIST_11] [AM_LIST_12] addon(.bat) list --no-compat"() {
    setup:
    ProcessResult process = launchAddonsManager([LIST_CMD, NO_COMPAT_LONG_OPT])
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_OK == process.exitValue()
    // Verify the output
    process.stdoutText.contains("incompatible-foo-addon")
  }

}
