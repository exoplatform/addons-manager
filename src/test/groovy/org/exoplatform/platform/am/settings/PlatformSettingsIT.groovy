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
package org.exoplatform.platform.am.settings

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

import static org.junit.Assert.*

@RunWith(Parameterized.class)
class PlatformSettingsIT {

  @Parameterized.Parameter(0)
  public File plfHome;

  @Parameterized.Parameters(name = "{index}: plfHome={0}")
  static Collection<Object[]> data() {
    def integrationTestsDirPath = System.getProperty("integrationTestsDirPath")
    assertNotNull("Integration tests directory path mustn't be null", integrationTestsDirPath)
    def File integrationTestsDir = new File(integrationTestsDirPath);
    assertTrue("Integration tests directory must be a directory", integrationTestsDir.isDirectory())
    def data = new ArrayList<Object[]>()
    integrationTestsDir.eachDir { directory ->
      data.add([directory] as Object[])
    }
    return data
  }

  @Test
  void validateSettings() {
    assertNotNull("Cannot validate platform settings", new PlatformSettings(plfHome))
  }

  @Test
  void validateVersion() {
    PlatformSettings settings = new PlatformSettings(plfHome)
    String expectedVersion =
        plfHome.name.replaceAll("platform-", "").replaceAll("community-", "").replaceAll("-jboss-standalone", "")
    assertEquals("Cannot validate platform version", expectedVersion, settings.version)
  }

  @Test
  void validateAppServerType() {
    PlatformSettings settings = new PlatformSettings(plfHome)
    PlatformSettings.AppServerType expectedAppServerType =
        plfHome.name.contains("jboss") ?
            PlatformSettings.AppServerType.JBOSSEAP : PlatformSettings.AppServerType.TOMCAT
    assertEquals("Cannot validate platform version", expectedAppServerType, settings.appServerType)
  }

  @Test
  void validateDistributionType() {
    PlatformSettings settings = new PlatformSettings(plfHome)
    PlatformSettings.DistributionType expectedDistributionType =
        plfHome.name.contains("community") ?
            PlatformSettings.DistributionType.COMMUNITY : PlatformSettings.DistributionType.ENTERPRISE
    assertEquals("Cannot validate platform version", expectedDistributionType, settings.distributionType)
  }
}
