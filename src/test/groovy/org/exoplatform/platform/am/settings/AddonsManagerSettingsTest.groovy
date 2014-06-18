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

import org.exoplatform.platform.am.utils.Console
import org.exoplatform.platform.am.utils.Logger
import spock.lang.Specification

/**
 * @author Arnaud Héritier <aheritier@exoplatform.com>
 */
class AddonsManagerSettingsTest extends Specification {

  def setupSpec() {
    Logger.get().enableDebug()
  }

  def cleanSpec() {
    Console.get().reset()
  }

  def "default properties are defined"() {
    setup:
    def managerSettings = new AddonsManagerSettings()
    Logger.get().debug("Manager Settings", managerSettings, ["class"])
    expect:
    managerSettings.addonsDirectoryPath
    managerSettings.archivesDirectoryName
    managerSettings.catalogsCacheDirectoryName
    managerSettings.centralCatalogUrl
    managerSettings.localAddonsCatalogFilename
    managerSettings.scriptBaseName
    managerSettings.scriptName
    managerSettings.statusesDirectoryName
    managerSettings.version
    // Created within the object constructor
    managerSettings.scriptName
  }

  def "System property am.XXX can override the XXX property"() {
    setup:
    System.setProperty("${AddonsManagerSettings.PROPERTY_PREFIX}.addonsDirectoryPath", "foo")
    System.setProperty("${AddonsManagerSettings.PROPERTY_PREFIX}.archivesDirectoryName", "foo")
    System.setProperty("${AddonsManagerSettings.PROPERTY_PREFIX}.catalogsCacheDirectoryName", "foo")
    System.setProperty("${AddonsManagerSettings.PROPERTY_PREFIX}.centralCatalogUrl", "foo")
    System.setProperty("${AddonsManagerSettings.PROPERTY_PREFIX}.localAddonsCatalogFilename", "foo")
    System.setProperty("${AddonsManagerSettings.PROPERTY_PREFIX}.scriptBaseName", "foo")
    System.setProperty("${AddonsManagerSettings.PROPERTY_PREFIX}.statusesDirectoryName", "foo")
    System.setProperty("${AddonsManagerSettings.PROPERTY_PREFIX}.version", "foo")
    def managerSettings = new AddonsManagerSettings()
    Logger.get().debug("Manager Settings", managerSettings, ["class"])
    expect:
    "foo".equals(managerSettings.addonsDirectoryPath)
    "foo".equals(managerSettings.archivesDirectoryName)
    "foo".equals(managerSettings.catalogsCacheDirectoryName)
    "foo".equals(managerSettings.centralCatalogUrl)
    "foo".equals(managerSettings.localAddonsCatalogFilename)
    "foo".equals(managerSettings.scriptBaseName)
    "foo".equals(managerSettings.statusesDirectoryName)
    "foo".equals(managerSettings.version)
    // Must have been updated too
    managerSettings.scriptName.startsWith("foo")
    cleanup:
    System.clearProperty("${AddonsManagerSettings.PROPERTY_PREFIX}.addonsDirectoryPath")
    System.clearProperty("${AddonsManagerSettings.PROPERTY_PREFIX}.archivesDirectoryName")
    System.clearProperty("${AddonsManagerSettings.PROPERTY_PREFIX}.catalogsCacheDirectoryName")
    System.clearProperty("${AddonsManagerSettings.PROPERTY_PREFIX}.centralCatalogUrl")
    System.clearProperty("${AddonsManagerSettings.PROPERTY_PREFIX}.localAddonsCatalogFilename")
    System.clearProperty("${AddonsManagerSettings.PROPERTY_PREFIX}.scriptBaseName")
    System.clearProperty("${AddonsManagerSettings.PROPERTY_PREFIX}.statusesDirectoryName")
    System.clearProperty("${AddonsManagerSettings.PROPERTY_PREFIX}.version")
  }

}
