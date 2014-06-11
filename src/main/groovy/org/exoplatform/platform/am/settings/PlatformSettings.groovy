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

import groovy.transform.Canonical
import org.exoplatform.platform.am.utils.AddonsManagerException
import org.exoplatform.platform.am.utils.Logging

import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.regex.Pattern

/**
 * Platform instance settings
 */
@Canonical
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
    _homeDirectory = new File(System.getProperty(PLATFORM_HOME_SYS_PROP))
    // Let's validate few things
    validate()
  }

/**
 * Platform Settings for the instance located with PLATFORM_HOME_SYS_PROP system property
 * @param homeDirectory the path to the PLF instance to analyze
 */
  PlatformSettings(File homeDirectory) {
    _homeDirectory = homeDirectory
    // Let's validate few things
    validate()
  }

  AppServerType getAppServerType() {
    if (!_appServerType) {
      if (new File(_homeDirectory, "bin/catalina.sh").exists()) {
        _appServerType = AppServerType.TOMCAT
      } else if (new File(_homeDirectory, "bin/standalone.sh").exists()) {
        _appServerType = AppServerType.JBOSSEAP
      } else {
        _appServerType = AppServerType.UNKNOWN
      }
    }
    return _appServerType
  }

  DistributionType getDistributionType() {
    if (!_distributionType) {
      if (new File(_homeDirectory, "eXo_Subscription_Agreement_US.pdf").exists()) {
        _distributionType = DistributionType.ENTERPRISE
      } else {
        _distributionType = DistributionType.COMMUNITY
      }
    }
    return _distributionType
  }

  String getVersion() {
    if (!_version) {
      Pattern filePattern = ~/platform-component-upgrade-plugins.*jar/
      String fileFound
      Closure findFilenameClosure = {
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
        _version = platformProperties.getProperty("org.exoplatform.platform")
      }
    }
    return _version
  }

  File getHomeDirectory() {
    return _homeDirectory
  }

  File getLibrariesDirectory() {
    if (!_librariesDirectory) {
      switch (appServerType) {
        case AppServerType.TOMCAT:
          _librariesDirectory = new File(_homeDirectory, "lib")
          break
        case AppServerType.JBOSSEAP:
          _librariesDirectory = new File(_homeDirectory, "standalone/deployments/platform.ear/lib")
          break
      }
    }
    return _librariesDirectory
  }

  File getWebappsDirectory() {
    if (!_webappsDirectory) {
      switch (appServerType) {
        case AppServerType.TOMCAT:
          _webappsDirectory = new File(_homeDirectory, "webapps")
          break
        case AppServerType.JBOSSEAP:
          _webappsDirectory = new File(_homeDirectory, "standalone/deployments/platform.ear")
          break
      }
    }
    return _webappsDirectory
  }

  private void validate() {
    if (!_homeDirectory.isDirectory()) {
      throw new AddonsManagerException("Erroneous setup, product home directory (${_homeDirectory}) is invalid.")
    }
    if (!librariesDirectory.isDirectory()) {
      throw new AddonsManagerException("Erroneous setup, platform libraries directory (${librariesDirectory}) is invalid.")
    }
    if (!webappsDirectory.isDirectory()) {
      throw new AddonsManagerException("Erroneous setup, platform web applications directory (${webappsDirectory}) is invalid.")
    }
    if (AppServerType.UNKNOWN.equals(appServerType)) {
      throw new AddonsManagerException("Erroneous setup, cannot computes the application server type.")
    }
    if (DistributionType.UNKNOWN.equals(distributionType)) {
      throw new AddonsManagerException("Erroneous setup, cannot computes the distribution type.")
    }
  }

  void describe() {
    Logging.displayMsgVerbose(
        "Platform Settings : ${properties.sort { it.key }.collect { it }.findAll { !['class'].contains(it.key) }.join(' , ')}")
  }

}
