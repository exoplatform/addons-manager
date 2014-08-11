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

import static org.exoplatform.platform.am.cli.CommandLineParameters.getDESCRIBE_CMD

/**
 * @author Arnaud Héritier <aheritier@exoplatform.com>
 */
class AddonsManagerDescribeIT extends IntegrationTestsSpecification {

  def cleanup() {
    // After each test we remove the content of the add-ons directory to be safe
    getEnvironmentSettings().getAddonsDirectory().deleteDir()
  }

  /**
   * if the foo-addon exists and has at least 1 released version : list all the informations about the most recent released version of foo-addon
   * if the foo-addon exists and has no released version (only snapshots) : must raise an error saying "The add-on foo-addon doesn't doesn't have a released version yet ! add snapshot option to use the snapshot version [KO]"
   * if foo-addon doesn't exists in the catalog : must raise an error saying "The add-on foo-addon doesn't exists in the catalog, check your add-on name [KO]"
   */
  def "[AM_INF_01] addon(.bat) describe foo-addon"() {
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_OK == launchAddonsManager([DESCRIBE_CMD, "foo-addon"]).exitValue()
  }

  /**
   * if the foo-addon exists and has released version 42 : list all the informations the version42 of foo-addon
   * if the foo-addon exists and has no released version 42 : must raise an error saying "The add-on foo-addon doesn't have a released version 42 yet ! check the version you specify [KO]"
   * if foo-addon doesn't exists in the catalog : must raise an error saying "The add-on foo-addon doesn't exists in the catalog, check your add-on name [KO]"
   */
  def "[AM_INF_02] addon(.bat) describe foo-addon:42"() {
    expect:
    // Verify return code
    AddonsManagerConstants.RETURN_CODE_OK == launchAddonsManager([DESCRIBE_CMD, "foo-addon:42"]).exitValue()
  }

}
