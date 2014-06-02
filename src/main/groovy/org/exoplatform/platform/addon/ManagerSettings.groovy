/**
 * Copyright (C) 2003-2014 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.exoplatform.platform.addon

/**
 * Manager execution settings
 */
class ManagerSettings {
  def private Properties props = new Properties()
  def PlatformSettings platformSettings
  def Action action
  def String addonId
  def String addonVersion
  def boolean verbose
  def boolean force
  def boolean snapshots

  enum Action {
    LIST, INSTALL, UNINSTALL, HELP
  }

  private Properties getProps() {
    if (props.isEmpty()) {
      InputStream inputStream = getClass().getClassLoader().
          getResourceAsStream("org/exoplatform/platform/addon/settings.properties")

      if (inputStream == null) {
        Logging.displayMsgError("Property file settings.properties not found in the classpath")
        return props
      }
      try {
        props.load(inputStream)
      } finally {
        try {
          inputStream.close()
        } catch (Exception e) {
        }
      }
    }
    return props;
  }

  String getVersion() {
    return getProps().version
  }

  String getlocalAddonsCatalogFilename() {
    return getProps().localAddonsCatalogFilename
  }

  String getAddonsDirectoryPath() {
    return getProps().addonsDirectoryPath
  }

  File getAddonsDirectory() {
    File directory = new File(platformSettings.homeDirectory, addonsDirectoryPath)
    if (!directory.exists()) {
      MiscUtils.mkdirs(directory)
    }
    return directory
  }

  File getLocalAddonsCatalogFile() {
    return new File(addonsDirectory, getlocalAddonsCatalogFilename())
  }

  String getLocalAddonsCatalog() {
    return getLocalAddonsCatalogFile().text
  }

  URL getCentralCatalogUrl() {
    return new URL(getProps().centralCatalogUrl)
  }

  String getCentralCatalog() {
    return getCentralCatalogUrl().text
  }

  boolean validate() {
    def result = true;
    if (!addonsDirectory.isDirectory()) {
      Logging.displayMsgError("error: Erroneous setup, add-ons directory (${addonsDirectory}) is invalid.")
      result = false
    }
    return result
  }

}
