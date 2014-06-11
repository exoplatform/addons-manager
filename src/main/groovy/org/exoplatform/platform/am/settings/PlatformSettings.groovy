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

import org.exoplatform.platform.am.utils.AddonsManagerException
import org.exoplatform.platform.am.utils.Logging

import java.util.jar.JarEntry
import java.util.jar.JarFile

/**
 * Platform instance settings
 */
class PlatformSettings {
  /**
   * The system property key used to pass the PLF home directory
   */
  static final String PLATFORM_HOME_SYS_PROP = "plf.home"

  /**
   * Application Server types on which PLF add-ons can be managed
   */
  enum AppServerType {
    TOMCAT, JBOSSEAP, UNKNOWN
  }

  /**
   * Distribution types on which PLF add-ons can be managed
   */
  enum DistributionType {
    COMMUNITY, ENTERPRISE, UNKNOWN
  }

  private File _homeDirectory
  private AppServerType _appServerType
  private DistributionType _distributionType
  private String _version
  private File _librariesDirectory
  private File _webappsDirectory

  /**
   * Platform Settings for the instance located with PLATFORM_HOME_SYS_PROP system property
   */
  PlatformSettings() {
    // Take the PLF_HOME from system properties
    if (!System.getProperty(PLATFORM_HOME_SYS_PROP)) {
      throw new AddonsManagerException("Erroneous setup, system property \"${PLATFORM_HOME_SYS_PROP}\" not defined.")
    }
    // Let's set the homeDir
    this._homeDirectory = new File(System.getProperty(PLATFORM_HOME_SYS_PROP))
    // Let's validate few things
    validate()
  }

/**
 * Platform Settings for the instance located with PLATFORM_HOME_SYS_PROP system property
 * @param homeDirectory the path to the PLF instance to analyze
 */
  PlatformSettings(File homeDirectory) {
    this._homeDirectory = homeDirectory
    // Let's validate few things
    validate()
  }

  AppServerType getAppServerType() {
    if (!this._appServerType) {
      if (new File(_homeDirectory, "bin/catalina.sh").exists()) {
        this._appServerType = AppServerType.TOMCAT
      } else if (new File(_homeDirectory, "bin/standalone.sh").exists()) {
        this._appServerType = AppServerType.JBOSSEAP
      } else {
        this._appServerType = AppServerType.UNKNOWN
      }
    }
    return this._appServerType
  }

  DistributionType getDistributionType() {
    if (!this._distributionType) {
      if (new File(_homeDirectory, "eXo_Subscription_Agreement_US.pdf").exists()) {
        this._distributionType = DistributionType.ENTERPRISE
      } else {
        this._distributionType = DistributionType.COMMUNITY
      }
    }
    return this._distributionType
  }

  String getVersion() {
    if (!this._version) {
      def filePattern = ~/platform-component-upgrade-plugins.*jar/
      def fileFound
      def findFilenameClosure = {
        if (filePattern.matcher(it.name).find()) {
          fileFound = it
        }
      }
      librariesDirectory.eachFile(findFilenameClosure)
      if (fileFound == null) {
        throw new AddonsManagerException("Unable to find platform-component-upgrade-plugins jar in ${librariesDirectory}")
      } else {
        JarFile jarFile = new JarFile(fileFound)
        JarEntry jarEntry = jarFile.getJarEntry("conf/platform.properties")
        InputStream inputStream = jarFile.getInputStream(jarEntry)
        Properties platformProperties = new Properties()
        platformProperties.load(inputStream)
        this._version = platformProperties.getProperty("org.exoplatform.platform")
      }
    }
    return this._version
  }

  File getHomeDirectory() {
    return this._homeDirectory
  }

  File getLibrariesDirectory() {
    if (!this._librariesDirectory) {
      switch (appServerType) {
        case AppServerType.TOMCAT:
          this._librariesDirectory = new File(_homeDirectory, "lib")
          break
        case AppServerType.JBOSSEAP:
          this._librariesDirectory = new File(_homeDirectory, "standalone/deployments/platform.ear/lib")
          break
      }
    }
    return this._librariesDirectory
  }

  File getWebappsDirectory() {
    if (!this._webappsDirectory) {
      switch (appServerType) {
        case AppServerType.TOMCAT:
          this._webappsDirectory = new File(_homeDirectory, "webapps")
          break
        case AppServerType.JBOSSEAP:
          this._webappsDirectory = new File(_homeDirectory, "standalone/deployments/platform.ear")
          break
      }
    }
    return this._webappsDirectory
  }

  private void validate() {
    if (!this._homeDirectory.isDirectory()) {
      throw new AddonsManagerException("Erroneous setup, product home directory (${_homeDirectory}) is invalid.")
    }
    if (!this.librariesDirectory.isDirectory()) {
      throw new AddonsManagerException("Erroneous setup, platform libraries directory (${librariesDirectory}) is invalid.")
    }
    if (!this.webappsDirectory.isDirectory()) {
      throw new AddonsManagerException("Erroneous setup, platform web applications directory (${webappsDirectory}) is invalid.")
    }
    if (AppServerType.UNKNOWN.equals(this.appServerType)) {
      throw new AddonsManagerException("Erroneous setup, cannot computes the application server type.")
    }
    if (DistributionType.UNKNOWN.equals(this.distributionType)) {
      throw new AddonsManagerException("Erroneous setup, cannot computes the distribution type.")
    }
  }

  void describe() {
    Logging.displayMsgVerbose(
        "Platform Settings : ${this.properties.sort { it.key }.collect { it }.findAll { !['class'].contains(it.key) }.join(' , ')}")
  }

}
