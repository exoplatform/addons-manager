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

import spock.lang.Subject

import static org.exoplatform.platform.am.cli.CommandLineParameters.*
/**
 * @author Arnaud Héritier <aheritier@exoplatform.com>
 */
@Subject(AddonsManager)
class AddonsManagerInstallIT extends IntegrationTestsSpecification {

  def cleanup() {
    // After each test we remove the content of the add-ons directory to be safe
    getEnvironmentSettings().getAddonsDirectory().deleteDir()
  }

  /**
   * if foo-addon is not already installed : must install the most recent stable version of foo-addon
   */
  def "[AM_INST_01] addon(.bat) install foo-addon - not yet installed"() {
    setup:
    ProcessResult process = launchAddonsManager([INSTALL_CMD, "foo-addon"])
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_OK == process.exitValue()
    // Verify that the add-on is correctly installed
    verifyAddonContentPresent(FOO_ADDON_42_CONTENT)
    cleanup:
    // Uninstall it
    launchAddonsManagerSilently([UNINSTALL_CMD, "foo-addon"])
  }

  /**
   * if foo-addon is already installed : must not install anything
   */
  def "[AM_INST_01] addon(.bat) install foo-addon - already installed"() {
    setup:
    // Install it first
    launchAddonsManagerSilently([INSTALL_CMD, "foo-addon"])
    ProcessResult process = launchAddonsManager([INSTALL_CMD, "foo-addon"])
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_ADDON_ALREADY_INSTALLED == process.exitValue()
    cleanup:
    // Uninstall it
    launchAddonsManagerSilently([UNINSTALL_CMD, "foo-addon"])
  }

  /**
   * if foo-addon doesn't exists in the catalog : must raise an error saying No add-on with identifier foo-addon found in local or remote catalogs, check your add-on identifier
   */
  def "[AM_INST_01] addon(.bat) install foo-addon - not found"() {
    setup:
    ProcessResult process = launchAddonsManager([INSTALL_CMD, "unknown-foo-addon"])
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_ADDON_NOT_FOUND == process.exitValue()
    // Verify error message
    process.stdoutText.contains(
        "No add-on with identifier unknown-foo-addon found in local or remote catalogs, check your add-on identifier."
    )
  }

  /**
   * if foo-addon is not already installed : must install the most recent development version of the foo-addon
   */
  def "[AM_INST_02] addon(.bat) install foo-addon --snapshots - not yet installed"() {
    setup:
    ProcessResult process = launchAddonsManager([INSTALL_CMD, "foo-addon", SNAPSHOTS_LONG_OPT])
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_OK == process.exitValue()
    // Verify that the add-on is correctly installed
    verifyAddonContentPresent(FOO_ADDON_43_SNAPSHOT_CONTENT)
    cleanup:
    // Uninstall it
    launchAddonsManagerSilently([UNINSTALL_CMD, "foo-addon"])
  }

  /**
   * if foo-addon is already installed : must not install anything
   * TODO : if the last stable version is more recent than the most recent development version, we must install the stable version
   */
  def "[AM_INST_02] addon(.bat) install foo-addon --snapshots - already installed"() {
    setup:
    // Install it first
    launchAddonsManagerSilently([INSTALL_CMD, "foo-addon", SNAPSHOTS_LONG_OPT])
    ProcessResult process = launchAddonsManager([INSTALL_CMD, "foo-addon", SNAPSHOTS_LONG_OPT])
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_ADDON_ALREADY_INSTALLED == process.exitValue()
    cleanup:
    // Uninstall it
    launchAddonsManagerSilently([UNINSTALL_CMD, "foo-addon"])
  }

  /**
   * if foo-addon doesn't exists in the catalog : must raise an error saying : No add-on with identifier foo-addon found in local or remote catalogs, check your add-on identifier
   */
  def "[AM_INST_02] addon(.bat) install foo-addon --snapshots - not found"() {
    setup:
    ProcessResult process = launchAddonsManager([INSTALL_CMD, "unknown-foo-addon", SNAPSHOTS_LONG_OPT])
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_ADDON_NOT_FOUND == process.exitValue()
    // Verify error message
    process.stdoutText.contains(
        "No add-on with identifier unknown-foo-addon found in local or remote catalogs, check your add-on identifier."
    )
  }

  /**
   * if foo-addon is not already installed : must install the most recent unstable version of the foo-addon (based on  aether generic version order)
   */
  def "[AM_INST_14] addon(.bat) install foo-addon --unstable - not yet installed"() {
    setup:
    ProcessResult process = launchAddonsManager([INSTALL_CMD, "foo-addon", UNSTABLE_LONG_OPT])
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_OK == process.exitValue()
    // Verify that the add-on is correctly installed
    verifyAddonContentPresent(FOO_ADDON_43_RC1_CONTENT)
    cleanup:
    // Uninstall it
    launchAddonsManagerSilently([UNINSTALL_CMD, "foo-addon"])
  }

