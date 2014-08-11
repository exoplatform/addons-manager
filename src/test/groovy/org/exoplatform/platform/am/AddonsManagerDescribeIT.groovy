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
class AddonsManagerDescribeIT extends IntegrationTestsSpecification {

  def cleanup() {
    // After each test we remove the content of the add-ons directory to be safe
    getEnvironmentSettings().getAddonsDirectory().deleteDir()
  }

  /**
   * [AM_INF_00] The describe command displays catalog information for a given add-on. All fields are displayed.
   * [AM_INF_00a] In addition to the specified add-on, describe displays information about other versions available in the catalog like this :
   * [AM_INF_00a]  Stable versions : 1.2.0, 1.2.1, 1.3.0
   * [AM_INF_00a]  Unstable versions : 1.1-RC1, 1.2-M1
   * [AM_INF_00a]  Development versions : 1.2.x-SNAPSHOT, 1.3.x-SNAPSHOT
   * [AM_INF_00a] The version of the add-on being described is being highlighted (using bold, underline, or a different color) among the list.
   * [AM_INF_01] If the foo-addon exists, describes most recent stable, unstable or development version of foo-addon
   * [AM_INF_01] If foo-addon doesn't exists in the catalog : must raise an error saying "No add-on with identifier foo-addon found in local or remote catalogs, check your add-on identifier"
   */
  @Issue("https://jira.exoplatform.org/browse/AM-50")
  def "[AM_INF_01] addon(.bat) describe foo-addon"() {
    setup:
    ProcessResult process = launchAddonsManager([DESCRIBE_CMD, "foo-addon"])
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_OK == process.exitValue()
  }

  /**
   * [AM_INF_00] The describe command displays catalog information for a given add-on. All fields are displayed.
   * [AM_INF_00a] In addition to the specified add-on, describe displays information about other versions available in the catalog like this :
   * [AM_INF_00a]  Stable versions : 1.2.0, 1.2.1, 1.3.0
   * [AM_INF_00a]  Unstable versions : 1.1-RC1, 1.2-M1
   * [AM_INF_00a]  Development versions : 1.2.x-SNAPSHOT, 1.3.x-SNAPSHOT
   * [AM_INF_00a] The version of the add-on being described is being highlighted (using bold, underline, or a different color) among the list.
   * [AM_INF_01] If the foo-addon exists, describes most recent stable, unstable or development version of foo-addon
   * [AM_INF_01] If foo-addon doesn't exists in the catalog : must raise an error saying "No add-on with identifier foo-addon found in local or remote catalogs, check your add-on identifier"
   */
  @Issue("https://jira.exoplatform.org/browse/AM-50")
  def "[AM_INF_01] addon(.bat) describe foo-addon - not found"() {
    setup:
    ProcessResult process = launchAddonsManager([DESCRIBE_CMD, "unknown-foo-addon"])
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_ADDON_NOT_FOUND == process.exitValue()
    // Verify error message
    process.stdoutText =~ "No add-on with identifier unknown-foo-addon found in local or remote catalogs, check your add-on identifier."
  }

  /**
   * [AM_INF_02] if the foo-addon exists and has released version 42 : describes the version 42  of foo-addon
   * [AM_INF_02] if the foo-addon exists and has no released version 42 : must raise an error saying The add-on foo-addon doesn't have a version 42. Check the version number
   * [AM_INF_02] if foo-addon doesn't exist in the catalog : must raise an error saying "No add-on with identifier foo-addon found in local or remote catalogs, check your add-on identifier"
   */
  @Issue("https://jira.exoplatform.org/browse/AM-51")
  def "[AM_INF_02] addon(.bat) describe foo-addon:42"() {
    setup:
    ProcessResult process = launchAddonsManager([DESCRIBE_CMD, "foo-addon:42"])
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_OK == process.exitValue()
  }

  /**
   * [AM_INF_02] if the foo-addon exists and has released version 42 : describes the version 42  of foo-addon
   * [AM_INF_02] if the foo-addon exists and has no released version 42 : must raise an error saying The add-on foo-addon doesn't have a version 42. Check the version number
   * [AM_INF_02] if foo-addon doesn't exist in the catalog : must raise an error saying "No add-on with identifier foo-addon found in local or remote catalogs, check your add-on identifier"
   */
  @Issue("https://jira.exoplatform.org/browse/AM-51")
  def "[AM_INF_02] addon(.bat) describe foo-addon:42 - wrong version"() {
    setup:
    ProcessResult process = launchAddonsManager([DESCRIBE_CMD, "foo-addon:1976"])
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_ADDON_NOT_FOUND == process.exitValue()
    // Verify error message
    process.stdoutText =~ "The add-on foo-addon doesn't have a version 1976. Check the version number."
  }

