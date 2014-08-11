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
class AddonsManagerUninstallIT extends IntegrationTestsSpecification {

  def cleanup() {
    // After each test we remove the content of the add-ons directory to be safe
    getEnvironmentSettings().getAddonsDirectory().deleteDir()
  }

  /**
   * if foo-addon not already installed : must raise an error saying "The add-on foo-addon was not installed [KO]"
   */
  def "[AM_UNINST_01] addon(.bat) uninstall foo-addon - not already installed"() {
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_ADDON_NOT_INSTALLED == launchAddonsManager(
        [UNINSTALL_CMD, "foo-addon"]).exitValue()
  }

  /**
   * if foo-addon is already installed : must uninstall the add-on whatever the installed version is stable or development
   */
  def "[AM_UNINST_01] addon(.bat) uninstall foo-addon - already installed"() {
    setup:
    launchAddonsManager([INSTALL_CMD, "foo-addon:42"])
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_OK == launchAddonsManager([UNINSTALL_CMD, "foo-addon"]).exitValue()
    // Verify that the add-on is not installed
    verifyAddonContentNotPresent(FOO_ADDON_42_CONTENT)
  }

  /**
   * At uninstall, files that were already existing are not removed unless they were overwritten with (--conflict=overwrite in
   * which case, the previous version of the file is restored and the following warning message is logged : File XYZ has been
   * restored
   */
  def "[AM_UNINST_02] Removal of add-ons installed with --conflict=overwrite"() {
    setup:
    // Let's create a file existing in the addon
    new File(getPlatformSettings().librariesDirectory, "foo-addon-42.jar") << "TEST"
    launchAddonsManager([INSTALL_CMD, "foo-addon:42", "${CONFLICT_LONG_OPT}=overwrite"])
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_OK == launchAddonsManager([UNINSTALL_CMD, "foo-addon"]).exitValue()
    // Verify that the add-on is remove installed
    !new File(getPlatformSettings().webappsDirectory, "foo-addon-42.war").exists()
    // But the replaced file should be restored
    new File(getPlatformSettings().librariesDirectory, "foo-addon-42.jar").exists()
    new File(getPlatformSettings().librariesDirectory, "foo-addon-42.jar").text == "TEST"
    cleanup:
    // Manually remove the additional file
    new File(getPlatformSettings().librariesDirectory, "foo-addon-42.jar").delete()
  }

}
