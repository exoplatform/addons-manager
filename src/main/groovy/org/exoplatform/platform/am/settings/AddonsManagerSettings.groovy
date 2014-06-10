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

/**
 * This class store the add-ons manager settings
 * @author Arnaud Héritier <aheritier@exoplatform.com>
 */
class AddonsManagerSettings extends Properties {
  private static final String ADDONS_MANAGER_PROPERTIES = "org/exoplatform/platform/am/settings/am.properties"

  AddonsManagerSettings() {
    super()
    InputStream inputStream = getClass().getClassLoader().
        getResourceAsStream(ADDONS_MANAGER_PROPERTIES)

    if (inputStream == null) {
      throw new AddonsManagerException(
          "Erroneous packaging, Property file \"${ADDONS_MANAGER_PROPERTIES}\" not found in the classpath")
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

  public String getScriptName() {
    // Computes the script addon from the OS
    def scriptName = "${scriptBaseName}.sh"
    if (System.properties['os.name'].toLowerCase().contains('windows')) {
      scriptName = "${scriptBaseName}.bat"
    }
    return scriptName
  }

  public String describe() {
    return this.sort { it.key }.collect { it }.findAll { !['class'].contains(it.key) }.join('\n')
  }
}
