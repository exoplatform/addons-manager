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
import org.exoplatform.platform.am.settings.PlatformSettings
import org.exoplatform.platform.am.utils.AddonsManagerException
import org.exoplatform.platform.am.utils.FileUtils
import org.exoplatform.platform.am.utils.InvalidJSONException
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
   * The identifier used in the catalog for the addons manager
   */
  private static final String ADDONS_MANAGER_CATALOG_ID = "exo-addons-manager"

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
   * @param catalogsCacheDirectory The directory where are cached remote catalogs
   * @param offline If the operation must be done offline (nothing will be downloaded)
   * @param localCatalogFile The local catalog file
   * @param distributionType The distribution type which addons listed must be compatible with
   * @param appServerType The application seerver type which addons listed must be compatible with
   * @return a list of addons
   */
  List<Addon> loadAddons(URL remoteCatalogUrl,
                         boolean noCache,
                         File catalogsCacheDirectory,
                         boolean offline,
                         File localCatalogFile,
                         PlatformSettings.DistributionType distributionType,
                         PlatformSettings.AppServerType appServerType
  ) {
    return mergeCatalogs(
        loadAddonsFromUrl(remoteCatalogUrl, noCache, offline, catalogsCacheDirectory),
        loadAddonsFromFile(localCatalogFile),
        distributionType,
        appServerType).findAll { !ADDONS_MANAGER_CATALOG_ID.equals(it.id) }.sort().reverse()
  }

  /**
   * [AM_CAT_07] At merge, de-duplication of add-on entries of the local and remote catalogs is
   * done using ID, Version, Distributions, Application Servers as the identifier.
   * In case of duplication, the remote entry takes precedence
   * @param remoteCatalog
   * @param localCatalog
   * @param distributionType The distribution type which addons listed must be compatible with
   * @param appServerType The application seerver type which addons listed must be compatible with
   * @return a list of addons
   */
  protected List<Addon> mergeCatalogs(
      final List<Addon> remoteCatalog,
      final List<Addon> localCatalog,
      PlatformSettings.DistributionType distributionType,
      PlatformSettings.AppServerType appServerType) {
    // Let's keep on entries that are interesting us
    List<Addon> filteredCentralCatalog = filterCatalog(remoteCatalog, distributionType, appServerType)
    List<Addon> filteredLocalCatalog = filterCatalog(localCatalog, distributionType, appServerType)
    // Let's initiate a new list from the filtered list of the remote catalog
    List<Addon> mergedCatalog = filteredCentralCatalog.clone()
    // Let's add entries from the filtered local catalog which aren't already in the catalog (based on id+version identifiers)
    localCatalog.findAll { !mergedCatalog.contains(it) }.each { mergedCatalog.add(it) }
    return mergedCatalog
  }

  /**
   * Returns all add-ons supporting a distributionType+appServerType
   * @param catalog The catalog to filter entries
   * @param distributionType The distribution type to support
   * @param appServerType The application server type to support
   * @return
   */
  protected List<Addon> filterCatalog(
      final List<Addon> catalog,
      PlatformSettings.DistributionType distributionType,
      PlatformSettings.AppServerType appServerType) {
    return catalog.findAll {
      it.supportedDistributions.contains(distributionType) && it.supportedApplicationServers.contains(appServerType)
    }
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
      try {
        LOG.withStatus("Loading add-ons list") {
          addons.addAll(parseJSONAddonsList(catalogContent))
        }
      } catch (groovy.json.JsonException je) {
        LOG.warn("Invalid JSON content in file : ${catalogFile}", je)
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
    // If there is no local cache of the remote catalog or if it is older than 1h
    use([TimeCategory]) {
      if ((noCache || !catalogCacheFile.exists() ||
          new Date(catalogCacheFile.lastModified()) < 1.hours.ago)
          && !offline
      ) {
        LOG.debug("Loading catalog from ${catalogUrl}")
        // Load the remote list
        File tempFile
        LOG.withStatus("Downloading catalog ${catalogUrl}") {
          try {
            // Create a temporary file in which we will download the remote catalog
            tempFile = File.createTempFile("addons-manager-remote-catalog", ".json", catalogCacheDir)
            // Don't forget to always delete it even in case of error
            tempFile.deleteOnExit()
            // Download the remote catalog
            FileUtils.downloadFile(catalogUrl, tempFile)
            // Read the catalog content
            catalogContent = tempFile.text
          } catch (FileNotFoundException fne) {
            throw new AddonsManagerException("Catalog ${catalogUrl} not found", fne)
          }
        }
        try {
          LOG.withStatus("Loading add-ons list") {
            addons.addAll(parseJSONAddonsList(catalogContent))
          }
          // Everything was ok, let's store the cache
          LOG.withStatus("Updating local cache") {
            FileUtils.copyFile(tempFile, catalogCacheFile, false)
          }
        } catch (groovy.json.JsonException je) {
          LOG.warn("Invalid JSON content from URL : ${catalogUrl}", je)
        } finally {
          // Delete the temp file
          tempFile.delete()
        }
      } else {
        if (catalogCacheFile.exists()) {
          // Let's load add-ons from the cache
          LOG.debug("Loading remote catalog from cache ${catalogCacheFile}")
          LOG.withStatus("Reading catalog cache for ${catalogUrl}") {
            catalogContent = catalogCacheFile.text
          }
          try {
            LOG.withStatus("Loading add-ons list") {
              addons.addAll(parseJSONAddonsList(catalogContent))
            }
          } catch (groovy.json.JsonException je) {
            LOG.warn("Invalid JSON content in cache file : ${catalogCacheFile}. Deleting it.", je)
            catalogCacheFile.delete()
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
        id: anAddon.id,
        version: anAddon.version);
    addonObj.unstable = anAddon.unstable
    addonObj.name = anAddon.name
    addonObj.description = anAddon.description
    addonObj.releaseDate = anAddon.releaseDate
    addonObj.sourceUrl = anAddon.sourceUrl
    addonObj.screenshotUrl = anAddon.screenshotUrl
    addonObj.thumbnailUrl = anAddon.thumbnailUrl
    addonObj.documentationUrl = anAddon.documentationUrl
    addonObj.downloadUrl = anAddon.downloadUrl
    addonObj.vendor = anAddon.vendor
    addonObj.author = anAddon.author
    addonObj.authorEmail = anAddon.authorEmail
    addonObj.license = anAddon.license
    addonObj.licenseUrl = anAddon.licenseUrl
    addonObj.mustAcceptLicense = anAddon.mustAcceptLicense
    if (anAddon.supportedDistributions instanceof String) {
      addonObj.supportedDistributions = anAddon.supportedDistributions.split(',').collect {
        String it ->
          try {
            PlatformSettings.DistributionType.valueOf(it.trim().toUpperCase())
          } catch (IllegalArgumentException iae) {
            LOG.debug("Unknown distribution type for add-on ${addonObj} : ${it}")
            PlatformSettings.DistributionType.UNKNOWN
          }
      }
    } else {
      addonObj.supportedDistributions = anAddon.supportedDistributions ? anAddon.supportedDistributions.collect {
        String it ->
          try {
            PlatformSettings.DistributionType.valueOf(it.trim().toUpperCase())
          } catch (IllegalArgumentException iae) {
            LOG.debug("Unknown distribution type for add-on ${addonObj} : ${it}")
            PlatformSettings.DistributionType.UNKNOWN
          }
      } : []
    }
    addonObj.supportedDistributions.removeAll(PlatformSettings.DistributionType.UNKNOWN)
    if (anAddon.supportedApplicationServers instanceof String) {
      addonObj.supportedApplicationServers = anAddon.supportedApplicationServers.split(',').collect {
        String it ->
          try {
            PlatformSettings.AppServerType.valueOf(it.trim().toUpperCase())
          }
          catch (IllegalArgumentException iae) {
            LOG.debug("Unknown application server type for add-on ${addonObj} : ${it}")
            PlatformSettings.AppServerType.UNKNOWN
          }
      }
    } else {
      addonObj.supportedApplicationServers = anAddon.supportedApplicationServers ? anAddon.supportedApplicationServers.collect {
        String it ->
          try {
            PlatformSettings.AppServerType.valueOf(it.trim().toUpperCase())
          } catch (IllegalArgumentException iae) {
            LOG.debug("Unknown application server type for add-on ${addonObj} : ${it}")
            PlatformSettings.AppServerType.UNKNOWN
          }
      } : []
    }
    addonObj.supportedApplicationServers.removeAll(PlatformSettings.AppServerType.UNKNOWN)
    addonObj.compatibility = anAddon.compatibility
    addonObj.installedLibraries = anAddon.installedLibraries
    addonObj.installedWebapps = anAddon.installedWebapps
    int errors = 0
    if (!addonObj.id) {
      LOG.debug("No id for add-on ${anAddon}")
      errors++
    }
    if (!addonObj.version) {
      LOG.debug("No version for add-on ${anAddon}")
      errors++
    }
    if (!addonObj.name) {
      LOG.debug("No name for add-on ${anAddon}")
      errors++
    }
    if (!addonObj.downloadUrl) {
      LOG.debug("No downloadUrl for add-on ${anAddon}")
      errors++
    } else {
      try {
        new URL(addonObj.downloadUrl)
      } catch (MalformedURLException mue) {
        LOG.debug("Invalid downloadUrl for add-on ${anAddon}")
        errors++
      }
    }
    if (addonObj.sourceUrl) {
      try {
        new URL(addonObj.sourceUrl)
      } catch (MalformedURLException mue) {
        // Not critical. Just a debug error
        LOG.debug("Invalid sourceUrl for add-on ${anAddon}")
      }
    }
    if (addonObj.screenshotUrl) {
      try {
        new URL(addonObj.screenshotUrl)
      } catch (MalformedURLException mue) {
        // Not critical. Just a debug error
        LOG.debug("Invalid screenshotUrl for add-on ${anAddon}")
      }
    }
    if (addonObj.thumbnailUrl) {
      try {
        new URL(addonObj.thumbnailUrl)
      } catch (MalformedURLException mue) {
        // Not critical. Just a debug error
        LOG.debug("Invalid thumbnailUrl for add-on ${anAddon}")
      }
    }
    if (addonObj.documentationUrl) {
      try {
        new URL(addonObj.documentationUrl)
      } catch (MalformedURLException mue) {
        // Not critical. Just a debug error
        LOG.debug("Invalid documentationUrl for add-on ${anAddon}")
      }
    }
    if (addonObj.licenseUrl) {
      try {
        new URL(addonObj.licenseUrl)
      } catch (MalformedURLException mue) {
        // Not critical. Just a debug error
        LOG.debug("Invalid licenseUrl for add-on ${anAddon}")
      }
    }
    if (!addonObj.vendor) {
      LOG.debug("No vendor for add-on ${anAddon}")
      errors++
    }
    if (!addonObj.license) {
      LOG.debug("No license for add-on ${anAddon}")
      errors++
    }
    if (addonObj.supportedApplicationServers.size() == 0) {
      LOG.debug("No supportedApplicationServers for add-on ${anAddon}")
      errors++
    }
    if (addonObj.supportedDistributions.size() == 0) {
      LOG.debug("No supportedDistributions for add-on ${anAddon}")
      errors++
    }
    if (errors > 0) {
      throw new InvalidJSONException(anAddon)
    }
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
      try {
        addonsList.add(fromJSON(anAddon))
      } catch (InvalidJSONException ije) {
        LOG.debug(ije.message)
      }
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