  /**
   * if foo-addon is already installed : must not install anything
   */
  def "[AM_INST_14] addon(.bat) install foo-addon --unstable - already installed"() {
    setup:
    // Install it first
    launchAddonsManagerSilently([INSTALL_CMD, "foo-addon", UNSTABLE_LONG_OPT])
    ProcessResult process = launchAddonsManager([INSTALL_CMD, "foo-addon", UNSTABLE_LONG_OPT])
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_ADDON_ALREADY_INSTALLED == process.exitValue()
    cleanup:
    // Uninstall it
    launchAddonsManagerSilently([UNINSTALL_CMD, "foo-addon"])
  }

  /**
   * if foo-addon doesn't exists in the catalog : must raise an error saying : No add-on with identifier foo-addon found in local or remote catalogs, check your add-on identifier
   */
  def "[AM_INST_14] addon(.bat) install foo-addon --unstable - not found"() {
    setup:
    ProcessResult process = launchAddonsManager([INSTALL_CMD, "unknown-foo-addon", UNSTABLE_LONG_OPT])
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_ADDON_NOT_FOUND == process.exitValue()
    // Verify error message
    process.stdoutText.contains(
        "No add-on with identifier unknown-foo-addon found in local or remote catalogs, check your add-on identifier."
    )
  }

  /**
   * if foo-addon not already installed : must install the most recent stable version of the foo-addon
   */
  def "[AM_INST_03] addon(.bat) install foo-addon --force - not yet installed"() {
    setup:
    ProcessResult process = launchAddonsManager([INSTALL_CMD, "foo-addon", FORCE_LONG_OPT])
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_OK == process.exitValue()
    // Verify that the add-on is correctly installed
    verifyAddonContentPresent(FOO_ADDON_42_CONTENT)
    cleanup:
    // Uninstall it
    launchAddonsManagerSilently([UNINSTALL_CMD, "foo-addon"])
  }

  /**
   * if foo-addon is already installed : must enforce to reinstall the foo-addon
   */
  def "[AM_INST_03] addon(.bat) install foo-addon --force - already installed"() {
    setup:
    // Install it first
    launchAddonsManagerSilently([INSTALL_CMD, "foo-addon"])
    ProcessResult process = launchAddonsManager([INSTALL_CMD, "foo-addon", FORCE_LONG_OPT])
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_OK == process.exitValue()
    // Verify that the add-on is correctly installed
    verifyAddonContentPresent(FOO_ADDON_42_CONTENT)
    cleanup:
    // Uninstall it
    launchAddonsManagerSilently([UNINSTALL_CMD, "foo-addon"])
  }

  /**
   * if foo-addon doesn't exists in the catalog : must raise an error saying No add-on with identifier foo-addon found in local or remote catalogs, check your add-on identifier
   */
  def "[AM_INST_03] addon(.bat) install foo-addon --force - not found"() {
    setup:
    ProcessResult process = launchAddonsManager([INSTALL_CMD, "unknown-foo-addon", FORCE_LONG_OPT])
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_ADDON_NOT_FOUND == process.exitValue()
    // Verify error message
    process.stdoutText.contains(
        "No add-on with identifier unknown-foo-addon found in local or remote catalogs, check your add-on identifier."
    )
  }

  /**
   * if foo-addon not already installed : must install the most recent development version of the foo-addon
   */
  def "[AM_INST_04] addon(.bat) install foo-addon --snapshots --force - not yet installed"() {
    setup:
    ProcessResult process = launchAddonsManager([INSTALL_CMD, "foo-addon", SNAPSHOTS_LONG_OPT, FORCE_LONG_OPT])
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_OK == process.exitValue()
    // Verify that the add-on is correctly installed
    verifyAddonContentPresent(FOO_ADDON_43_SNAPSHOT_CONTENT)
    cleanup:
    // Uninstall it
    launchAddonsManagerSilently([UNINSTALL_CMD, "foo-addon"])
  }

  /**
   * if foo-addon is already installed : must enforce to reinstall the foo-addon with its more recent development version
   * TODO : if the last stable version is more recent than the most recent development version, we must install / reinstall the stable version
   */
  def "[AM_INST_04] addon(.bat) install foo-addon --snapshots --force - already installed"() {
    setup:
    // Install it first
    launchAddonsManagerSilently([INSTALL_CMD, "foo-addon", SNAPSHOTS_LONG_OPT])
    ProcessResult process = launchAddonsManager([INSTALL_CMD, "foo-addon", SNAPSHOTS_LONG_OPT, FORCE_LONG_OPT])
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_OK == process.exitValue()
    // Verify that the add-on is correctly installed
    verifyAddonContentPresent(FOO_ADDON_43_SNAPSHOT_CONTENT)
    cleanup:
    // Uninstall it
    launchAddonsManagerSilently([UNINSTALL_CMD, "foo-addon"])
  }

