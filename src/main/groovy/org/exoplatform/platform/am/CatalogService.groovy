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

import groovy.json.JsonSlurper
import groovy.time.TimeCategory
import org.exoplatform.platform.am.utils.AddonsManagerException
import org.exoplatform.platform.am.utils.FileUtils
import org.exoplatform.platform.am.utils.Logger

import java.security.MessageDigest

/**
 * @author Arnaud Héritier <aheritier@exoplatform.com>
 */
class CatalogService {

  /**
   * Logger
   */
  private static final Logger LOG = Logger.get()

  /**
   * Parse a JSON String representing an Add-on to build an {@link Addon} object
   * @param text the JSON text to parse
   * @return an Addon object
   */
  Addon parseJSONAddon(String text) {
    return fromJSON(new JsonSlurper().parseText(text))
  }

  /**
   * Merge addons loaded from a remote and a local catalog
   * @param remoteCatalogUrl The remote catalog URL
   * @param noCache If the 1h cache must be used for the remote catalog
   * @param offline If the operation must be done offline (nothing will be downloaded)
   * @param localCatalogFile The local catalog file
   * @param catalogsCacheDirectory The directory where are cached remote catalogs
   * @return
   */
  List<Addon> loadAddons(URL remoteCatalogUrl, boolean noCache,
                         File catalogsCacheDirectory, boolean offline,
                         File localCatalogFile
  ) {
    return mergeCatalogs(
        loadAddonsFromUrl(remoteCatalogUrl, noCache, offline, catalogsCacheDirectory),
        loadAddonsFromFile(localCatalogFile))
  }

  /**
   * [AM_CAT_07] At merge, de-duplication of add-on entries of the local and remote catalogs is
   * done using ID, Version, Distributions, Application Servers as the identifier.
   * In case of duplication, the remote entry takes precedence
   * @param centralCatalog
   * @param localCatalog
   * @return
   */
  protected List<Addon> mergeCatalogs(final List<Addon> centralCatalog, final List<Addon> localCatalog) {
    List<Addon> mergedCatalog = centralCatalog.clone()
    localCatalog.findAll { !centralCatalog.contains(it) }.each { mergedCatalog.add(it) }
    return mergedCatalog
  }

  /**
   * Load add-ons list from a local file (JSON formatted)
   * @param catalogFile
   * @return a list of Add-ons. Empty if the file doesn't exist.
   */
  protected List<Addon> loadAddonsFromFile(File catalogFile) {
    List<Addon> addons = new ArrayList<Addon>()
    String catalogContent
    if (catalogFile.exists()) {
      LOG.debug("Loading catalog from ${catalogFile}")
      LOG.withStatus("Reading catalog ${catalogFile}") {
        catalogContent = catalogFile.text
      }
      LOG.withStatus("Loading add-ons list") {
        addons.addAll(parseJSONAddonsList(catalogContent))
      }
    } else {
      LOG.debug("No local catalog to load from ${catalogFile}")
    }
    return addons
  }

  /**
   * Load add-ons list from a remote Url (JSON formatted)
   * @param catalogUrl
   * @param noCache
   * @param offline
   * @param catalogCacheDir
   * @return a list of Add-ons
   */
  protected List<Addon> loadAddonsFromUrl(
      URL catalogUrl,
      boolean noCache,
      boolean offline,
      File catalogCacheDir) {
    List<Addon> addons = new ArrayList<Addon>()
    String catalogContent
    File catalogCacheFile = new File(catalogCacheDir, getCacheFilename(catalogUrl));
    LOG.debug("Remote catalog cache file for ${catalogUrl} : ${catalogCacheFile}")
    // If there is no local cache of the central catalog or if it is older than 1h
    use([TimeCategory]) {
      if ((noCache || !catalogCacheFile.exists() ||
          new Date(catalogCacheFile.lastModified()) < 1.hours.ago)
          && !offline
      ) {
        LOG.debug("Loading catalog from ${catalogUrl}")
        // Load the central list
        File tempFile
        LOG.withStatus("Downloading catalog ${catalogUrl}") {
          try {
            // Create a temporary file in which we will download the central catalog
            tempFile = File.createTempFile("addons-manager-central-catalog", ".json", catalogCacheDir)
            // Don't forget to always delete it even in case of error
            tempFile.deleteOnExit()
            // Download the central catalog
            FileUtils.downloadFile(catalogUrl, tempFile)
            // Read the catalog content
            catalogContent = tempFile.text
          } catch (FileNotFoundException fne) {
            throw new AddonsManagerException("Catalog ${catalogUrl} not found", fne)
          }
        }
        LOG.withStatus("Loading add-ons list") {
          addons.addAll(parseJSONAddonsList(catalogContent))
        }
        if (!noCache) {
          // Everything was ok, let's store the cache
          LOG.withStatus("Updating local cache") {
            FileUtils.copyFile(tempFile, catalogCacheFile, false)
          }
        }
      } else {
        if (catalogCacheFile.exists()) {
          // Let's load add-ons from the cache
          LOG.debug("Loading central catalog from cache ${catalogCacheFile}")
          LOG.withStatus("Reading catalog cache for ${catalogUrl}") {
            catalogContent = catalogCacheFile.text
          }
          LOG.withStatus("Loading add-ons list") {
            catalogContent = catalogCacheFile.text
            addons.addAll(parseJSONAddonsList(catalogContent))
          }
        } else {
          LOG.warn("No remote catalog cache and offline mode activated")
        }
      }
    }
    return addons
  }

