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
import groovy.transform.Canonical
import groovy.util.slurpersupport.GPathResult
import groovy.xml.StreamingMarkupBuilder
import groovy.xml.XmlUtil
import org.eclipse.aether.util.version.GenericVersionScheme
import org.eclipse.aether.version.VersionScheme
import org.exoplatform.platform.am.ex.AddonNotFoundException
import org.exoplatform.platform.am.ex.CompatibilityException
import org.exoplatform.platform.am.ex.InvalidJSONException
import org.exoplatform.platform.am.ex.UnknownErrorException
import org.exoplatform.platform.am.settings.EnvironmentSettings
import org.exoplatform.platform.am.settings.PlatformSettings
import org.exoplatform.platform.am.utils.FileUtils
import org.exoplatform.platform.am.utils.Logger

import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

import static org.exoplatform.platform.am.AddonService.ParsingErrorType.INVALID_ENTRY
import static org.exoplatform.platform.am.AddonService.ParsingErrorType.MALFORMED_ENTRY
import static org.exoplatform.platform.am.utils.FileUtils.copyFile
import static org.exoplatform.platform.am.utils.FileUtils.downloadFile

/**
 * All services related to add-ons
 * @author Arnaud Héritier <aheritier@exoplatform.com>
 */
class AddonService {

  /**
   * Logger
   */
  private static final Logger LOG = Logger.getInstance()
  /**
   * The identifier used in the catalog for the add-ons manager
   */
  private static final String ADDONS_MANAGER_CATALOG_ID = "exo-addons-manager"

  private static final STATUS_FILE_EXT = ".status"

  /**
   * Version Scheme to compare/sort versions
   */
  private static final VersionScheme VERSION_SCHEME = new GenericVersionScheme()

  /**
   * Singleton
   */
  private static final AddonService singleton = new AddonService()

  /**
   * Factory
   * @return The {@link AddonService} singleton instance
   */
  static AddonService getInstance() {
    return singleton
  }

  /**
   * You should use the singleton
   */
  private AddonService() {
  }

