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
 * This class exposes environment settings about the Add-ons Manager, the PLF server, the system, ...
 */
class EnvironmentSettings {
  private static final String ADDONS_MANAGER_PROPERTIES = "org/exoplatform/platform/addon/settings.properties"
  def private Properties props
  def PlatformSettings platformSettings
  def ManagerCLIArgs cliArgs

  private Properties getProps() {
    if (!props) {
      InputStream inputStream = getClass().getClassLoader().
          getResourceAsStream(ADDONS_MANAGER_PROPERTIES)

      if (inputStream == null) {
        throw new RuntimeException("Property file settings.properties not found in the classpath")
      }
      try {
        props = new Properties()
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

  /**
   * Returns the current version of the platform manager
   * @return the manager version
   */
  public String getManagerVersion() {
    return getProps().version
  }

  /**
   * Returns the path where add-ons are stored
   * @return a directory path
   */
  public File getAddonsDirectory() {
    File directory = new File(platformSettings.homeDirectory, getProps().addonsDirectoryPath)
    if (!directory.exists()) {
      MiscUtils.mkdirs(directory)
    }
    return directory
  }

  /**
   * Returns the path to the local catalog
   * @return a file path
   */
  public File getLocalAddonsCatalogFile() {
    return new File(addonsDirectory, getProps().localAddonsCatalogFilename)
  }

  /**
   * Returns the content of the local catalog
   * @return a JSON formatted text
   */
  public String getLocalAddonsCatalog() {
    return getLocalAddonsCatalogFile().text
  }

  /**
   * Returns the URL to the remote catalog
   * @return the URL of the central catalog
   */
  public URL getCentralCatalogUrl() {
    return new URL(getProps().centralCatalogUrl)
  }

  /**
   * Returns the content of the remote catalog
   * @return a JSON formatted text
   */
  public String getCentralCatalog() {
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