  /**
   * [AM_INF_02] if the foo-addon exists and has released version 42 : describes the version 42  of foo-addon
   * [AM_INF_02] if the foo-addon exists and has no released version 42 : must raise an error saying The add-on foo-addon doesn't have a version 42. Check the version number
   * [AM_INF_02] if foo-addon doesn't exist in the catalog : must raise an error saying "No add-on with identifier foo-addon found in local or remote catalogs, check your add-on identifier"
   */
  @Issue("https://jira.exoplatform.org/browse/AM-51")
  def "[AM_INF_02] addon(.bat) describe foo-addon:42 - not found"() {
    setup:
    ProcessResult process = launchAddonsManager([DESCRIBE_CMD, "unknown-foo-addon:42"])
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_ADDON_NOT_FOUND == process.exitValue()
    // Verify error message
    process.stdoutText =~ "No add-on with identifier unknown-foo-addon found in local or remote catalogs, check your add-on identifier."
  }

  /**
   * Describe an add-on by sourcing the remote catalog at http://example.org/remote.json (see AM_CAT_02)
   * [AM_INF_03] --offline, --catalog and --no-cache can be used with describe command. They control how the catalog is loaded. They work in the same way as the list command.
   */
  @Issue("https://jira.exoplatform.org/browse/AM-50")
  def "[AM_INF_03] add-on.(sh|bat) describe foo-addon --catalog=http://example.org/remote.json"() {
    setup:
    ProcessResult process = launchAddonsManager([DESCRIBE_CMD, "foo-addon", "${CATALOG_LONG_OPT}=${getWebServerRootUrl()}/catalog2.json"])
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_OK == process.exitValue()
  }

  /**
   * Describe an add-on by re-downloading the remote catalog
   * [AM_INF_03] --offline, --catalog and --no-cache can be used with describe command. They control how the catalog is loaded. They work in the same way as the list command.
   */
  @Issue("https://jira.exoplatform.org/browse/AM-50")
  def "[AM_INF_03] add-on.(sh|bat) describe foo-addon --no-cache"() {
    setup:
    ProcessResult process = launchAddonsManager([DESCRIBE_CMD, "foo-addon", NO_CACHE_LONG_OPT])
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_OK == process.exitValue()
  }

  /**
   * Describe an add-on without downloading the remote catalog (using only the cached and local catalogs)
   * [AM_INF_03] --offline, --catalog and --no-cache can be used with describe command. They control how the catalog is loaded. They work in the same way as the list command.
   */
  @Issue("https://jira.exoplatform.org/browse/AM-50")
  def "[AM_INF_03] addon(.bat) describe foo-addon --offline"() {
    setup:
    // Let's put the remote catalog in cache
    launchAddonsManager([DESCRIBE_CMD, "foo-addon"])
    ProcessResult process = launchAddonsManager([DESCRIBE_CMD, "foo-addon", OFFLINE_LONG_OPT])
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_OK == process.exitValue()
  }

  /**
   * Describe an add-on without redownloading the remote catalog and the without using the cached catalog (using only the local catalog)
   * [AM_INF_03] --offline, --catalog and --no-cache can be used with describe command. They control how the catalog is loaded. They work in the same way as the list command.
   */
  @Issue(["https://jira.exoplatform.org/browse/AM-50","https://jira.exoplatform.org/browse/AM-102"])
  def "[AM_INF_03] add-on.(sh|bat) describe foo-addon --offline --no-cache"() {
    setup:
    // Let's put the remote catalog in cache
    launchAddonsManager([DESCRIBE_CMD, "foo-addon"])
    ProcessResult process = launchAddonsManager([DESCRIBE_CMD, "foo-addon", NO_CACHE_LONG_OPT, OFFLINE_LONG_OPT])
    expect:
    // Verify return code (The local catalog is empty and we cannot use the remote cache)
    AddonsManagerConstants.RETURN_CODE_ADDON_NOT_FOUND == process.exitValue()
  }
}
