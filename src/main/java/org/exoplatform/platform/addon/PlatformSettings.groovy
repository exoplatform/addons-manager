package org.exoplatform.platform.addon

import java.nio.file.*;

public class PlatformSettings {

  public static final String ADDONS_DIR = "addons"

  public enum AppServerType {
    TOMCAT, JBOSSEAP, UNKNOWN
  }

  public enum DistributionType {
    COMMUNITY, ENTERPRISE, UNKNOWN
  }

  private File homeDirectory

  public PlatformSettings(File homeDirectory) {
    this.homeDirectory = homeDirectory
  }

  public File getAddonsDirectory() {
    File directory = new File(homeDirectory, ADDONS_DIR)
    if (!directory.exists()) {
      MiscUtils.mkdirs(directory)
    }
    return directory
  }

  public AppServerType getAppServerType() {
    if (new File(homeDirectory, "bin/catalina.sh").exists()) return AppServerType.TOMCAT
    if (new File(homeDirectory, "bin/standalone.sh").exists()) return AppServerType.JBOSSEAP
    return AppServerType.UNKNOWN
  }

  public DistributionType getDistributionType() {
    if (new File(homeDirectory, "eXo_Subscription_Agreement_US.pdf").exists()) return DistributionType.ENTERPRISE
    return DistributionType.COMMUNITY
  }

  public String getVersion() {
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

  public File getLibrariesDirectory() {
    switch (appServerType) {
      case AppServerType.TOMCAT:
        return new File(homeDirectory, "lib")
      case AppServerType.JBOSSEAP:
        return new File(homeDirectory, "standalone/deployments/platform.ear/lib")
    }
  }

  public File getWebappsDirectory() {
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
      println "error: Erroneous setup, product home directory (${homeDirectory}) is invalid."
      result = false
    }
    if (!addonsDirectory.isDirectory()) {
      println "error: Erroneous setup, add-ons directory (${addonsDirectory}) is invalid."
      result = false
    }
    if (!librariesDirectory.isDirectory()) {
      println "error: Erroneous setup, platform libraries directory (${librariesDir}) is invalid."
      result = false
    }
    if (!webappsDirectory.isDirectory()) {
      println "error: Erroneous setup, platform web applications directory (${webappsDir}) is invalid."
      result = false
    }
    if (AppServerType.UNKNOWN.equals(appServerType)) {
      println "error: Erroneous setup, cannot computes the application server type."
      result = false
    }
    if (DistributionType.UNKNOWN.equals(distributionType)) {
      println "error: Erroneous setup, cannot computes the distribution type."
      result = false
    }
    return result
  }


}
