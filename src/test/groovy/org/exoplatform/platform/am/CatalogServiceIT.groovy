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
package org.exoplatform.platform.am

import groovy.json.JsonException
import org.exoplatform.platform.am.utils.Logger
import spock.lang.Shared

/**
 * @author Arnaud Héritier <aheritier@exoplatform.com>
 */
class CatalogServiceIT extends IntegrationTestsSpecification {

  /**
   * Logger
   */
  private static final Logger LOG = Logger.get()

  def setupSpec() {
    LOG.enableDebug()
  }

  @Shared
  CatalogService catalogService = new CatalogService()

  def "parseJSONAddonsList can read a valid catalog"() {
    when:
    List<Addon> catalog = catalogService.parseJSONAddonsList(new File(testDataDir(), "catalog.json").text)
    then:
    catalog.size() == 7
  }

  def "parseJSONAddonsList removes invalid entries"() {
    when:
    List<Addon> catalog = catalogService.parseJSONAddonsList(new File(testDataDir(), "catalog-with-invalid-entries.json").text)
    then:
    catalog.size() == 1
  }

  def "parseJSONAddonsList cannot read invalid JSON"() {
    when:
    catalogService.parseJSONAddonsList(new File(testDataDir(), "catalog-unreadable.json").text)
    then:
    thrown JsonException
  }

}