  /**
   * if foo-addon doesn't exists in the catalog : must raise an error saying No add-on with identifier foo-addon found in local or remote catalogs, check your add-on identifier
   */
  def "[AM_INST_04] addon(.bat) install foo-addon --snapshots --force - not found"() {
    setup:
    ProcessResult process = launchAddonsManager([INSTALL_CMD, "unknown-foo-addon", SNAPSHOTS_LONG_OPT, FORCE_LONG_OPT])
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_ADDON_NOT_FOUND == process.exitValue()
    // Verify error message
    process.stdoutText.contains(
        "No add-on with identifier unknown-foo-addon found in local or remote catalogs, check your add-on identifier."
    )
  }

  /**
   * if foo-addon not already installed : must install the most recent unstable version of the foo-addon
   */
  def "addon(.bat) install foo-addon --unstable --force - not yet installed"() {
    setup:
    ProcessResult process = launchAddonsManager([INSTALL_CMD, "foo-addon", UNSTABLE_LONG_OPT, FORCE_LONG_OPT])
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_OK == process.exitValue()
    // Verify that the add-on is correctly installed
    verifyAddonContentPresent(FOO_ADDON_43_RC1_CONTENT)
    cleanup:
    // Uninstall it
    launchAddonsManagerSilently([UNINSTALL_CMD, "foo-addon"])
  }

  /**
   * if foo-addon is already installed : must enforce to reinstall the foo-addon with its more recent unstable version
   * TODO : if the last stable version is more recent than the most recent unstable version, we must install / reinstall the stable
   * version
   */
  def "addon(.bat) install foo-addon --unstable --force - already installed"() {
    setup:
    // Install it first
    launchAddonsManagerSilently([INSTALL_CMD, "foo-addon", UNSTABLE_LONG_OPT])
    ProcessResult process = launchAddonsManager([INSTALL_CMD, "foo-addon", UNSTABLE_LONG_OPT, FORCE_LONG_OPT])
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_OK == process.exitValue()
    // Verify that the add-on is correctly installed
    verifyAddonContentPresent(FOO_ADDON_43_RC1_CONTENT)
    cleanup:
    // Uninstall it
    launchAddonsManagerSilently([UNINSTALL_CMD, "foo-addon"])
  }

  /**
   * if foo-addon doesn't exists in the catalog : must raise an error saying : No add-on with identifier foo-addon found in local or remote catalogs, check your add-on identifier
   */
  def "addon(.bat) install foo-addon --unstable --force - not found"() {
    setup:
    ProcessResult process = launchAddonsManager([INSTALL_CMD, "unknown-foo-addon", UNSTABLE_LONG_OPT, FORCE_LONG_OPT])
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_ADDON_NOT_FOUND == process.exitValue()
    // Verify error message
    process.stdoutText.contains(
        "No add-on with identifier unknown-foo-addon found in local or remote catalogs, check your add-on identifier."
    )
  }

  /**
   * if foo-addon not already installed : must install the version 42 of the foo-addon
   */
  def "[AM_INST_05] addon(.bat) install foo-addon:42 - not yet installed"() {
    setup:
    ProcessResult process = launchAddonsManager([INSTALL_CMD, "foo-addon:42"])
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_OK == process.exitValue()
    // Verify that the add-on is correctly installed
    verifyAddonContentPresent(FOO_ADDON_42_CONTENT)
    cleanup:
    // Uninstall it
    launchAddonsManagerSilently([UNINSTALL_CMD, "foo-addon"])
  }

  /**
   * if foo-addon is already installed : must not install anything
   */
  def "[AM_INST_05] addon(.bat) install foo-addon:42 - already installed"() {
    setup:
    // Install it first
    launchAddonsManagerSilently([INSTALL_CMD, "foo-addon"])
    ProcessResult process = launchAddonsManager([INSTALL_CMD, "foo-addon:42"])
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_ADDON_ALREADY_INSTALLED == process.exitValue()
    cleanup:
    // Uninstall it
    launchAddonsManagerSilently([UNINSTALL_CMD, "foo-addon"])
  }

  /**
   * if the foo-addon exists and has no released version 42 : must raise an error saying The add-on foo-addon doesn't have a version 42. Check the version number
   */
  def "[AM_INST_05] addon(.bat) install foo-addon - wrong version"() {
    setup:
    ProcessResult process = launchAddonsManager([INSTALL_CMD, "foo-addon:1976"])
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_ADDON_NOT_FOUND == process.exitValue()
    // Verify error message
    process.stdoutText.contains(
        "The add-on foo-addon doesn't have a version 1976. Check the version number."
    )
  }

  /**
   * if foo-addon doesn't exists in the catalog : must raise an error saying No add-on with identifier foo-addon found in local or remote catalogs, check your add-on identifier
   */
  def "[AM_INST_05] addon(.bat) install foo-addon - not found"() {
    setup:
    ProcessResult process = launchAddonsManager([INSTALL_CMD, "unknown-foo-addon:42"])
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_ADDON_NOT_FOUND == process.exitValue()
    // Verify error message
    process.stdoutText.contains(
        "No add-on with identifier unknown-foo-addon found in local or remote catalogs, check your add-on identifier."
    )
  }

