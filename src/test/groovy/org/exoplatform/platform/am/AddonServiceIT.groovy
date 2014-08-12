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
import spock.lang.Shared
import spock.lang.Subject

/**
 * @author Arnaud Héritier <aheritier@exoplatform.com>
 */
class AddonServiceIT extends IntegrationTestsSpecification {

  /**
   * Logger
   */
  private static final int NB_ADDONS_CATALOG_JSON = 10

  @Shared
  @Subject
  AddonService addonService = AddonService.getInstance()

  def "One call of loadAddonsFromUrl online with cache"() {
    setup:
    File tmpDir = File.createTempDir()
    URL catalogUrl = new URL(getWebServerRootUrl() + "/catalog.json")
    File catalogCache = new File(tmpDir, "${addonService.convertUrlToFilename(catalogUrl)}.json")
    when:
    List<Addon> addons = addonService.loadAddonsFromUrl(catalogUrl, false, false, tmpDir)
    then:
    // We correctly received the addons list
    addons != null
    addons.size() == NB_ADDONS_CATALOG_JSON
    // And the catalog cache is filled
    catalogCache.exists()
    catalogCache.text == new File(getTestDataDir(), "catalog.json").text
    cleanup:
    tmpDir.deleteDir()
  }

  def "One call of loadAddonsFromUrl offline without cache"() {
    setup:
    File tmpDir = File.createTempDir()
    URL catalogUrl = new URL(getWebServerRootUrl() + "/catalog.json")
    File catalogCache = new File(tmpDir, "${addonService.convertUrlToFilename(catalogUrl)}.json")
    when:
    List<Addon> addons = addonService.loadAddonsFromUrl(catalogUrl, true, true, tmpDir)
    then:
    // We receive an empty list of add-ons
    addons != null
    addons.size() == 0
    // And the catalog cache mustn't exist
    !catalogCache.exists()
    cleanup:
    tmpDir.deleteDir()
  }

  def "Two successive calls of loadAddonsFromUrl online with cache"() {
    setup:
    File tmpDir = File.createTempDir()
    File testCatalog = new File(getTestDataDir(), "tmp-catalog-with-cache.json")
    testCatalog << new File(getTestDataDir(), "catalog.json").text
    URL catalogUrl = new URL("${getWebServerRootUrl()}/${testCatalog.name}")
    File catalogCache = new File(tmpDir, "${addonService.convertUrlToFilename(catalogUrl)}.json")
    // We call it a first time
    addonService.loadAddonsFromUrl(catalogUrl, false, false, tmpDir)
    // We change the remote content
    testCatalog.withWriter { w ->
      w << new File(getTestDataDir(), "catalog.json").text
    }
    when:
    List<Addon> addons = addonService.loadAddonsFromUrl(catalogUrl, false, false, tmpDir)
    then:
    // We correctly received the add-ons list
    addons != null
    addons.size() == NB_ADDONS_CATALOG_JSON // The number of entries in catalog.json, we don't see the change in the remote catalog
    // And the catalog cache is filled
    catalogCache.exists()
    catalogCache.text == new File(getTestDataDir(), "catalog.json").text
    cleanup:
    tmpDir.deleteDir()
    testCatalog.delete()
  }

  def "Two successive calls of loadAddonsFromUrl online without cache"() {
    setup:
    File tmpDir = File.createTempDir()
    File testCatalog = new File(getTestDataDir(), "tmp-catalog-without-cache.json")
    testCatalog << new File(getTestDataDir(), "catalog.json").text
    URL catalogUrl = new URL("${getWebServerRootUrl()}/${testCatalog.name}")
    File catalogCache = new File(tmpDir, "${addonService.convertUrlToFilename(catalogUrl)}.json")
    // We call it a first time
    addonService.loadAddonsFromUrl(catalogUrl, false, false, tmpDir)
    // We change the remote content
    testCatalog.withWriter { w ->
      w << new File(getTestDataDir(), "catalog2.json").text
    }
    when:
    List<Addon> addons = addonService.loadAddonsFromUrl(catalogUrl, true, false, tmpDir)
    then:
    // We correctly received the add-ons list
    addons != null
    addons.size() == 1 // The number of entries in catalog2.json
    // And the catalog cache is filled
    catalogCache.exists()
    catalogCache.text == new File(getTestDataDir(), "catalog2.json").text
    cleanup:
    tmpDir.deleteDir()
    testCatalog.delete()
  }

  def "createAddonsFromJsonText can read a valid catalog"() {
    when:
    List<Addon> catalog = addonService.createAddonsFromJsonText(new File(getTestDataDir(), "catalog.json").text)
    then:
    catalog.size() == NB_ADDONS_CATALOG_JSON
  }

  def "createAddonsFromJsonText removes invalid entries"() {
    when:
    List<Addon> catalog = addonService.createAddonsFromJsonText(
        new File(getTestDataDir(), "catalog-with-invalid-entries.json").text)
    then:
    catalog.size() == 1
  }

  def "AM_CAT_07 : createAddonsFromJsonText removes duplicated entries (and keeps the first one)"() {
    when:
    List<Addon> catalog = addonService.createAddonsFromJsonText(
        new File(getTestDataDir(), "catalog-with-duplicated-entries.json").text)
    then:
    catalog.size() == 1
    catalog.first().name == "Fake addon - ONE"
  }

  def "createAddonsFromJsonText cannot read invalid JSON"() {
    when:
    addonService.createAddonsFromJsonText(new File(getTestDataDir(), "catalog-unreadable.json").text)
    then:
    thrown JsonException
  }

}
