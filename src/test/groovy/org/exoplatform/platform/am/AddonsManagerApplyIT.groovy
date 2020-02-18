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

import org.exoplatform.platform.am.AddonsManager
import org.exoplatform.platform.am.AddonsManagerConstants
import org.exoplatform.platform.am.IntegrationTestsSpecification
import org.exoplatform.platform.am.cli.CommandLineParameters
import spock.lang.Subject

import static org.exoplatform.platform.am.cli.CommandLineParameters.*
import static org.exoplatform.platform.am.cli.CommandLineParameters.APPLY_CMD
import static org.exoplatform.platform.am.cli.CommandLineParameters.CRCCHECK_OPT

/**
 * @author Houssem Ben Ali <hbenali@exoplatform.com>
 */
@Subject(AddonsManager)
class AddonsManagerApplyIT extends IntegrationTestsSpecification {

  def cleanup() {
    // After each test we remove the content of the add-ons directory to be safe
    getEnvironmentSettings().getAddonsDirectory().deleteDir()
  }

  /**
   * if patch-5.2:1 is not already installed : must install patch-5.2-1
   */
  def "[AM_APPLY_01] addon(.bat) apply patch-5.2-1.zip.enc public.exokey - not yet installed"() {
    setup:
    ProcessResult process = launchAddonsManager([CommandLineParameters.APPLY_CMD, "patch-5.2-1.zip.enc","public.exokey"])
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_OK == process.exitValue()
    // Verify that the add-on is correctly installed
    verifyAddonContentPresent(IntegrationTestsSpecification.PROP_FILES_PATCH_52_CONTENT)
    cleanup:
    // Uninstall it
    launchAddonsManagerSilently([CommandLineParameters.UNINSTALL_CMD, "patch-5.2:1"])
  }

  /**
   * if patch-5.2:1 is already installed : must not install anything
   */
  def "[AM_APPLY_02] addon(.bat) apply patch-5.2-1.zip.enc public.exokey - already installed"() {
    setup:
    // Install it first
    launchAddonsManagerSilently([CommandLineParameters.APPLY_CMD,  "patch-5.2-1.zip.enc","public.exokey"])
    ProcessResult process = launchAddonsManager([CommandLineParameters.APPLY_CMD,  "patch-5.2-1.zip.enc","public.exokey"])
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_ADDON_ALREADY_INSTALLED == process.exitValue()
    cleanup:
    // Uninstall it
    launchAddonsManagerSilently([CommandLineParameters.UNINSTALL_CMD, "patch-5.2:1"])
  }

  /**
   * if patch-5.2:1 is already installed : must not install anything
   * TODO : if the last stable version is more recent than the most recent development version, we must install the stable version
   */
  def "[AM_APPLY_03] addon(.bat) apply patch-5.2-1.zip.enc public.exokey - with correct crc value"() {
    setup:
    // Install it first
    launchAddonsManagerSilently([CommandLineParameters.APPLY_CMD, "patch-5.2-1.zip.enc","public.exokey","${CRCCHECK_OPT}=801053583"])
    ProcessResult process = launchAddonsManager([CommandLineParameters.APPLY_CMD, "foo-addon", CommandLineParameters.SNAPSHOTS_LONG_OPT])
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_OK == process.exitValue()
    cleanup:
    // Uninstall it
    launchAddonsManagerSilently([CommandLineParameters.UNINSTALL_CMD, "patch-5.2:1"])
  }

}