  /**
   * if foo-addon not already installed : must install the version 43-SNAPSHOT of the foo-addon
   */
  def "addon(.bat) install foo-addon:43-SNAPSHOT --snapshots - not yet installed"() {
    setup:
    ProcessResult process = launchAddonsManager([INSTALL_CMD, "foo-addon:43-SNAPSHOT", SNAPSHOTS_LONG_OPT])
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_OK == process.exitValue()
    // Verify that the add-on is correctly installed
    verifyAddonContentPresent(FOO_ADDON_43_SNAPSHOT_CONTENT)
    cleanup:
    // Uninstall it
    launchAddonsManagerSilently([UNINSTALL_CMD, "foo-addon"])
  }

  /**
   * if foo-addon is already installed : must not install anything
   */
  def "addon(.bat) install foo-addon:43-SNAPSHOT --snapshots - already installed"() {
    setup:
    // Install it first
    launchAddonsManagerSilently([INSTALL_CMD, "foo-addon:43-SNAPSHOT", SNAPSHOTS_LONG_OPT])
    ProcessResult process = launchAddonsManager([INSTALL_CMD, "foo-addon", SNAPSHOTS_LONG_OPT])
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_ADDON_ALREADY_INSTALLED == process.exitValue()
    cleanup:
    // Uninstall it
    launchAddonsManagerSilently([UNINSTALL_CMD, "foo-addon"])
  }

  /**
   * if the foo-addon exists and has no released version 42-SNAPSHOT : must raise an error saying "The add-on foo-addon doesn't have a version 42-SNAPSHOT. Check the version
   * number"
   */
  def "addon(.bat) install foo-addon:43-SNAPSHOT --snapshots - wrong version"() {
    setup:
    ProcessResult process = launchAddonsManager([INSTALL_CMD, "foo-addon:1976-SNAPSHOT", SNAPSHOTS_LONG_OPT])
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_ADDON_NOT_FOUND == process.exitValue()
    // Verify error message
    process.stdoutText.contains(
        "The add-on foo-addon doesn't have a version 1976-SNAPSHOT. Check the version number."
    )
  }

  /**
   * if foo-addon doesn't exists in the catalog : must raise an error saying "No add-on with identifier foo-addon found in local or remote catalogs, check your add-on identifier"
   */
  def "addon(.bat) install foo-addon:43-SNAPSHOT --snapshots - not found"() {
    setup:
    ProcessResult process = launchAddonsManager([INSTALL_CMD, "unknown-foo-addon:43-SNAPSHOT", SNAPSHOTS_LONG_OPT])
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_ADDON_NOT_FOUND == process.exitValue()
    // Verify error message
    process.stdoutText.contains(
        "No add-on with identifier unknown-foo-addon found in local or remote catalogs, check your add-on identifier."
    )
  }

  /**
   * if foo-addon not already installed : must install the 43-RC1 version of the foo-addon
   */
  def "addon(.bat) install foo-addon:43-RC1 --unstable - not yet installed"() {
    setup:
    ProcessResult process = launchAddonsManager([INSTALL_CMD, "foo-addon:43-RC1", UNSTABLE_LONG_OPT])
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_OK == process.exitValue()
    // Verify that the add-on is correctly installed
    verifyAddonContentPresent(FOO_ADDON_43_RC1_CONTENT)
    cleanup:
    // Uninstall it
    launchAddonsManagerSilently([UNINSTALL_CMD, "foo-addon"])
  }

  /**
   * if foo-addon is already installed : must not install anything
   */
  def "addon(.bat) install foo-addon:43-RC1 --unstable - already installed"() {
    setup:
    // Install it first
    launchAddonsManagerSilently([INSTALL_CMD, "foo-addon:43-RC1", UNSTABLE_LONG_OPT])
    ProcessResult process = launchAddonsManager([INSTALL_CMD, "foo-addon", UNSTABLE_LONG_OPT])
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_ADDON_ALREADY_INSTALLED == process.exitValue()
    cleanup:
    // Uninstall it
    launchAddonsManagerSilently([UNINSTALL_CMD, "foo-addon"])
  }

  /**
   * if the foo-addon exists and has no released version 43-RC1 : must raise an error saying "The add-on foo-addon doesn't have a version 43-RC1. Check the version number"
   */
  def "addon(.bat) install foo-addon:43-RC1 --unstable - wrong version"() {
    setup:
    ProcessResult process = launchAddonsManager([INSTALL_CMD, "foo-addon:1976-RC1", UNSTABLE_LONG_OPT])
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_ADDON_NOT_FOUND == process.exitValue()
    // Verify error message
    process.stdoutText.contains(
        "The add-on foo-addon doesn't have a version 1976-RC1. Check the version number."
    )
  }

