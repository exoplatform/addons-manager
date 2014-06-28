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

import org.exoplatform.platform.am.utils.FileUtils

/**
 * This class exposes environment settings about the Add-ons Manager, the PLF server, the system, ...
 */
class EnvironmentSettings {
  PlatformSettings platform
  AddonsManagerSettings manager
  /**
   * The path where add-ons are stored
   */
  File addonsDirectory
  /**
   * The path where add-ons archives are stored
   */
  File archivesDirectory
  /**
   * Where are cached remote catalogs
   */
  File catalogsCacheDirectory
  /**
   * Where overwritten files are stored
   */
  File overwrittenFilesDirectory
  /**
   * The path where add-ons statuses are stored
   */
  File statusesDirectory
  /**
   * The path where add-ons manager old versions are stored
   */
  File versionsDirectory
  /**
   * The path to the local catalog
   */
  File localAddonsCatalogFile

  EnvironmentSettings() {
    manager = new AddonsManagerSettings()
    platform = new PlatformSettings()
    addonsDirectory = new File(platform.homeDirectory, manager.addonsDirectoryPath)
    if (!addonsDirectory.exists()) {
      FileUtils.mkdirs(addonsDirectory)
    }
    archivesDirectory = new File(addonsDirectory, manager.archivesDirectoryName)
    if (!archivesDirectory.exists()) {
      FileUtils.mkdirs(archivesDirectory)
    }
    catalogsCacheDirectory = new File(addonsDirectory, manager.catalogsCacheDirectoryName)
    if (!catalogsCacheDirectory.exists()) {
      FileUtils.mkdirs(catalogsCacheDirectory)
    }
    overwrittenFilesDirectory = new File(addonsDirectory, manager.overwrittenFilesDirectoryName)
    if (!overwrittenFilesDirectory.exists()) {
      FileUtils.mkdirs(overwrittenFilesDirectory)
    }
    statusesDirectory = new File(addonsDirectory, manager.statusesDirectoryName)
    if (!statusesDirectory.exists()) {
      FileUtils.mkdirs(statusesDirectory)
    }
    versionsDirectory = new File(addonsDirectory, manager.versionsDirectoryName)
    if (!versionsDirectory.exists()) {
      FileUtils.mkdirs(versionsDirectory)
    }
    localAddonsCatalogFile = new File(addonsDirectory, manager.localAddonsCatalogFilename)
  }

  /**
   * Returns the URL to the remote catalog
   * @return the URL of the remote catalog
   */
  URL getRemoteCatalogUrl() {
    return new URL(manager.remoteCatalogUrl)
  }
}
