package org.exoplatform.platform.addon

/**
 * This class store the add-ons manager settings
 * @author Arnaud Héritier <aheritier@exoplatform.com>
 */
class ManagerSettings extends Properties {
  private static final String ADDONS_MANAGER_PROPERTIES = "org/exoplatform/platform/addon/settings.properties"

  ManagerSettings() {
    super()
    if (isEmpty()) {
      InputStream inputStream = getClass().getClassLoader().
          getResourceAsStream(ADDONS_MANAGER_PROPERTIES)

      if (inputStream == null) {
        throw new RuntimeException("Property file settings.properties not found in the classpath")
      }
      try {
        load(inputStream)
      } finally {
        try {
          inputStream.close()
        } catch (Exception e) {
        }
      }
    }
  }
}
