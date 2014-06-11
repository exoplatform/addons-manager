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

import org.exoplatform.platform.am.utils.Logging
import spock.lang.Specification

/**
 * @author Arnaud Héritier <aheritier@exoplatform.com>
 */
class AddonsManagerSettingsTest extends Specification {
  AddonsManagerSettings managerSettings = new AddonsManagerSettings()

  def setupSpec() {
    Logging.verbose = true
  }

  def "version is defined"() {
    expect:
    managerSettings.version
  }

  def "centralCatalogUrl is defined"() {
    expect:
    managerSettings.centralCatalogUrl
  }

  def "addonsDirectoryPath is defined"() {
    expect:
    managerSettings.addonsDirectoryPath
  }

  def "localAddonsCatalogFilename is defined"() {
    expect:
    managerSettings.localAddonsCatalogFilename
  }

  def "scriptName is defined"() {
    expect:
    managerSettings.scriptName
  }
}
