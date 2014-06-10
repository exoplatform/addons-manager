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

import org.exoplatform.platform.am.cli.CommandLineParameters
import org.exoplatform.platform.am.utils.AddonsManagerException
import org.exoplatform.platform.am.utils.FileUtils
import org.exoplatform.platform.am.utils.Logging

/**
 * This class exposes environment settings about the Add-ons Manager, the PLF server, the system, ...
 */
class EnvironmentSettings {
  def PlatformSettings platformSettings
  def AddonsManagerSettings managerSettings
  def CommandLineParameters commandLineArgs

  EnvironmentSettings(AddonsManagerSettings managerSettings, PlatformSettings platformSettings, CommandLineParameters commandLineArgs) {
    this.managerSettings = managerSettings
    this.platformSettings = platformSettings
    this.commandLineArgs = commandLineArgs
    // Let's validate few things
    validate()
  }

  /**
   * Returns the path where add-ons are stored
   * @return a directory path
   */
  File getAddonsDirectory() {
    File directory = new File(platformSettings._homeDirectory, managerSettings.addonsDirectoryPath)
    if (!directory.exists()) {
      FileUtils.mkdirs(directory)
    }
    return directory
  }

  /**
   * Returns the path to the local catalog
   * @return a file path
   */
  File getLocalAddonsCatalogFile() {
    return new File(addonsDirectory, managerSettings.localAddonsCatalogFilename)
  }

  /**
   * Returns the content of the local catalog
   * @return a JSON formatted text
   */
  String getLocalAddonsCatalog() {
    return localAddonsCatalogFile.text
  }

  /**
   * Returns the URL to the remote catalog
   * @return the URL of the central catalog
   */
  URL getCentralCatalogUrl() {
    return new URL(managerSettings.centralCatalogUrl)
  }

  /**
   * Returns the content of the remote catalog
   * @return a JSON formatted text
   */
  String getCentralCatalog() {
    return centralCatalogUrl.text
  }

  private void validate() {
    if (!addonsDirectory.isDirectory()) {
      throw new AddonsManagerException("Erroneous setup, add-ons directory (${addonsDirectory}) is invalid.")
    }
  }

  void describe() {
    Logging.displayMsgVerbose(
        "Environment Settings :\n${this.properties.sort { it.key }.collect { it }.findAll { !['class', 'platformSettings', 'managerSettings', 'commandLineArgs', 'centralCatalog', 'localAddonsCatalog'].contains(it.key) }.join('\n')}\n")
    managerSettings.describe()
    platformSettings.describe()
    commandLineArgs.describe()
  }

}
