package org.exoplatform.platform.addon
/**
 * Platform instance settings
 */
class PlatformSettings {

  static final String ADDONS_DIR = "addons"

  enum AppServerType {
    TOMCAT, JBOSSEAP, UNKNOWN
  }

  enum DistributionType {
    COMMUNITY, ENTERPRISE, UNKNOWN
  }

  File homeDirectory

  PlatformSettings(File homeDirectory) {
    this.homeDirectory = homeDirectory
  }

  File getAddonsDirectory() {
    File directory = new File(homeDirectory, ADDONS_DIR)
    if (!directory.exists()) {
      MiscUtils.mkdirs(directory)
    }
    return directory
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
    def filePattern = ~/platform-component-common-.*jar/
    def fileFound
    def findFilenameClosure = {
      if (filePattern.matcher(it.name).find()) {
        fileFound = it
      }
    }
    librariesDirectory.eachFile(findFilenameClosure)
    return fileFound.name.replaceAll("platform-component-common-", "").replaceAll(".jar", "")
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
    if (!addonsDirectory.isDirectory()) {
      Logging.displayMsgError("error: Erroneous setup, add-ons directory (${addonsDirectory}) is invalid.")
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