  /**
   * Loads an Addon from its object representation created by the JsonSlurper
   * @param anAddon An Object built from JsonSlurper
   * @return an Addon
   */
  protected Addon fromJSON(anAddon) {
    Addon addonObj = new Addon(
        id: anAddon.id ? anAddon.id : 'N/A',
        version: anAddon.version ? anAddon.version : 'N/A');
    addonObj.unstable = anAddon.unstable ? anAddon.unstable : Boolean.FALSE
    addonObj.name = anAddon.name ? anAddon.name : 'N/A'
    addonObj.description = anAddon.description ? anAddon.description : 'N/A'
    addonObj.releaseDate = anAddon.releaseDate ? anAddon.releaseDate : 'N/A'
    addonObj.sourceUrl = anAddon.sourceUrl ? anAddon.sourceUrl : 'N/A'
    addonObj.screenshotUrl = anAddon.screenshotUrl ? anAddon.screenshotUrl : 'N/A'
    addonObj.thumbnailUrl = anAddon.thumbnailUrl ? anAddon.thumbnailUrl : 'N/A'
    addonObj.documentationUrl = anAddon.documentationUrl ? anAddon.documentationUrl : 'N/A'
    addonObj.downloadUrl = anAddon.downloadUrl ? anAddon.downloadUrl : 'N/A'
    addonObj.vendor = anAddon.vendor ? anAddon.vendor : 'N/A'
    addonObj.author = anAddon.author ? anAddon.author : 'N/A'
    addonObj.authorEmail = anAddon.authorEmail ? anAddon.authorEmail : 'N/A'
    addonObj.license = anAddon.license ? anAddon.license : 'N/A'
    addonObj.licenseUrl = anAddon.licenseUrl ? anAddon.licenseUrl : 'N/A'
    addonObj.mustAcceptLicense = anAddon.mustAcceptLicense ? anAddon.mustAcceptLicense : 'N/A'
    if (anAddon.supportedDistributions instanceof String) {
      addonObj.supportedDistributions = anAddon.supportedDistributions.split(',')
    } else {
      addonObj.supportedDistributions = anAddon.supportedDistributions ? anAddon.supportedDistributions : []
    }
    if (anAddon.supportedApplicationServers instanceof String) {
      addonObj.supportedApplicationServers = anAddon.supportedApplicationServers.split(',')
    } else {
      addonObj.supportedApplicationServers = anAddon.supportedApplicationServers ? anAddon.supportedApplicationServers : []
    }
    addonObj.compatibility = anAddon.compatibility ? anAddon.compatibility : 'N/A'
    addonObj.installedLibraries = anAddon.installedLibraries ? anAddon.installedLibraries : []
    addonObj.installedWebapps = anAddon.installedWebapps ? anAddon.installedWebapps : []
    // TODO : Add some validations here
    return addonObj
  }

  /**
   * Loads a list of Addon from its JSON text representation
   * @param text The JSON text to parse
   * @return A List of addons
   */
  protected List<Addon> parseJSONAddonsList(String text) {
    List<Addon> addonsList = new ArrayList<Addon>();
    new JsonSlurper().parseText(text).each { anAddon ->
      addonsList.add(fromJSON(anAddon))
    }
    return addonsList
  }

  /**
   * Build the cache filename from the URL using a MD5 conversion
   * @param catalogUrl The catalog URL
   * @return The filename associated to the given URL
   */
  protected String getCacheFilename(URL catalogUrl) {
    return new BigInteger(1, MessageDigest.getInstance("MD5").digest(catalogUrl.toString().getBytes()))
        .toString(16).padLeft(32, "0").toUpperCase() + ".json"
  }

}
