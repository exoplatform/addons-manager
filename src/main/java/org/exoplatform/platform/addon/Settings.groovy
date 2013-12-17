package org.exoplatform.platform.addon

/**
 * Copyright (C) 2003-2013 eXo Platform SAS.
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
/**
 * Settings management
 */
public class Settings {
  static final instance = new Settings()
  public static final String ADDONS_DIR = "addons"

  def private Properties props
  def File productHome
  def File librariesDir
  def File webappsDir
  def Action action
  def String addonId
  def boolean verbose
  def boolean force
  def boolean snapshots

  private Settings() {
    if (!System.getProperty("product.home")) {
      println 'error: Erroneous setup, system property product.home not defined.'
      System.exit 1
    }
    productHome = new File(System.getProperty("product.home"))
    // Discovers if this is a Tomcat or JBoss distribution
    if (new File(productHome, "bin/catalina.sh")) {
      librariesDir = new File(productHome, "lib")
      webappsDir = new File(productHome, "webapps")
    } else {
      librariesDir = new File(productHome, "standalone/deployments/platform.ear/lib")
      webappsDir = new File(productHome, "standalone/deployments/platform.ear")
    }
  }

  enum Action {
    LIST, INSTALL, UNINSTALL, HELP
  }

  private Properties getProps() {
    if (props == null) {
      props = new Properties()
      InputStream inputStream = getClass().getClassLoader().
          getResourceAsStream("org/exoplatform/platform/addon/settings.properties")

      if (inputStream == null) {
        println "Property file settings.properties not found in the classpath"
        System.exit 1
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

  boolean validate() {
    if (!productHome.isDirectory()) {
      println "error: Erroneous setup, product home directory (${productHome}) is invalid."
      System.exit 1
    }
    if (!addonsDirectory.isDirectory()) {
      println "error: Erroneous setup, add-ons directory (${addonsDirectory}) is invalid."
      System.exit 1
    }
    if (!librariesDir.isDirectory()) {
      println "error: Erroneous setup, platform libraries directory (${librariesDir}) is invalid."
      System.exit 1
    }
    if (!webappsDir.isDirectory()) {
      println "error: Erroneous setup, platform web applications directory (${webappsDir}) is invalid."
      System.exit 1
    }
  }

  String getVersion() {
    return getProps().version
  }

  String getLocalAddonsCatalogFilename() {
    return getProps().localAddonsCatalogFilename
  }


  File getLocalAddonsCatalogFile() {
    return new File(addonsDirectory, getLocalAddonsCatalogFilename())
  }

  URL getCentralCatalogUrl() {
    return new URL(getProps().centralCatalogUrl)
  }

  String getLocalAddonsCatalog() {
    return getLocalAddonsCatalogFile().text
  }

  String getCentralCatalog() {
    return getCentralCatalogUrl().text
  }

  File getAddonsDirectory() {
    File directory = new File(productHome, ADDONS_DIR)
    if (!directory.exists()) {
      MiscUtils.mkdirs(directory)
    }
    return directory
  }

}
