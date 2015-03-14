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

import org.exoplatform.platform.am.ex.ErroneousSetupException

import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.regex.Pattern

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
    TOMCAT("lib", "webapps"),
    JBOSS("standalone/deployments/platform.ear/lib", "standalone/deployments/platform.ear"),
    BITNAMI("lib", "../../apache-tomcat/webapps"),
    UNKNOWN("", "")
    final String librariesPath
    final String webappsPath

    AppServerType(String librariesPath, String webappsPath) {
      this.librariesPath = librariesPath
      this.webappsPath = webappsPath
    }
  }

  /**
   * Distribution types on which PLF add-ons can be managed
   */
  enum DistributionType {
    COMMUNITY, ENTERPRISE, UNKNOWN
  }

  File homeDirectory
  AppServerType appServerType
  DistributionType distributionType
  String version
  File librariesDirectory
  File webappsDirectory

  /**
   * Platform Settings for the instance located with PLATFORM_HOME_SYS_PROP system property
   */
  PlatformSettings() {
    // Take the PLF_HOME from system properties
    if (!System.getProperty(PLATFORM_HOME_SYS_PROP)) {
      throw new ErroneousSetupException("Erroneous setup, system property \"${PLATFORM_HOME_SYS_PROP}\" not defined.")
    }
    // Let's validate few things
    init(new File(System.getProperty(PLATFORM_HOME_SYS_PROP)))
  }

/**
 * Platform Settings for the instance located with PLATFORM_HOME_SYS_PROP system property
 * @param homeDirectory the path to the PLF instance to analyze
 */
  PlatformSettings(File homeDirectory) {
    init(homeDirectory)
  }

  /**
   * Initialize and validate platform settings
   */
  private void init(File homeDirectory) {
    // Let's set the homeDir
    this.homeDirectory = homeDirectory
    if (!homeDirectory.isDirectory()) {
      throw new ErroneousSetupException("Erroneous setup, product home directory (${homeDirectory}) is invalid.")
    }

    if (new File(homeDirectory, "bin/catalina.sh").exists()) {
      this.appServerType = AppServerType.TOMCAT
    } else if (new File(homeDirectory, "bin/standalone.sh").exists()) {
      this.appServerType = AppServerType.JBOSS
    } else if (new File(homeDirectory, "../../apache-tomcat/bin/catalina.sh").exists()) {
      this.appServerType = AppServerType.BITNAMI
    } else {
      this.appServerType = AppServerType.UNKNOWN
    }
    if (AppServerType.UNKNOWN.equals(this.appServerType)) {
      throw new ErroneousSetupException("Erroneous setup, cannot compute the application server type.")
    }

    if (new File(homeDirectory, "eXo_Subscription_Agreement_US.pdf").exists()) {
      this.distributionType = DistributionType.ENTERPRISE
    } else {
      this.distributionType = DistributionType.COMMUNITY
    }
    if (DistributionType.UNKNOWN.equals(this.distributionType)) {
      throw new ErroneousSetupException("Erroneous setup, cannot compute the distribution type.")
    }

    this.librariesDirectory = new File(homeDirectory, this.appServerType.librariesPath)
    if (!this.librariesDirectory.isDirectory()) {
      throw new ErroneousSetupException("Erroneous setup, platform libraries directory (${this.librariesDirectory}) is invalid.")
    }

    this.webappsDirectory = new File(homeDirectory, this.appServerType.webappsPath)
    if (!this.webappsDirectory.isDirectory()) {
      throw new ErroneousSetupException(
          "Erroneous setup, platform web applications directory (${this.webappsDirectory}) is invalid.")
    }
    Pattern filePattern = ~/platform-component-upgrade-plugins.*jar/
    String fileFound
    Closure findFilenameClosure = {
      if (filePattern.matcher(it.name).find()) {
        fileFound = it
      }
    }
    this.librariesDirectory.eachFile(findFilenameClosure)
    if (fileFound == null) {
      throw new ErroneousSetupException(
          "Erroneous setup, Unable to find platform-component-upgrade-plugins jar in ${librariesDirectory}")
    } else {
      JarFile jarFile = new JarFile(fileFound)
      JarEntry jarEntry = jarFile.getJarEntry("conf/platform.properties")
      InputStream inputStream = jarFile.getInputStream(jarEntry)
      Properties platformProperties = new Properties()
      platformProperties.load(inputStream)
      this.version = platformProperties.getProperty("org.exoplatform.platform")
    }
  }

}
