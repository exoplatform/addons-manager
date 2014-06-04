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
