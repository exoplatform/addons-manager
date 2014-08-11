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
   * if foo-addon not already installed : must install the most recent stable version of foo-addon
   */
  def "[AM_INST_01] addon(.bat) install foo-addon - not yet installed"() {
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_OK == launchAddonsManager([INSTALL_CMD, "foo-addon"]).exitValue()
    // Verify that the add-on is correctly installed
    verifyAddonContentPresent(FOO_ADDON_42_CONTENT)
    cleanup:
    // Uninstall it
    launchAddonsManager([UNINSTALL_CMD, "foo-addon"])
  }

  /**
   * if foo-addon is already installed : must not install anything
   */
  def "[AM_INST_01] addon(.bat) install foo-addon - already installed"() {
    setup:
    // Install it first
    launchAddonsManager([INSTALL_CMD, "foo-addon"])
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_ADDON_ALREADY_INSTALLED == launchAddonsManager(
        [INSTALL_CMD, "foo-addon"]).exitValue()
    cleanup:
    // Uninstall it
    launchAddonsManager([UNINSTALL_CMD, "foo-addon"])
  }

  /**
   * if foo-addon doesn't exists in the catalog : must raise an error saying "The add-on foo-addon doesn't exists in the remote
   * catalog, check your add-on name [KO]"
   */
  def "[AM_INST_01] addon(.bat) install foo-addon - not found"() {
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_ADDON_NOT_FOUND == launchAddonsManager(
        [INSTALL_CMD, "unknown-foo-addon"]).exitValue()
  }

  /**
   * if foo-addon not already installed : must install the most recent development version of the foo-addon
   */
  def "[AM_INST_02] addon(.bat) install foo-addon --snapshots - not yet installed"() {
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_OK == launchAddonsManager([INSTALL_CMD, "foo-addon", SNAPSHOTS_LONG_OPT]).exitValue()
    // Verify that the add-on is correctly installed
    verifyAddonContentPresent(FOO_ADDON_43_SNAPSHOT_CONTENT)
    cleanup:
    // Uninstall it
    launchAddonsManager([UNINSTALL_CMD, "foo-addon"])
  }

  /**
   * if foo-addon is already installed : must not install anything
   * TODO : if the last stable version is more recent than the most recent development version, we must install the stable version
   */
  def "[AM_INST_02] addon(.bat) install foo-addon --snapshots - already installed"() {
    setup:
    // Install it first
    launchAddonsManager([INSTALL_CMD, "foo-addon", SNAPSHOTS_LONG_OPT])
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_ADDON_ALREADY_INSTALLED == launchAddonsManager(
        [INSTALL_CMD, "foo-addon", SNAPSHOTS_LONG_OPT]).exitValue()
    cleanup:
    // Uninstall it
    launchAddonsManager([UNINSTALL_CMD, "foo-addon"])
  }

  /**
   * if foo-addon doesn't exists in the catalog : must raise an error saying "The add-on foo-addon doesn't exists in the remote
   * catalog, check your add-on name [KO]"
   */
  def "[AM_INST_02] addon(.bat) install foo-addon --snapshots - not found"() {
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_ADDON_NOT_FOUND == launchAddonsManager(
        [INSTALL_CMD, "unknown-foo-addon", SNAPSHOTS_LONG_OPT]).exitValue()
  }

  /**
   * if foo-addon not already installed : must install the most recent unstable version of the foo-addon
   */
  def "addon(.bat) install foo-addon --unstable - not yet installed"() {
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_OK == launchAddonsManager([INSTALL_CMD, "foo-addon", UNSTABLE_LONG_OPT]).exitValue()
    // Verify that the add-on is correctly installed
    verifyAddonContentPresent(FOO_ADDON_43_RC1_CONTENT)
    cleanup:
    // Uninstall it
    launchAddonsManager([UNINSTALL_CMD, "foo-addon"])
  }

  /**
   * if foo-addon is already installed : must not install anything
   * TODO : if the last stable version is more recent than the most recent unstable version, we must install the stable version
   */
  def "addon(.bat) install foo-addon --unstable - already installed"() {
    setup:
    // Install it first
    launchAddonsManager([INSTALL_CMD, "foo-addon", UNSTABLE_LONG_OPT])
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_ADDON_ALREADY_INSTALLED == launchAddonsManager(
        [INSTALL_CMD, "foo-addon", UNSTABLE_LONG_OPT]).exitValue()
    cleanup:
    // Uninstall it
    launchAddonsManager([UNINSTALL_CMD, "foo-addon"])
  }

  /**
   * if foo-addon doesn't exists in the catalog : must raise an error saying "The add-on foo-addon doesn't exists in the remote
   * catalog, check your add-on name [KO]"
   */
  def "addon(.bat) install foo-addon --unstable - not found"() {
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_ADDON_NOT_FOUND == launchAddonsManager(
        [INSTALL_CMD, "unknown-foo-addon", UNSTABLE_LONG_OPT]).exitValue()
  }

  /**
   * if foo-addon not already installed : must install the most recent stable version of the foo-addon
   */
  def "[AM_INST_03] addon(.bat) install foo-addon --force - not yet installed"() {
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_OK == launchAddonsManager([INSTALL_CMD, "foo-addon", FORCE_LONG_OPT]).exitValue()
    // Verify that the add-on is correctly installed
    verifyAddonContentPresent(FOO_ADDON_42_CONTENT)
    cleanup:
    // Uninstall it
    launchAddonsManager([UNINSTALL_CMD, "foo-addon"])
  }

  /**
   * if foo-addon is already installed : must enforce to reinstall the foo-addon
   */
  def "[AM_INST_03] addon(.bat) install foo-addon --force - already installed"() {
    setup:
    // Install it first
    launchAddonsManager([INSTALL_CMD, "foo-addon"])
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_OK == launchAddonsManager([INSTALL_CMD, "foo-addon", FORCE_LONG_OPT]).exitValue()
    // Verify that the add-on is correctly installed
    verifyAddonContentPresent(FOO_ADDON_42_CONTENT)
    cleanup:
    // Uninstall it
    launchAddonsManager([UNINSTALL_CMD, "foo-addon"])
  }

  /**
   * if foo-addon doesn't exists in the catalog : must raise an error saying "The add-on foo-addon doesn't exists in the remote
   * catalog, check your add-on name [KO]"
   */
  def "[AM_INST_03] addon(.bat) install foo-addon --force - not found"() {
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_ADDON_NOT_FOUND == launchAddonsManager(
        [INSTALL_CMD, "unknown-foo-addon", FORCE_LONG_OPT]).exitValue()
  }

  /**
   * if foo-addon not already installed : must install the most recent development version of the foo-addon
   */
  def "[AM_INST_04] addon(.bat) install foo-addon --snapshots --force - not yet installed"() {
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_OK == launchAddonsManager(
        [INSTALL_CMD, "foo-addon", SNAPSHOTS_LONG_OPT, FORCE_LONG_OPT]).exitValue()
    // Verify that the add-on is correctly installed
    verifyAddonContentPresent(FOO_ADDON_43_SNAPSHOT_CONTENT)
    cleanup:
    // Uninstall it
    launchAddonsManager([UNINSTALL_CMD, "foo-addon"])
  }

  /**
   * if foo-addon is already installed : must enforce to reinstall the foo-addon with its more recent development version
   * TODO : if the last stable version is more recent than the most recent development version, we must install / reinstall the stable
   * version
   */
  def "[AM_INST_04] addon(.bat) install foo-addon --snapshots --force - already installed"() {
    setup:
    // Install it first
    launchAddonsManager([INSTALL_CMD, "foo-addon", SNAPSHOTS_LONG_OPT])
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_OK == launchAddonsManager(
        [INSTALL_CMD, "foo-addon", SNAPSHOTS_LONG_OPT, FORCE_LONG_OPT]).exitValue()
    // Verify that the add-on is correctly installed
    verifyAddonContentPresent(FOO_ADDON_43_SNAPSHOT_CONTENT)
    cleanup:
    // Uninstall it
    launchAddonsManager([UNINSTALL_CMD, "foo-addon"])
  }

  /**
   * if foo-addon doesn't exists in the catalog : must raise an error saying "The add-on foo-addon doesn't exists in the remote
   * catalog, check your add-on name [KO]"
   */
  def "[AM_INST_04] addon(.bat) install foo-addon --snapshots --force - not found"() {
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_ADDON_NOT_FOUND == launchAddonsManager(
        [INSTALL_CMD, "unknown-foo-addon", SNAPSHOTS_LONG_OPT, FORCE_LONG_OPT]).exitValue()
  }

  /**
   * if foo-addon not already installed : must install the most recent unstable version of the foo-addon
   */
  def "addon(.bat) install foo-addon --unstable --force - not yet installed"() {
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_OK == launchAddonsManager(
        [INSTALL_CMD, "foo-addon", UNSTABLE_LONG_OPT, FORCE_LONG_OPT]).exitValue()
    // Verify that the add-on is correctly installed
    verifyAddonContentPresent(FOO_ADDON_43_RC1_CONTENT)
    cleanup:
    // Uninstall it
    launchAddonsManager([UNINSTALL_CMD, "foo-addon"])
  }

  /**
   * if foo-addon is already installed : must enforce to reinstall the foo-addon with its more recent development version
   * TODO : if the last stable version is more recent than the most recent unstable version, we must install / reinstall the stable
   * version
   */
  def "addon(.bat) install foo-addon --unstable --force - already installed"() {
    setup:
    // Install it first
    launchAddonsManager([INSTALL_CMD, "foo-addon", UNSTABLE_LONG_OPT])
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_OK == launchAddonsManager(
        [INSTALL_CMD, "foo-addon", UNSTABLE_LONG_OPT, FORCE_LONG_OPT]).exitValue()
    // Verify that the add-on is correctly installed
    verifyAddonContentPresent(FOO_ADDON_43_RC1_CONTENT)
    cleanup:
    // Uninstall it
    launchAddonsManager([UNINSTALL_CMD, "foo-addon"])
  }

  /**
   * if foo-addon doesn't exists in the catalog : must raise an error saying "The add-on foo-addon doesn't exists in the remote
   * catalog, check your add-on name [KO]"
   */
  def "addon(.bat) install foo-addon --unstable --force - not found"() {
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_ADDON_NOT_FOUND == launchAddonsManager(
        [INSTALL_CMD, "unknown-foo-addon", UNSTABLE_LONG_OPT, FORCE_LONG_OPT]).exitValue()
  }

  /**
   * if foo-addon not already installed : must install the version 42 of the foo-addon
   */
  def "[AM_INST_05] addon(.bat) install foo-addon:42 - not yet installed"() {
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_OK == launchAddonsManager([INSTALL_CMD, "foo-addon:42"]).exitValue()
    // Verify that the add-on is correctly installed
    verifyAddonContentPresent(FOO_ADDON_42_CONTENT)
    cleanup:
    // Uninstall it
    launchAddonsManager([UNINSTALL_CMD, "foo-addon"])
  }

  /**
   * if foo-addon is already installed : must not install anything
   */
  def "[AM_INST_05] addon(.bat) install foo-addon:42 - already installed"() {
    setup:
    // Install it first
    launchAddonsManager([INSTALL_CMD, "foo-addon"])
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_ADDON_ALREADY_INSTALLED == launchAddonsManager(
        [INSTALL_CMD, "foo-addon:42"]).exitValue()
    cleanup:
    // Uninstall it
    launchAddonsManager([UNINSTALL_CMD, "foo-addon"])
  }

  /**
   * if foo-addon doesn't exists in the catalog : must raise an error saying "The add-on foo-addon doesn't exists in the remote
   * catalog, check your add-on name [KO]"
   */
  def "[AM_INST_05] addon(.bat) install foo-addon - not found"() {
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_ADDON_NOT_FOUND == launchAddonsManager(
        [INSTALL_CMD, "unknown-foo-addon:42"]).exitValue()
  }

  /**
   * if foo-addon not already installed : must install the last 42 snapshot version available of the foo-addon
   */
  def "[AM_INST_06] addon(.bat) install foo-addon:43-SNAPSHOT --snapshots - not yet installed"() {
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_OK == launchAddonsManager(
        [INSTALL_CMD, "foo-addon:43-SNAPSHOT", SNAPSHOTS_LONG_OPT]).exitValue()
    // Verify that the add-on is correctly installed
    verifyAddonContentPresent(FOO_ADDON_43_SNAPSHOT_CONTENT)
    cleanup:
    // Uninstall it
    launchAddonsManager([UNINSTALL_CMD, "foo-addon"])
  }

  /**
   * if foo-addon is already installed : must not install anything
   */
  def "[AM_INST_06] addon(.bat) install foo-addon:43-SNAPSHOT --snapshots - already installed"() {
    setup:
    // Install it first
    launchAddonsManager([INSTALL_CMD, "foo-addon:43-SNAPSHOT", SNAPSHOTS_LONG_OPT])
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_ADDON_ALREADY_INSTALLED == launchAddonsManager(
        [INSTALL_CMD, "foo-addon", SNAPSHOTS_LONG_OPT]).exitValue()
    cleanup:
    // Uninstall it
    launchAddonsManager([UNINSTALL_CMD, "foo-addon"])
  }

  /**
   * if foo-addon doesn't exists in the catalog : must raise an error saying "The add-on foo-addon doesn't exists in the remote
   * catalog, check your add-on name [KO]"
   */
  def "[AM_INST_06] addon(.bat) install foo-addon:43-SNAPSHOT --snapshots - not found"() {
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_ADDON_NOT_FOUND == launchAddonsManager(
        [INSTALL_CMD, "unknown-foo-addon:43-SNAPSHOT", SNAPSHOTS_LONG_OPT]).exitValue()
  }

  /**
   * if foo-addon not already installed : must install the 43-RC1 version of the foo-addon
   */
  def "addon(.bat) install foo-addon:43-RC1 --unstable - not yet installed"() {
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_OK == launchAddonsManager(
        [INSTALL_CMD, "foo-addon:43-RC1", UNSTABLE_LONG_OPT]).exitValue()
    // Verify that the add-on is correctly installed
    verifyAddonContentPresent(FOO_ADDON_43_RC1_CONTENT)
    cleanup:
    // Uninstall it
    launchAddonsManager([UNINSTALL_CMD, "foo-addon"])
  }

  /**
   * if foo-addon is already installed : must not install anything
   */
  def "addon(.bat) install foo-addon:43-RC1 --unstable - already installed"() {
    setup:
    // Install it first
    launchAddonsManager([INSTALL_CMD, "foo-addon:43-RC1", UNSTABLE_LONG_OPT])
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_ADDON_ALREADY_INSTALLED == launchAddonsManager(
        [INSTALL_CMD, "foo-addon", UNSTABLE_LONG_OPT]).exitValue()
    cleanup:
    // Uninstall it
    launchAddonsManager([UNINSTALL_CMD, "foo-addon"])
  }

  /**
   * if foo-addon doesn't exists in the catalog : must raise an error saying "The add-on foo-addon doesn't exists in the remote
   * catalog, check your add-on name [KO]"
   */
  def "addon(.bat) install foo-addon:43-RC1 --unstable - not found"() {
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_ADDON_NOT_FOUND == launchAddonsManager(
        [INSTALL_CMD, "unknown-foo-addon:43-RC1", UNSTABLE_LONG_OPT]).exitValue()
  }

  /**
   * if foo-addon not already installed : must install the version 42 of the foo-addon
   */
  def "[AM_INST_07] addon(.bat) install foo-addon:42 --force - not yet installed"() {
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_OK == launchAddonsManager([INSTALL_CMD, "foo-addon:42", FORCE_LONG_OPT]).exitValue()
    // Verify that the add-on is correctly installed
    verifyAddonContentPresent(FOO_ADDON_42_CONTENT)
    cleanup:
    // Uninstall it
    launchAddonsManager([UNINSTALL_CMD, "foo-addon"])
  }

  /**
   * if foo-addon is already installed : must enforce to reinstall the version 42 of the foo-addon
   */
  def "[AM_INST_07] addon(.bat) install foo-addon:42 --force - already installed"() {
    setup:
    // Install it first
    launchAddonsManager([INSTALL_CMD, "foo-addon"])
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_OK == launchAddonsManager([INSTALL_CMD, "foo-addon:42", FORCE_LONG_OPT]).exitValue()
    // Verify that the add-on is correctly installed
    verifyAddonContentPresent(FOO_ADDON_42_CONTENT)
    cleanup:
    // Uninstall it
    launchAddonsManager([UNINSTALL_CMD, "foo-addon"])
  }

  /**
   * if foo-addon doesn't exists in the catalog : must raise an error saying "The add-on foo-addon doesn't exists in the remote
   * catalog, check your add-on name [KO]"
   */
  def "[AM_INST_07] addon(.bat) install foo-addon:42 --force - not found"() {
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_ADDON_NOT_FOUND == launchAddonsManager(
        [INSTALL_CMD, "unknown-foo-addon:42", FORCE_LONG_OPT]).exitValue()
  }

  /**
   * if foo-addon not already installed : must install the most recent 43-SNAPSHOT development version of the foo-addon
   */
  def "[AM_INST_08] addon(.bat) install foo-addon:43-SNAPSHOT --snapshots --force - not yet installed"() {
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_OK == launchAddonsManager(
        [INSTALL_CMD, "foo-addon:43-SNAPSHOT", SNAPSHOTS_LONG_OPT, FORCE_LONG_OPT]).exitValue()
    // Verify that the add-on is correctly installed
    verifyAddonContentPresent(FOO_ADDON_43_SNAPSHOT_CONTENT)
    cleanup:
    // Uninstall it
    launchAddonsManager([UNINSTALL_CMD, "foo-addon"])
  }

  /**
   * if foo-addon is already installed : must enforce to reinstall the foo-addon with its more recent 43-SNAPSHOT development
   * version
   */
  def "[AM_INST_08] addon(.bat) install foo-addon:43-SNAPSHOT --snapshots --force - already installed"() {
    setup:
    // Install it first
    launchAddonsManager([INSTALL_CMD, "foo-addon", SNAPSHOTS_LONG_OPT])
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_OK == launchAddonsManager(
        [INSTALL_CMD, "foo-addon:43-SNAPSHOT", SNAPSHOTS_LONG_OPT, FORCE_LONG_OPT]).exitValue()
    // Verify that the add-on is correctly installed
    verifyAddonContentPresent(FOO_ADDON_43_SNAPSHOT_CONTENT)
    cleanup:
    // Uninstall it
    launchAddonsManager([UNINSTALL_CMD, "foo-addon"])
  }

  /**
   * if foo-addon doesn't exists in the catalog : must raise an error saying "The add-on foo-addon doesn't exists in the remote
   * catalog, check your add-on name [KO]"
   */
  def "[AM_INST_08] addon(.bat) install foo-addon:43-SNAPSHOT --snapshots --force - not found"() {
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_ADDON_NOT_FOUND == launchAddonsManager(
        [INSTALL_CMD, "unknown-foo-addon:43-SNAPSHOT", SNAPSHOTS_LONG_OPT, FORCE_LONG_OPT]).exitValue()
  }

  /**
   * if foo-addon not already installed : must install the 43-RC1 version of the foo-addon
   */
  def "addon(.bat) install foo-addon:43-RC1 --unstable --force - not yet installed"() {
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_OK == launchAddonsManager(
        [INSTALL_CMD, "foo-addon:43-RC1", UNSTABLE_LONG_OPT, FORCE_LONG_OPT]).exitValue()
    // Verify that the add-on is correctly installed
    verifyAddonContentPresent(FOO_ADDON_43_RC1_CONTENT)
    cleanup:
    // Uninstall it
    launchAddonsManager([UNINSTALL_CMD, "foo-addon"])
  }

  /**
   * if foo-addon is already installed : must enforce to reinstall the foo-addon with its 43-RC1 version
   */
  def "addon(.bat) install foo-addon:43-RC1 --unstable --force - already installed"() {
    setup:
    // Install it first
    launchAddonsManager([INSTALL_CMD, "foo-addon", UNSTABLE_LONG_OPT])
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_OK == launchAddonsManager(
        [INSTALL_CMD, "foo-addon:43-RC1", UNSTABLE_LONG_OPT, FORCE_LONG_OPT]).exitValue()
    // Verify that the add-on is correctly installed
    verifyAddonContentPresent(FOO_ADDON_43_RC1_CONTENT)
    cleanup:
    // Uninstall it
    launchAddonsManager([UNINSTALL_CMD, "foo-addon"])
  }

  /**
   * if foo-addon doesn't exists in the catalog : must raise an error saying "The add-on foo-addon doesn't exists in the remote
   * catalog, check your add-on name [KO]"
   */
  def "addon(.bat) install foo-addon:43-RC1 --unstable --force - not found"() {
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_ADDON_NOT_FOUND == launchAddonsManager(
        [INSTALL_CMD, "unknown-foo-addon:43-RC1", UNSTABLE_LONG_OPT, FORCE_LONG_OPT]).exitValue()
  }

  /**
   * The add-ons manager does a compatibility check using the compatibility values prior to install an add-on. If the add-on is
   * not compatible, the installation interrupts with an error : "The add-on foo-addon:version is not compatible with your
   * version of eXo Platform. Use --no-compat to ignore this compatibility check and install anyway.
   */
  def "[AM_INST_09] The add-ons manager does a compatibility check using the compatibility values prior to install an add-on."() {
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_INCOMPATIBILITY_ERROR == launchAddonsManager(
        [INSTALL_CMD, "incompatible-foo-addon:42"]).exitValue()
  }

  /**
   * installs foo-addon version 1.2 ignoring the compatiblity check
   */
  def "[AM_INST_10] addon(.bat) install foo-addon --no-compat"() {
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_OK == launchAddonsManager(
        [INSTALL_CMD, "incompatible-foo-addon:42", NO_COMPAT_LONG_OPT]).exitValue()
    cleanup:
    // Uninstall it
    launchAddonsManager([UNINSTALL_CMD, "incompatible-foo-addon"])
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
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_UNKNOWN_ERROR == launchAddonsManager(
        [INSTALL_CMD, "foo-addon:42", "${CONFLICT_LONG_OPT}=fail"]).exitValue()
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
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_OK == launchAddonsManager(
        [INSTALL_CMD, "foo-addon:42", "${CONFLICT_LONG_OPT}=skip"]).exitValue()
    // Verify that the add-on is correctly installed
    verifyAddonContentPresent(FOO_ADDON_42_CONTENT)
    // It shouldn't have been touched
    new File(getPlatformSettings().librariesDirectory, "foo-addon-42.jar").text == "TEST"
    cleanup:
    // Uninstall it
    launchAddonsManager([UNINSTALL_CMD, "foo-addon"])
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
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_OK == launchAddonsManager(
        [INSTALL_CMD, "foo-addon:42", "${CONFLICT_LONG_OPT}=overwrite"]).exitValue()
    // Verify that the add-on is correctly installed
    verifyAddonContentPresent(FOO_ADDON_42_CONTENT)
    // It should have been replaced
    new File(getPlatformSettings().librariesDirectory, "foo-addon-42.jar").text != "TEST"
    cleanup:
    // Uninstall it
    launchAddonsManager([UNINSTALL_CMD, "foo-addon"])
    // Manually remove or additional file
    new File(getPlatformSettings().librariesDirectory, "foo-addon-42.jar").delete()
  }

  /**
   * At the end of a successful install command, the README of the add-on is displayed in the console if present.
   */
  def "[AM_INST_12] At the end of a successful install command, the README of the add-on is displayed in the console if present."() {
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_OK == launchAddonsManager(
        [INSTALL_CMD, "readme-addon:42"], ['', '', '', '', '', '', '', '', '', '', '']).exitValue()
    // Verify that the add-on is correctly installed
    verifyAddonContentPresent(README_ADDON_42_CONTENT)
    cleanup:
    // Uninstall it
    launchAddonsManager([UNINSTALL_CMD, "readme-addon"])
  }

  /**
   * Other files and folders located at the root of the add-on archive are copied as-is under $PLATFORM_HOME
   */
  def "[AM_STRUCT_04] addon(.bat) install other-files-addon"() {
    expect:
    // Install it
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_OK == launchAddonsManager([INSTALL_CMD, "other-files-addon:42"]).exitValue()
    // Verify that the add-on is correctly installed
    verifyAddonContentPresent(OTHER_FILES_ADDON_42_CONTENT)
    // Uninstall it
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_OK == launchAddonsManager([UNINSTALL_CMD, "other-files-addon"]).exitValue()
    // Verify that the add-on is correctly installed
    verifyAddonContentNotPresent(OTHER_FILES_ADDON_42_CONTENT)
  }

  /**
   * [LICENSE_01] Download and display license if mustAcceptLicenseTerms=true
   * [LICENSE_03] [LICENSE_04] interactive validation of license
   */
  def "[LICENSE_01] [LICENSE_03] [LICENSE_04] Download and display license if mustAcceptLicenseTerms=true. The user refuses it."() {
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_LICENSE_NOT_ACCEPTED == launchAddonsManager(
        [INSTALL_CMD, "license-addon:42"],
        ['no\n']).exitValue()
    // Verify that the add-on isn't installed
    verifyAddonContentNotPresent(FOO_ADDON_42_CONTENT)
  }

  /**
   * [LICENSE_01] Download and display license if mustAcceptLicenseTerms=true
   * [LICENSE_03] [LICENSE_04] interactive validation of license
   */
  def "[LICENSE_01] [LICENSE_03] [LICENSE_04] Download and display license if mustAcceptLicenseTerms=true. The user accepts it."() {
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_OK == launchAddonsManager(
        [INSTALL_CMD, "license-addon:42"],
        ['yes\n']).exitValue()
    // Verify that the add-on is correctly installed
    verifyAddonContentPresent(FOO_ADDON_42_CONTENT)
    cleanup:
    // Uninstall it
    launchAddonsManager([UNINSTALL_CMD, "license-addon"])
  }

  /**
   * [LICENSE_05] Don't prompt to validate a license already accepted
   */
  def "[LICENSE_05] Don't prompt to validate a license already accepted."() {
    expect:
    // Install it a first time
    launchAddonsManager([INSTALL_CMD, "license-addon:42"], ['yes\n'])
    // Remove it
    launchAddonsManager([UNINSTALL_CMD, "license-addon"])
    // Reinstall it and verify return code
    AddonsManagerConstants.RETURN_CODE_OK == launchAddonsManager([INSTALL_CMD, "license-addon:42"]).exitValue()
    // Verify that the add-on is correctly installed
    verifyAddonContentPresent(FOO_ADDON_42_CONTENT)
    cleanup:
    // Uninstall it
    launchAddonsManager([UNINSTALL_CMD, "license-addon"])
  }

}