  /**
   * if foo-addon doesn't exists in the catalog : must raise an error saying "No add-on with identifier foo-addon found in local or remote catalogs, check your add-on identifier"
   */
  def "addon(.bat) install foo-addon:43-RC1 --unstable - not found"() {
    setup:
    ProcessResult process = launchAddonsManager([INSTALL_CMD, "unknown-foo-addon:43-RC1", UNSTABLE_LONG_OPT])
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_ADDON_NOT_FOUND == process.exitValue()
    // Verify error message
    process.stdoutText.contains(
        "No add-on with identifier unknown-foo-addon found in local or remote catalogs, check your add-on identifier."
    )
  }

  /**
   * if foo-addon not already installed : must install the version 42 of the foo-addon
   */
  def "[AM_INST_07] addon(.bat) install foo-addon:42 --force - not yet installed"() {
    setup:
    ProcessResult process = launchAddonsManager([INSTALL_CMD, "foo-addon:42", FORCE_LONG_OPT])
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_OK == process.exitValue()
    // Verify that the add-on is correctly installed
    verifyAddonContentPresent(FOO_ADDON_42_CONTENT)
    cleanup:
    // Uninstall it
    launchAddonsManagerSilently([UNINSTALL_CMD, "foo-addon"])
  }

  /**
   * if foo-addon is already installed : must enforce to reinstall the version 42 of the foo-addon
   */
  def "[AM_INST_07] addon(.bat) install foo-addon:42 --force - already installed"() {
    setup:
    // Install it first
    launchAddonsManagerSilently([INSTALL_CMD, "foo-addon"])
    ProcessResult process = launchAddonsManager([INSTALL_CMD, "foo-addon:42", FORCE_LONG_OPT])
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_OK == process.exitValue()
    // Verify that the add-on is correctly installed
    verifyAddonContentPresent(FOO_ADDON_42_CONTENT)
    cleanup:
    // Uninstall it
    launchAddonsManagerSilently([UNINSTALL_CMD, "foo-addon"])
  }

  /**
   * if the foo-addon exists and has no released version 42 : must raise an error saying "The add-on foo-addon doesn't have a version 42. Check the version number"
   */
  def "[AM_INST_07] addon(.bat) install foo-addon:42 --force - wrong version"() {
    setup:
    ProcessResult process = launchAddonsManager([INSTALL_CMD, "foo-addon:1976", FORCE_LONG_OPT])
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_ADDON_NOT_FOUND == process.exitValue()
    // Verify error message
    process.stdoutText.contains(
        "The add-on foo-addon doesn't have a version 1976. Check the version number"
    )
  }

  /**
   * if foo-addon doesn't exists in the catalog : must raise an error saying "No add-on with identifier foo-addon found in local or remote catalogs, check your add-on identifier"
   */
  def "[AM_INST_07] addon(.bat) install foo-addon:42 --force - not found"() {
    setup:
    ProcessResult process = launchAddonsManager([INSTALL_CMD, "unknown-foo-addon:42", FORCE_LONG_OPT])
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_ADDON_NOT_FOUND == process.exitValue()
    // Verify error message
    process.stdoutText.contains(
        "No add-on with identifier unknown-foo-addon found in local or remote catalogs, check your add-on identifier."
    )
  }

  /**
   * if foo-addon not already installed : must install the most recent 43-SNAPSHOT development version of the foo-addon
   */
  def "[AM_INST_08] addon(.bat) install foo-addon:43-SNAPSHOT --snapshots --force - not yet installed"() {
    setup:
    ProcessResult process = launchAddonsManager([INSTALL_CMD, "foo-addon:43-SNAPSHOT", SNAPSHOTS_LONG_OPT, FORCE_LONG_OPT])
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_OK == process.exitValue()
    // Verify that the add-on is correctly installed
    verifyAddonContentPresent(FOO_ADDON_43_SNAPSHOT_CONTENT)
    cleanup:
    // Uninstall it
    launchAddonsManagerSilently([UNINSTALL_CMD, "foo-addon"])
  }

  /**
   * if foo-addon is already installed : must enforce to reinstall the foo-addon with its more recent 43-SNAPSHOT development
   * version
   */
  def "[AM_INST_08] addon(.bat) install foo-addon:43-SNAPSHOT --snapshots --force - already installed"() {
    setup:
    // Install it first
    launchAddonsManagerSilently([INSTALL_CMD, "foo-addon", SNAPSHOTS_LONG_OPT])
    ProcessResult process = launchAddonsManager([INSTALL_CMD, "foo-addon:43-SNAPSHOT", SNAPSHOTS_LONG_OPT, FORCE_LONG_OPT])
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_OK == process.exitValue()
    // Verify that the add-on is correctly installed
    verifyAddonContentPresent(FOO_ADDON_43_SNAPSHOT_CONTENT)
    cleanup:
    // Uninstall it
    launchAddonsManagerSilently([UNINSTALL_CMD, "foo-addon"])
  }

  /**
   * if foo-addon doesn't exists in the catalog : must raise an error saying "No add-on with identifier foo-addon found in local or remote catalogs, check your add-on identifier"
   */
  def "[AM_INST_08] addon(.bat) install foo-addon:43-SNAPSHOT --snapshots --force - not found"() {
    setup:
    ProcessResult process = launchAddonsManager([INSTALL_CMD, "unknown-foo-addon:43-SNAPSHOT", SNAPSHOTS_LONG_OPT, FORCE_LONG_OPT])
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_ADDON_NOT_FOUND == process.exitValue()
    // Verify error message
    process.stdoutText.contains(
        "No add-on with identifier unknown-foo-addon found in local or remote catalogs, check your add-on identifier."
    )
  }

