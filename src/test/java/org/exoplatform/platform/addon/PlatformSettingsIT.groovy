package org.exoplatform.platform.addon

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

import static org.junit.Assert.*

@RunWith(Parameterized.class)
class PlatformSettingsIT {

  @Parameterized.Parameter(0)
  public File productHome;

  @Parameterized.Parameters(name = "{index}: productHome={0}")
  static Collection<Object[]> data() {
    def integrationTestsDirPath = System.getProperty("integrationTestsDirPath")
    assertNotNull("Integration tests directory path mustn't be null", integrationTestsDirPath)
    def File integrationTestsDir = new File(integrationTestsDirPath);
    assertTrue("Integration tests directory must be a directory", integrationTestsDir.isDirectory())
    def data = new ArrayList<Object[]>()
    integrationTestsDir.eachDir { directory ->
      data.add([directory] as Object[])
    }
    return data
  }

  @Test
  void validateSettings() {
    PlatformSettings settings = new PlatformSettings(productHome)
    assertTrue("Cannot validate platform settings", settings.validate())
  }

  @Test
  void validateVersion() {
    PlatformSettings settings = new PlatformSettings(productHome)
    String expectedVersion =
        productHome.name.replaceAll("platform-","").replaceAll("community-","").replaceAll("-jboss-standalone","")
    assertEquals("Cannot validate platform version", expectedVersion, settings.version)
  }

  @Test
  void validateAppServerType() {
    PlatformSettings settings = new PlatformSettings(productHome)
    PlatformSettings.AppServerType expectedAppServerType =
        productHome.name.contains("jboss") ?
            PlatformSettings.AppServerType.JBOSSEAP : PlatformSettings.AppServerType.TOMCAT
    assertEquals("Cannot validate platform version", expectedAppServerType, settings.appServerType)
  }

  @Test
  void validateDistributionType() {
    PlatformSettings settings = new PlatformSettings(productHome)
    PlatformSettings.DistributionType expectedDistributionType =
        productHome.name.contains("community") ?
                    PlatformSettings.DistributionType.COMMUNITY : PlatformSettings.DistributionType.ENTERPRISE
    assertEquals("Cannot validate platform version", expectedDistributionType, settings.distributionType)
  }
}
