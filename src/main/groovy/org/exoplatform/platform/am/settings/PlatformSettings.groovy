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
package org.exoplatform.platform.am.settings

import org.exoplatform.platform.am.utils.AddonsManagerException
import org.exoplatform.platform.am.utils.Logging

import java.util.jar.JarEntry
import java.util.jar.JarFile

/**
 * Platform instance settings
 */
class PlatformSettings {
  public static final String PLATFORM_HOME_SYS_PROP = "product.home"

  enum AppServerType {
    TOMCAT, JBOSSEAP, UNKNOWN
  }

  enum DistributionType {
    COMMUNITY, ENTERPRISE, UNKNOWN
  }

  File homeDirectory

  PlatformSettings() {
    // Platform settings initialization
    if (!System.getProperty(PLATFORM_HOME_SYS_PROP)) {
      throw new AddonsManagerException("Erroneous setup, system property \"${PLATFORM_HOME_SYS_PROP}\" not defined.")
    }
    this.homeDirectory = new File(System.getProperty(PLATFORM_HOME_SYS_PROP))
  }

  PlatformSettings(File homeDirectory) {
    this.homeDirectory = homeDirectory
  }

  AppServerType getAppServerType() {
    if (new File(homeDirectory, "bin/catalina.sh").exists()) return AppServerType.TOMCAT
    if (new File(homeDirectory, "bin/standalone.sh").exists()) return AppServerType.JBOSSEAP
    return AppServerType.UNKNOWN
  }

  DistributionType getDistributionType() {
    if (new File(homeDirectory, "eXo_Subscription_Agreement_US.pdf").exists()) return DistributionType.ENTERPRISE
    return DistributionType.COMMUNITY
  }

  String getVersion() {
    def filePattern = ~/platform-component-upgrade-plugins.*jar/
    def fileFound
    def findFilenameClosure = {
      if (filePattern.matcher(it.name).find()) {
        fileFound = it
      }
    }
    librariesDirectory.eachFile(findFilenameClosure)
    if (fileFound == null) {
      throw new Exception("Unable to find platform-component-upgrade-plugins jar in ${librariesDirectory}")
    } else {
      JarFile jarFile = new JarFile(fileFound)
      JarEntry jarEntry = jarFile.getJarEntry("conf/platform.properties")
      InputStream inputStream = jarFile.getInputStream(jarEntry)
      Properties platformProperties = new Properties()
      platformProperties.load(inputStream)
      return platformProperties.getProperty("org.exoplatform.platform")
    }
  }

  File getLibrariesDirectory() {
    switch (appServerType) {
      case AppServerType.TOMCAT:
        return new File(homeDirectory, "lib")
      case AppServerType.JBOSSEAP:
        return new File(homeDirectory, "standalone/deployments/platform.ear/lib")
    }
  }

  File getWebappsDirectory() {
    switch (appServerType) {
      case AppServerType.TOMCAT:
        return new File(homeDirectory, "webapps")
      case AppServerType.JBOSSEAP:
        return new File(homeDirectory, "standalone/deployments/platform.ear")
    }
  }

  boolean validate() {
    def result = true;
    if (!homeDirectory.isDirectory()) {
      Logging.displayMsgError("error: Erroneous setup, product home directory (${homeDirectory}) is invalid.")
      result = false
    }
    if (!librariesDirectory.isDirectory()) {
      Logging.displayMsgError("error: Erroneous setup, platform libraries directory (${librariesDirectory}) is invalid.")
      result = false
    }
    if (!webappsDirectory.isDirectory()) {
      Logging.displayMsgError("error: Erroneous setup, platform web applications directory (${webappsDirectory}) is invalid.")
      result = false
    }
    if (AppServerType.UNKNOWN.equals(appServerType)) {
      Logging.displayMsgError("error: Erroneous setup, cannot computes the application server type.")
      result = false
    }
    if (DistributionType.UNKNOWN.equals(distributionType)) {
      Logging.displayMsgError("error: Erroneous setup, cannot computes the distribution type.")
      result = false
    }
    return result
  }

}