  /**
   * Load add-ons from local and remote catalogs
   * @param env The execution environment
   * @param alternateCatalog The alternate remote catalog URL
   * @param noCache If the 1h cache must be used for the remote catalog
   * @param offline If the operation must be done offline (nothing will be downloaded)
   * @param allowSnapshot allow add-ons with snapshot version
   * @param allowUnstable allow add-ons with unstable version
   * @return a list of add-ons
   */
  protected List<Addon> loadAddons(
      EnvironmentSettings env,
      URL alternateCatalog,
      Boolean noCache,
      Boolean offline,
      Boolean allowSnapshot,
      Boolean allowUnstable) {
    URL remoteCatalogUrl = alternateCatalog ?: env.remoteCatalogUrl
    List<Addon> allAddons = mergeCatalogs(
        loadAddonsFromUrl(remoteCatalogUrl, noCache, offline, env.catalogsCacheDirectory),
        loadAddonsFromFile(env.localAddonsCatalogFile)
    )
    Addon newerAddonManager = findAddonsNewerThan(
        new Addon(id: ADDONS_MANAGER_CATALOG_ID, version: env.manager.version),
        filterAddonsByVersion(allAddons, true, false, false))?.max()
    if (newerAddonManager) {
      LOG.info(
          "New Add-ons Manager version @|yellow,bold ${newerAddonManager.version}|@ found. It will be automatically updated " +
              "after its restart.")
      // Backup the current library
      File backupDirectory = new File(env.versionsDirectory, env.manager.version)
      if (!backupDirectory.exists()) {
        FileUtils.mkdirs(backupDirectory)
      }
      copyFile("Backing up current add-ons manager library", new File(env.addonsDirectory, "addons-manager.jar"), new File(backupDirectory, "addons-manager.jar"), false)
      // Let's download the new one
      File newAddonsManagerArchive = new File(env.archivesDirectory, "${newerAddonManager.id}-${newerAddonManager.version}.zip")
      FileUtils.downloadFile("Downloading Add-ons Manager version @|yellow,bold ${newerAddonManager.version}|@", newerAddonManager.downloadUrl, newAddonsManagerArchive)
      LOG.withStatus("Extracting Add-ons Manager version @|yellow,bold ${newerAddonManager.version}|@") {
        ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(newAddonsManagerArchive))
        zipInputStream.withStream {
          ZipEntry entry
          while (entry = zipInputStream.nextEntry) {
            if (entry.name == "addons/addons-manager.jar") {
              FileOutputStream output = new FileOutputStream(new File(env.addonsDirectory, "addons-manager.jar.new"))
              output.withStream {
                int len = 0;
                byte[] buffer = new byte[4096]
                while ((len = zipInputStream.read(buffer)) > 0) {
                  output.write(buffer, 0, len);
                }
              }
            }
          }
        }
      }
    }
    return filterAddonsByVersion(
        allAddons.findAll { !ADDONS_MANAGER_CATALOG_ID.equals(it.id) },
        true,
        allowUnstable,
        allowSnapshot)
  }

  /**
   * Load add-ons list from a remote Url (JSON formatted)
   * @param catalogUrl The remote catalog URL
   * @param noCache If the 1h cache must be used for the remote catalog
   * @param offline If the operation must be done offline (nothing will be downloaded)
   * @param catalogCacheDir The directory where are stored catalogs caches
   * @return a list of Add-ons
   */
  protected List<Addon> loadAddonsFromUrl(
      URL catalogUrl,
      Boolean noCache,
      Boolean offline,
      File catalogCacheDir) {
    List<Addon> addons = new ArrayList<Addon>()
    String catalogContent
    File catalogCacheFile = new File(catalogCacheDir, "${convertUrlToFilename(catalogUrl)}.json");
    LOG.debug("Catalog cache file for ${catalogUrl} : ${catalogCacheFile}")
    if (noCache && catalogCacheFile.exists()) {
      // AM-102 : Let's drop the catalog cache with --no-cache
      catalogCacheFile.delete()
    }
    // If there is no local cache of the remote catalog or if it is older than 1h
    use([TimeCategory]) {
      if ((!catalogCacheFile.exists() || new Date(catalogCacheFile.lastModified()) < 1.hours.ago) && !offline) {
        // Load the remote list
        File tempFile
        try {
          // Create a temporary file in which we will download the remote catalog
          tempFile = File.createTempFile("addons-manager-remote-catalog", ".json", catalogCacheDir)
          // Don't forget to always delete it even in case of error
          tempFile.deleteOnExit()
          // Download the remote catalog
          downloadFile("Downloading catalog ${catalogUrl}", catalogUrl, tempFile)
          // Read the catalog content
          catalogContent = tempFile.text
        } catch (FileNotFoundException fne) {
          throw new UnknownErrorException("Catalog ${catalogUrl} not found", fne)
        }
        try {
          addons.addAll(createAddonsFromJsonText(catalogContent))
          // Everything was ok, let's store the cache
          copyFile("Updating cache for catalog ${catalogUrl}", tempFile, catalogCacheFile, false)
        } catch (groovy.json.JsonException je) {
          throw new InvalidJSONException("Invalid JSON content from URL : ${catalogUrl}", je)
        } finally {
          // Delete the temp file
          tempFile.delete()
        }
      } else {
        if (catalogCacheFile.exists()) {
          // Let's load add-ons from the cache
          LOG.withStatus("Reading catalog cache for ${catalogUrl}") {
            catalogContent = catalogCacheFile.text
          }
          try {
            addons.addAll(createAddonsFromJsonText(catalogContent))
          } catch (groovy.json.JsonException je) {
            catalogCacheFile.delete()
            throw new InvalidJSONException("Invalid JSON content in cache file : ${catalogCacheFile.name}. Deleting it.", je)
          }
        } else {
          LOG.warn("No remote catalog cache and offline mode activated")
        }
      }
    }
    return addons
  }

  /**
   * Load add-ons list from a local file (JSON formatted)
   * @param catalogFile The catalog file to read
   * @return a list of Add-ons. Empty if the file doesn't exist.
   */
  protected List<Addon> loadAddonsFromFile(
      File catalogFile) {
    List<Addon> addons = new ArrayList<Addon>()
    String catalogContent
    if (catalogFile.exists()) {
      LOG.debug("Loading catalog from ${catalogFile}")
      LOG.withStatus("Reading catalog ${catalogFile.name}") {
        catalogContent = catalogFile.text
      }
      try {
        addons.addAll(createAddonsFromJsonText(catalogContent))
      } catch (groovy.json.JsonException je) {
        throw new InvalidJSONException("Invalid JSON content in file : ${catalogFile}", je)
      }
    } else {
      LOG.debug("No local catalog to load from ${catalogFile}")
    }
    return addons
  }

  /**
   * Returns the list of add-ons installed in the current environment @{code env}.
   * @param env The environment where the add-on
   * must be uninstalled
   * @return A list of @{link Addon}
   */
  protected List<Addon> getInstalledAddons(
      EnvironmentSettings env) {
    List<Addon> result = new ArrayList<>();
    env.statusesDirectory.list(
        { dir, file ->
          file ==~ /.*?\${AddonService.STATUS_FILE_EXT}/
        } as FilenameFilter
    ).toList().each { statusFile ->
      try {
        LOG.withStatus("Loading add-on details from ${statusFile}") {
          result << createAddonFromJsonText(new File(env.statusesDirectory, statusFile).text)
        }
      } catch (InvalidJSONException ije) {
        LOG.debug(ije)
        LOG.warn("${statusFile} isn't readable")
      }
    }
    return result
  }

  /**
   * Returns the list of outdated add-ons by comparing the list of @{code installedAddons} with the one of
   * @{code availableAddons}.
   * @param installedAddons The list of installed add-ons
   * @param availableAddons The list of available add-ons
   * @return The list of outdated add-ons
   */
  protected List<Addon> getOutdatedAddons(
      List<Addon> installedAddons,
      List<Addon> availableAddons) {
    return installedAddons.findAll { installedAddon ->
      findAddonsNewerThan(installedAddon, availableAddons).size() > 0
    }
  }

  /**
   * Find in the @{code addons} list the one with the current @{code addonId} and @{code addonVersion}. If
   * @{code addonVersion} isn't set it will find the more recent version (stable per default excepted if @{code allowUnstable},
   * or @{code allowSnapshot} are set.
   * @param addons The list of add-ons in wich to do the search
   * @param addonId The Identifier of the add-on to find
   * @param addonVersion The version of the add-on to find
   * @param allowSnapshot allows add-ons with snapshot version Allow to retrieve a snapshot version if it is the most recent and
   * @{code addonVersion} isn't set
   * @param allowUnstable allows add-ons with snapshot version Allow to retrieve an unstable version if it is the most recent and
   * @{code addonVersion} isn't set
   * @return the add-on or null if not found
   */
  protected Addon findAddon(
      final List<Addon> addons,
      final String addonId,
      final String addonVersion,
      final Boolean allowSnapshots,
      final Boolean allowUnstable
  ) {
    // Let's find the add-on with the given id and version
    Addon result
    if (addonVersion == null) {
      // No version specified thus we need to find the newer version available
      // Let's find the first add-on with the given id (including or not snapshots depending of the option)
      result = findNewestAddon(addonId,
                               filterAddonsByVersion(addons, true, allowUnstable, allowSnapshots))
      if (result == null) {
        if (!addons.find { it.id == addonId }) {
          throw new AddonNotFoundException(addonId)
        } else {
          // Let's try to find an unstable version of the addon
          if (!allowUnstable && findNewestAddon(addonId,
                                                filterAddonsByVersion(addons, true, true, allowSnapshots))) {
            LOG.error(
                "This add-on exists but doesn't have a stable released version yet! add --unstable option to use an unstable version")
          }
          // Let's try to find a snapshot version of the add-on
          if (!allowSnapshots && findNewestAddon(addonId,
                                                 filterAddonsByVersion(addons, true, allowUnstable, true))) {
            LOG.error(
                "This add-on exists but doesn't have a stable released version yet! add --snapshots option to use a development version")
          }
          throw new AddonNotFoundException(addonId)
        }
      }
    } else {
      result = addons.find { it.id == addonId && it.version == addonVersion }
      if (result == null) {
        if (!addons.find { it.id == addonId }) {
          throw new AddonNotFoundException(addonId)
        } else {
          List<Addon> stableAddons = filterAddonsByVersion(addons.findAll { it.id == addonId }, true, false, false)
          if (!stableAddons.empty) {
            LOG.error "Stable version(s) available for add-on ${addonId} : ${stableAddons.sort().reverse().collect { it.version }.join(', ')}"
          }
          List<Addon> unstableAddons = filterAddonsByVersion(addons.findAll { it.id == addonId }, false, true, false)
          if (!unstableAddons.empty) {
            LOG.error "Unstable version(s) available for add-on ${addonId} : ${unstableAddons.sort().reverse().collect { it.version }.join(', ')}"
          }
          List<Addon> snapshotAddons = filterAddonsByVersion(addons.findAll { it.id == addonId }, false, false, true)
          if (!snapshotAddons.empty) {
            LOG.error "Development version(s) available for add-on ${addonId} : ${snapshotAddons.sort().reverse().collect { it.version }.join(', ')}"
          }
          throw new AddonNotFoundException(addonId, addonVersion)
        }
      }
    }
    return result
  }

  /**
   * Find in the list {@code addons} all add-ons with the same identifier {@link Addon#id} and a higher version number
   * {@link Addon#version} than {@code addonRef}
   * @param addonRef The add-on reference
   * @param addons The list to filter
   * @return A list of add-ons
   */
  protected List<Addon> findAddonsNewerThan(
      Addon addonRef,
      List<Addon> addons) {
    assert addonRef
    assert addonRef.id
    assert addonRef.version
    return addons.findAll { it.id == addonRef.id && it > addonRef }
  }

  /**
   * Find in the list {@code addons} the add-on with the identifier {@code addonId} and the highest version number
   * @param addonId The add-on identifier
   * @param addons The list to filter
   * @return The add-on matching constraints or null if none.
   */
  protected Addon findNewestAddon(
      String addonId,
      List<Addon> addons) {
    assert addonId
    return addons.findAll { it.id == addonId }.max()
  }

  /**
   * Filter entries in {@code addons} to keep only versions matching some criteria.
   * @param addons The list of add-ons to filter
   * @param allowStable Return add-ons with stable versions
   * @param allowUnstable Return add-ons with unstable versions (alpha, beta, RC, ...)
   * @param allowSnapshot Return add-ons with snapshot versions (-SNAPSHOT)
   * @return the filtered list of add-ons.
   */
  protected List<Addon> filterAddonsByVersion(
      List<Addon> addons,
      Boolean allowStable,
      Boolean allowUnstable,
      Boolean allowSnapshot
  ) {
    return addons.findAll {
      !it.unstable && !it.isSnapshot() && allowStable ||
          it.unstable && !it.isSnapshot() && allowUnstable ||
          it.isSnapshot() && allowSnapshot
    }
  }

  /**
   * Returns all add-ons supporting a platform instance (distributionType, appServerType, version)
   * @param addons The catalog to filter entries
   * @param plfSettings The settings about the platform instance
   * @return the filtered list
   */
  protected List<Addon> filterCompatibleAddons(
      final List<Addon> addons,
      PlatformSettings plfSettings) {
    return addons.findAll {
      isCompatible(it, plfSettings)
    }
  }

  /**
   * Verify if an add-on is compatible with the platform instance (distributionType, appServerType, version)
   * @param addon The add-on to verify
   * @param plfSettings The settings about the platform instance
   * @throws CompatibilityException if the add-on isn't compatible
   */
  protected void validateCompatibility(Addon addon, PlatformSettings plfSettings) throws CompatibilityException {
    LOG.withStatus("Checking compatibility of the add-on with your eXo platform instance") {
      if (!isCompatible(addon, plfSettings)) {
        throw new CompatibilityException(addon, plfSettings)
      }
    }
  }

  /**
   * Verify if an add-on is compatible with the platform instance (distributionType, appServerType, version)
   * @param addon The add-on to verify
   * @param plfSettings The settings about the platform instance
   * @return true is the add-on is compatible
   */
  protected Boolean isCompatible(Addon addon, PlatformSettings plfSettings) {
    return addon.supportedDistributions.contains(plfSettings.distributionType) &&
        testAppServerTypeCompatibility(plfSettings.appServerType, addon.supportedApplicationServers) &&
        testVersionCompatibility(plfSettings.version, addon.compatibility)
  }

  /**
   * [AM_CAT_07] At merge, de-duplication of add-on entries of the local and remote catalogs is
   * done using ID, Version, Distributions, Application Servers as the identifier.
   * In case of duplication, the remote entry takes precedence
   * TODO : Only ID+Version are used in comparison. It should take care of Distributions, Application Servers.
   * @param remoteCatalog
   * @param localCatalog
   * @return a list of add-ons
   */
  protected List<Addon> mergeCatalogs(
      final List<Addon> remoteCatalog,
      final List<Addon> localCatalog) {
    // Let's initiate a new list from the remote catalog content
    List<Addon> mergedCatalog = remoteCatalog.clone()
    if (localCatalog) {
      List<Addon> duplicatedEntries = new ArrayList<>();
      // Let's add entries from the local catalog which aren't already in the catalog (based on id+version identifiers)
      LOG.withStatus("Merging local and remote catalogs") {
        localCatalog.each {
          if (!mergedCatalog.contains(it)) {
            mergedCatalog.add(it)
          } else {
            duplicatedEntries.add(it)
          }
        }
      }
      if (duplicatedEntries) {
        duplicatedEntries.each {
          LOG.error("Ignored invalid entry ${it.id}:${it.version} in local catalog: already existing in remote catalog")
        }
      }
    }
    return mergedCatalog
  }

  /**
   * Parse a JSON String representing an Add-on to build an {@link Addon} object
   * @param text the JSON text to parse
   * @return an Addon object
   * @throws InvalidJSONException if there is at least one error while reading an add-on
   */
  protected Addon createAddonFromJsonText(String text) throws InvalidJSONException {
    Addon result
    ParsingErrors errors = new ParsingErrors();
    try {
      result = createAddonFromJsonObject(new JsonSlurper().parseText(text), errors)
    } finally {
      printMessages(errors)
    }
    return result
  }

  /**
   * Loads a list of Add-on from its JSON text representation
   * @param text The JSON text to parse
   * @return A List of add-ons
   */
  protected List<Addon> createAddonsFromJsonText(
      String text) {
    List<Addon> addonsList = new ArrayList<Addon>();
    ParsingErrors errors = new ParsingErrors();
    LOG.withStatus("Loading add-ons list") {
      new JsonSlurper().parseText(text).each { anAddon ->
        try {
          Addon addonToAdd = createAddonFromJsonObject(anAddon, errors)
          if (!addonsList.contains(addonToAdd)) {
            addonsList.add(addonToAdd)
          } else {
            errors.addInvalid("${addonToAdd.id}:${addonToAdd.version}", "Duplicated entry")
          }
        } catch (InvalidJSONException ije) {
          // skip it
        }
      }
    }
    printMessages(errors)
    return addonsList
  }

  /**
   * Loads an Add-on from its object representation created by the JsonSlurper
   * @param anAddon An Object built from JsonSlurper
   * @param errors Error messages to populate while reading
   * @return an Addon or null if there are some errors
   * @throws InvalidJSONException if there is at least one error while reading an add-on
   */
  protected Addon createAddonFromJsonObject(
      Object anAddon,
      ParsingErrors errors) throws InvalidJSONException {
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
    addonObj.supported = anAddon.supported
    if (anAddon.supportedDistributions instanceof String) {
      addonObj.supportedDistributions = anAddon.supportedDistributions.split(',').collect {
        String it ->
          try {
            PlatformSettings.DistributionType.valueOf(it.trim().toUpperCase())
          } catch (IllegalArgumentException iae) {
            errors.addMalformed("${addonObj.id}:${addonObj.version}", "Unknown distribution type <${it}>")
            PlatformSettings.DistributionType.UNKNOWN
          }
      }
    } else {
      addonObj.supportedDistributions = anAddon.supportedDistributions ? anAddon.supportedDistributions.collect {
        String it ->
          try {
            PlatformSettings.DistributionType.valueOf(it.trim().toUpperCase())
          } catch (IllegalArgumentException iae) {
            errors.addMalformed("${addonObj.id}:${addonObj.version}", "Unknown distribution type <${it}>")
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
            errors.addMalformed("${addonObj.id}:${addonObj.version}", "Unknown application server type <${it}>")
            PlatformSettings.AppServerType.UNKNOWN
          }
      }
    } else {
      addonObj.supportedApplicationServers = anAddon.supportedApplicationServers ? anAddon.supportedApplicationServers.collect {
        String it ->
          try {
            PlatformSettings.AppServerType.valueOf(it.trim().toUpperCase())
          } catch (IllegalArgumentException iae) {
            errors.addMalformed("${addonObj.id}:${addonObj.version}", "Unknown application server type <${it}>")
            PlatformSettings.AppServerType.UNKNOWN
          }
      } : []
    }
    addonObj.supportedApplicationServers.removeAll(PlatformSettings.AppServerType.UNKNOWN)
    addonObj.compatibility = anAddon.compatibility
    addonObj.installedLibraries = anAddon.installedLibraries
    addonObj.installedWebapps = anAddon.installedWebapps
    addonObj.installedProperties = anAddon.installedProperties
    addonObj.installedOthersFiles = anAddon.installedOthersFiles
    addonObj.overwrittenFiles = anAddon.overwrittenFiles
    if (!addonObj.id) {
      errors.addInvalid("${addonObj.id}:${addonObj.version}", "No id")
    }
    if (!addonObj.version) {
      errors.addInvalid("${addonObj.id}:${addonObj.version}", "No version")
    }
    if (!addonObj.name) {
      errors.addInvalid("${addonObj.id}:${addonObj.version}", "No name")
    }
    if (!addonObj.downloadUrl) {
      errors.addInvalid("${addonObj.id}:${addonObj.version}", "No downloadUrl")
    } else {
      try {
        new URL(addonObj.downloadUrl)
      } catch (MalformedURLException mue) {
        errors.addInvalid("${addonObj.id}:${addonObj.version}", "Invalid downloadUrl <${addonObj.downloadUrl}>")
      }
    }
    if (addonObj.sourceUrl) {
      try {
        new URL(addonObj.sourceUrl)
      } catch (MalformedURLException mue) {
        // Not critical. Just a debug error
        errors.addMalformed("${addonObj.id}:${addonObj.version}", "Invalid sourceUrl <${addonObj.sourceUrl}>")
      }
    }
    if (addonObj.screenshotUrl) {
      try {
        new URL(addonObj.screenshotUrl)
      } catch (MalformedURLException mue) {
        // Not critical. Just a debug error
        errors.addMalformed("${addonObj.id}:${addonObj.version}", "Invalid screenshotUrl <${addonObj.screenshotUrl}>")
      }
    }
    if (addonObj.thumbnailUrl) {
      try {
        new URL(addonObj.thumbnailUrl)
      } catch (MalformedURLException mue) {
        // Not critical. Just a debug error
        errors.addMalformed("${addonObj.id}:${addonObj.version}", "Invalid thumbnailUrl <${addonObj.thumbnailUrl}>")
      }
    }
    if (addonObj.documentationUrl) {
      try {
        new URL(addonObj.documentationUrl)
      } catch (MalformedURLException mue) {
        // Not critical. Just a debug error
        errors.addMalformed("${addonObj.id}:${addonObj.version}", "Invalid documentationUrl <${addonObj.documentationUrl}>")
      }
    }
    if (addonObj.licenseUrl) {
      try {
        new URL(addonObj.licenseUrl)
      } catch (MalformedURLException mue) {
        // Not critical. Just a debug error
        errors.addMalformed("${addonObj.id}:${addonObj.version}", "Invalid licenseUrl <${addonObj.licenseUrl}>")
      }
    }
    if (!addonObj.vendor) {
      errors.addInvalid("${addonObj.id}:${addonObj.version}", "No vendor")
    }
    if (!addonObj.license) {
      errors.addInvalid("${addonObj.id}:${addonObj.version}", "No license")
    }
    if (addonObj.supportedApplicationServers.size() == 0) {
      errors.addInvalid("${addonObj.id}:${addonObj.version}", "No supportedApplicationServers")
    }
    if (addonObj.supportedDistributions.size() == 0) {
      errors.addInvalid("${addonObj.id}:${addonObj.version}", "No supportedDistributions")
    }
    // Reject it only it is marked as invalid
    if (errors?.findAll { String key, List<ParsingError> value ->
      key == "${addonObj?.id}:${addonObj?.version}" && value?.findAll { ParsingError pe ->
        pe?.type == INVALID_ENTRY
      }?.size()
    }?.size()) {
      throw new InvalidJSONException(anAddon)
    }
    return addonObj
  }

  /**
   * Displays the list of @{code errors}.
   * @param errors The list of errors to display
   */
  void printMessages(ParsingErrors errors) {
    errors.each { id, msgs ->
      if (msgs.findAll { it.type == MALFORMED_ENTRY }) {
        LOG.warn(
            "Malformed descriptor ${id} : ${msgs.findAll { it.type == MALFORMED_ENTRY }*.content.join(', ')}")
      }
      if (msgs.findAll { it.type == INVALID_ENTRY }) {
        LOG.error(
            "Ignored invalid entry ${id} : ${msgs.findAll { it.type == INVALID_ENTRY }*.content.join(', ')}")
      }
    }
  }

  /**
   * Returns the local archive file of an add-on
   * @param archivesDirectory The archives directory
   * @param addon The add-on
   * @return a File (existing or not)
   */
  protected File getAddonLocalArchive(
      File archivesDirectory,
      Addon addon) {
    return new File(archivesDirectory, "${addon.id}-${addon.version}.zip")
  }

  /**
   * Returns the status File for a given add-on
   * @param statusesDirectory The directory where statuses are stored
   * @param addonId The identifier of the add-on to find
   * @return a File (existing or not)
   */
  protected File getAddonStatusFile(
      File statusesDirectory,
      String addonId) {
    return new File(statusesDirectory, "${addonId}${STATUS_FILE_EXT}")
  }

  /**
   * Returns the status File for a given add-on
   * @param statusesDirectory The directory where statuses are stored
   * @param addon The add-on to find
   * @return a File (existing or not)
   */
  protected File getAddonStatusFile(
      File statusesDirectory,
      Addon addon) {
    return getAddonStatusFile(statusesDirectory, addon.id)
  }

  /**
   * Returns the License File for a given add-on
   * @param statusesDirectory The directory where statuses are stored
   * @param addon The add-on to find
   * @return a File (existing or not)
   */
  protected File getAddonLicenseFile(
      File statusesDirectory,
      Addon addon) {
    new File(statusesDirectory, "${addon.id}-${convertUrlToFilename(new URL(addon.licenseUrl))}.license")
  }

  /**
   * Checks if the given add-on is installed
   * @param statusesDirectory The directory where are stored status files
   * @param addon The add-on to check
   * @return True if the add-on is installed (thus if its status file exists)
   */
  protected Boolean isAddonInstalled(
      File statusesDirectory,
      Addon addon) {
    return getAddonStatusFile(statusesDirectory, addon).exists()
  }

  /**
   * Serializes XML
   * @param xml The XML content
   * @return a String representation of the XML
   */
  protected String serializeXml(
      GPathResult xml) {
    XmlUtil.serialize(new StreamingMarkupBuilder().bind {
      mkp.yield xml
    })
  }

  /**
   * Applies a conversion on a text file
   * @param file The file to change
   * @param processText The conversion to apply
   */
  protected void processFileInplace(
      File file,
      Closure processText) {
    String text = file.text
    file.write(processText(text))
  }

  /**
   * Build the cache filename from the URL using a MD5 conversion
   * @param catalogUrl The catalog URL
   * @return The filename associated to the given URL
   */
  protected String convertUrlToFilename(
      URL catalogUrl) {
    return new BigInteger(1, MessageDigest.getInstance("MD5").digest(catalogUrl.toString().getBytes()))
        .toString(16).padLeft(32, "0").toUpperCase()
  }

  /**
   * Test if a version is compatible with the given constraint
   * @param version A version
   * @param constraint A constraint (range)
   * @return true if the version is compatible with the constraint
   */
  protected Boolean testVersionCompatibility(
      String version,
      String constraint
  ) {
    assert version
    !constraint || VERSION_SCHEME.parseVersionConstraint(constraint).containsVersion(VERSION_SCHEME.parseVersion(version))
  }

  /**
   * Test if the Application Server type is supported
   * @param appServerType An application server type
   * @param supportedServerType A list of supported server type
   */
  protected Boolean testAppServerTypeCompatibility(
      PlatformSettings.AppServerType appServerType,
      List<PlatformSettings.AppServerType> supportedServerType
  ) {
    return supportedServerType.contains(appServerType) || 
           supportedServerType.contains(PlatformSettings.AppServerType.TOMCAT) && appServerType.equals(PlatformSettings.AppServerType.BITNAMI)
  }

  private enum ParsingErrorType {
    INVALID_ENTRY, MALFORMED_ENTRY
  }

  /**
   * An inner class used to store messages
   */
  @Canonical
  private class ParsingError {
    ParsingErrorType type
    String content
  }

  @Canonical
  private class ParsingErrors {
    @Delegate
    Map<String, List<ParsingError>> errors = new TreeMap<>()

    void addMalformed(String identifier, String reason) {
      if (this.errors.containsKey(identifier)) {
        this.errors.get(identifier) << new ParsingError(MALFORMED_ENTRY, reason)
      } else {
        this.errors[identifier] = [new ParsingError(MALFORMED_ENTRY, reason)]
      }
    }

    void addInvalid(String identifier, String reason) {
      if (this.errors.containsKey(identifier)) {
        this.errors.get(identifier) << new ParsingError(INVALID_ENTRY, reason)
      } else {
        this.errors[identifier] = [new ParsingError(INVALID_ENTRY, reason)]
      }
    }

  }

}