  /**
   * if foo-addon not already installed : must install the 43-RC1 version of the foo-addon
   */
  def "addon(.bat) install foo-addon:43-RC1 --unstable --force - not yet installed"() {
    setup:
    ProcessResult process = launchAddonsManager([INSTALL_CMD, "foo-addon:43-RC1", UNSTABLE_LONG_OPT, FORCE_LONG_OPT])
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_OK == process.exitValue()
    // Verify that the add-on is correctly installed
    verifyAddonContentPresent(FOO_ADDON_43_RC1_CONTENT)
    cleanup:
    // Uninstall it
    launchAddonsManagerSilently([UNINSTALL_CMD, "foo-addon"])
  }

  /**
   * if foo-addon is already installed : must enforce to reinstall the foo-addon with its 43-RC1 version
   */
  def "addon(.bat) install foo-addon:43-RC1 --unstable --force - already installed"() {
    setup:
    // Install it first
    launchAddonsManagerSilently([INSTALL_CMD, "foo-addon", UNSTABLE_LONG_OPT])
    ProcessResult process = launchAddonsManager([INSTALL_CMD, "foo-addon:43-RC1", UNSTABLE_LONG_OPT, FORCE_LONG_OPT])
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_OK == process.exitValue()
    // Verify that the add-on is correctly installed
    verifyAddonContentPresent(FOO_ADDON_43_RC1_CONTENT)
    cleanup:
    // Uninstall it
    launchAddonsManagerSilently([UNINSTALL_CMD, "foo-addon"])
  }

  /**
   * if foo-addon doesn't exists in the catalog : must raise an error saying "No add-on with identifier foo-addon found in local or remote catalogs, check your add-on identifier"
   */
  def "addon(.bat) install foo-addon:43-RC1 --unstable --force - not found"() {
    setup:
    ProcessResult process = launchAddonsManager([INSTALL_CMD, "unknown-foo-addon:43-RC1", UNSTABLE_LONG_OPT, FORCE_LONG_OPT])
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_ADDON_NOT_FOUND == process.exitValue()
    // Verify error message
    process.stdoutText.contains(
        "No add-on with identifier unknown-foo-addon found in local or remote catalogs, check your add-on identifier."
    )
  }

  /**
   * The add-ons manager does a compatibility check using the compatibility values prior to install an add-on. If the add-on is
   * not compatible, the installation interrupts with an error : "The add-on foo-addon:version is not compatible with your
   * version of eXo Platform. Use --no-compat to ignore this compatibility check and install anyway.
   */
  def "[AM_INST_09] The add-ons manager does a compatibility check using the compatibility values prior to install an add-on."() {
    setup:
    ProcessResult process = launchAddonsManager([INSTALL_CMD, "incompatible-foo-addon:42"])
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_INCOMPATIBILITY_ERROR == process.exitValue()
    // Verify error message
    process.stdoutText =~ "The add-on incompatible-foo-addon:42 is not compatible : .*. Use --no-compat to bypass this compatibility check and install anyway"
  }

  /**
   * installs foo-addon version 1.2 ignoring the compatiblity check
   */
  def "[AM_INST_10] addon(.bat) install foo-addon --no-compat"() {
    setup:
    ProcessResult process = launchAddonsManager([INSTALL_CMD, "incompatible-foo-addon:42", NO_COMPAT_LONG_OPT])
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_OK == process.exitValue()
    cleanup:
    // Uninstall it
    launchAddonsManagerSilently([UNINSTALL_CMD, "incompatible-foo-addon"])
  }

  /**
   * If installation requires to install an existing file, the default behaviour is to abort the installation with an error :
   * File XYZ already exists. Installation aborted. Use --conflict=skip|overwrite.
   * --conflict=skip will skip the conflicted files and log a warning for each one : File XYZ already exists. Skipped.
   * --conflict=overwrite will overwrite the conflicted files by the one contained in the add-on and log a warning for each one
   * : File XYZ already exists. Overwritten.
   */
  def "[AM_INST_11] addon(.bat) install foo-addon --conflict=fail"() {
    setup:
    // Let's create files existing in the addon
    new File(getPlatformSettings().librariesDirectory, "foo-addon-42.jar") << "TEST"
    new File(getPlatformSettings().webappsDirectory, "foo-addon-42.war") << "TEST"
    ProcessResult process = launchAddonsManager([INSTALL_CMD, "foo-addon:42", "${CONFLICT_LONG_OPT}=fail"])
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_UNKNOWN_ERROR == process.exitValue()
    // Verify output messages
    process.stdoutText =~ "File .* already exists. Installation aborted. Use --conflict=skip|overwrite."
    // Existing file shouldn't have been touched
    new File(getPlatformSettings().librariesDirectory, "foo-addon-42.jar").text == "TEST"
    new File(getPlatformSettings().webappsDirectory, "foo-addon-42.war").text == "TEST"
    cleanup:
    // Manually remove or additional file
    new File(getPlatformSettings().librariesDirectory, "foo-addon-42.jar").delete()
    new File(getPlatformSettings().webappsDirectory, "foo-addon-42.war").delete()
  }

  /**
   * If installation requires to install an existing file, the default behaviour is to abort the installation with an error :
   * File XYZ already exists. Installation aborted. Use --conflict=skip|overwrite.
   * --conflict=skip will skip the conflicted files and log a warning for each one : File XYZ already exists. Skipped.
   * --conflict=overwrite will overwrite the conflicted files by the one contained in the add-on and log a warning for each one
   * : File XYZ already exists. Overwritten.
   */
  def "[AM_INST_11] addon(.bat) install foo-addon --conflict=skip"() {
    setup:
    // Let's create a file existing in the addon
    new File(getPlatformSettings().librariesDirectory, "foo-addon-42.jar") << "TEST"
    ProcessResult process = launchAddonsManager([INSTALL_CMD, "foo-addon:42", "${CONFLICT_LONG_OPT}=skip"])
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_OK == process.exitValue()
    // Verify output messages
    process.stdoutText =~ "File .* already exists. Skipped."
    // Verify that the add-on is correctly installed
    verifyAddonContentPresent(FOO_ADDON_42_CONTENT)
    // It shouldn't have been touched
    new File(getPlatformSettings().librariesDirectory, "foo-addon-42.jar").text == "TEST"
    cleanup:
    // Uninstall it
    launchAddonsManagerSilently([UNINSTALL_CMD, "foo-addon"])
    // Manually remove or additional file
    new File(getPlatformSettings().librariesDirectory, "foo-addon-42.jar").delete()
  }

  /**
   * If installation requires to install an existing file, the default behaviour is to abort the installation with an error :
   * File XYZ already exists. Installation aborted. Use --conflict=skip|overwrite.
   * --conflict=skip will skip the conflicted files and log a warning for each one : File XYZ already exists. Skipped.
   * --conflict=overwrite will overwrite the conflicted files by the one contained in the add-on and log a warning for each one
   * : File XYZ already exists. Overwritten.
   */
  def "[AM_INST_11] addon(.bat) install foo-addon --conflict=overwrite"() {
    setup:
    // Let's create a file existing in the addon
    new File(getPlatformSettings().librariesDirectory, "foo-addon-42.jar") << "TEST"
    ProcessResult process = launchAddonsManager([INSTALL_CMD, "foo-addon:42", "${CONFLICT_LONG_OPT}=overwrite"])
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_OK == process.exitValue()
    // Verify output messages
    process.stdoutText =~ "File .* already exists. Overwritten."
    // Verify that the add-on is correctly installed
    verifyAddonContentPresent(FOO_ADDON_42_CONTENT)
    // It should have been replaced
    new File(getPlatformSettings().librariesDirectory, "foo-addon-42.jar").text != "TEST"
    cleanup:
    // Uninstall it
    launchAddonsManagerSilently([UNINSTALL_CMD, "foo-addon"])
    // Manually remove or additional file
    new File(getPlatformSettings().librariesDirectory, "foo-addon-42.jar").delete()
  }

  /**
   * At the end of a successful install command, the README of the add-on is displayed in the console if present.
   */
  def "[AM_INST_12] At the end of a successful install command, the README of the add-on is displayed in the console if present."() {
    setup:
    ProcessResult process = launchAddonsManager([INSTALL_CMD, "readme-addon:42"], ['', '', '', '', '', '', '', '', '', '', ''])
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_OK == process.exitValue()
    // Verify output messages
    process.stdoutText.contains("LOREM IPSUM")
    // Verify that the add-on is correctly installed
    verifyAddonContentPresent(README_ADDON_42_CONTENT)
    cleanup:
    // Uninstall it
    launchAddonsManagerSilently([UNINSTALL_CMD, "readme-addon"])
  }

  /**
   * if the add-on exists in the local archives, attempts to install it from archives without downloading the add-on. An informational message indicates it : Using foo-addon
   * archive from local archives directory
   * if the add-on does not exists in the local archives, fails with an error message : Failed to install : foo-addon:1.2 not found in local archives. Remove offline to download
   * it
   */
  def "[AM_INST_13] addon(.bat) install foo-addon:42 --offline - already present"() {
    setup:
    // Install it a first time
    launchAddonsManagerSilently([INSTALL_CMD, "foo-addon:42"])
    // remove it
    launchAddonsManagerSilently([UNINSTALL_CMD, "foo-addon"])
    // Let's install it a second time
    ProcessResult process = launchAddonsManager([INSTALL_CMD, "foo-addon:42", "--offline"])
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_OK == process.exitValue()
    // Verify the output
    process.stdoutText.contains("Using foo-addon:42 archive from local archives directory")
    // Verify that the add-on is correctly installed
    verifyAddonContentPresent(FOO_ADDON_42_CONTENT)
    cleanup:
    // Uninstall it
    launchAddonsManagerSilently([UNINSTALL_CMD, "foo-addon"])
  }

  /**
   * if the add-on exists in the local archives, attempts to install it from archives without downloading the add-on. An informational message indicates it : Using foo-addon
   * archive from local archives directory
   * if the add-on does not exists in the local archives, fails with an error message : Failed to install : foo-addon:1.2 not found in local archives. Remove offline to download
   * it
   */
  def "[AM_INST_13] addon(.bat) install foo-addon:42 --offline - not present"() {
    setup:
    // Let's load the catalog
    launchAddonsManagerSilently([LIST_CMD])
    // Let's install it a second time
    ProcessResult process = launchAddonsManager([INSTALL_CMD, "foo-addon:42","--offline"])
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_UNKNOWN_ERROR == process.exitValue()
    // Verify the output
    process.stdoutText.contains("foo-addon:42 not found in local archives. Remove --offline to download it")
  }

  /**
   * Properties files are installed to the right location
   */
  def "[AM_STRUCT_07] addon(.bat) install properties-files-addon"() {
    setup:
    ProcessResult process = launchAddonsManager([INSTALL_CMD, "properties-files-addon:42"])
    expect:
    // Install it
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_OK == process.exitValue()
    // Verify that the add-on is correctly installed
    verifyAddonContentPresent(PROP_FILES_ADDON_42_CONTENT)
    cleanup:
    // Uninstall it
    AddonsManagerConstants.RETURN_CODE_OK == launchAddonsManager([UNINSTALL_CMD, "properties-files-addon"]).exitValue()
  }

  /**
   * Other files and folders located at the root of the add-on archive are copied as-is under $PLATFORM_HOME
   */
  def "[AM_STRUCT_04] addon(.bat) install other-files-addon"() {
    setup:
    ProcessResult process = launchAddonsManager([INSTALL_CMD, "other-files-addon:42"])
    expect:
    // Install it
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_OK == process.exitValue()
    // Verify that the add-on is correctly installed
    verifyAddonContentPresent(OTHER_FILES_ADDON_42_CONTENT)
    cleanup:
    // Uninstall it
    AddonsManagerConstants.RETURN_CODE_OK == launchAddonsManager([UNINSTALL_CMD, "other-files-addon"]).exitValue()
  }

  /**
   * [LICENSE_01] Download and display license if mustAcceptLicenseTerms=true
   * [LICENSE_03] [LICENSE_04] interactive validation of license
   */
  def "[LICENSE_01] [LICENSE_03] [LICENSE_04] Download and display license if mustAcceptLicenseTerms=true. The user refuses it."() {
    setup:
    ProcessResult process = launchAddonsManager([INSTALL_CMD, "license-addon:42"], ['no\n'])
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_LICENSE_NOT_ACCEPTED == process.exitValue()
    // Verify output messages
    process.stdoutText.contains("fakeLicenseV1")
    // Verify that the add-on isn't installed
    verifyAddonContentNotPresent(FOO_ADDON_42_CONTENT)
  }

  /**
   * [LICENSE_01] Download and display license if mustAcceptLicenseTerms=true
   * [LICENSE_03] [LICENSE_04] interactive validation of license
   */
  def "[LICENSE_01] [LICENSE_03] [LICENSE_04] Download and display license if mustAcceptLicenseTerms=true. The user accepts it."() {
    setup:
    ProcessResult process = launchAddonsManager([INSTALL_CMD, "license-addon:42"], ['yes\n'])
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_OK == process.exitValue()
    // Verify output messages
    process.stdoutText.contains("fakeLicenseV1")
    // Verify that the add-on is correctly installed
    verifyAddonContentPresent(FOO_ADDON_42_CONTENT)
    cleanup:
    // Uninstall it
    launchAddonsManagerSilently([UNINSTALL_CMD, "license-addon"])
  }

  /**
   * [LICENSE_05] Don't prompt to validate a license already accepted
   */
  def "[LICENSE_05] Don't prompt to validate a license already accepted."() {
    setup:
    // Install it a first time
    launchAddonsManagerSilently([INSTALL_CMD, "license-addon:42"], ['yes\n'])
    // Remove it
    launchAddonsManagerSilently([UNINSTALL_CMD, "license-addon"])
    ProcessResult process = launchAddonsManager([INSTALL_CMD, "license-addon:42"])
    expect:
    // Reinstall it and verify return code
    AddonsManagerConstants.RETURN_CODE_OK == process.exitValue()
    // Verify output messages
    !process.stdoutText.contains("fakeLicenseV1")
    // Verify that the add-on is correctly installed
    verifyAddonContentPresent(FOO_ADDON_42_CONTENT)
    cleanup:
    // Uninstall it
    launchAddonsManagerSilently([UNINSTALL_CMD, "license-addon"])
  }

}
