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
import groovy.json.StreamingJsonBuilder
import groovy.time.TimeCategory
import groovy.util.slurpersupport.GPathResult
import groovy.xml.StreamingMarkupBuilder
import groovy.xml.XmlUtil
import org.eclipse.aether.util.version.GenericVersionScheme
import org.eclipse.aether.version.Version
import org.eclipse.aether.version.VersionConstraint
import org.eclipse.aether.version.VersionScheme
import org.exoplatform.platform.am.cli.CommandLineParameters
import org.exoplatform.platform.am.settings.EnvironmentSettings
import org.exoplatform.platform.am.settings.PlatformSettings
import org.exoplatform.platform.am.utils.*

import java.security.MessageDigest
import java.text.SimpleDateFormat

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
   * The identifier used in the catalog for the addons manager
   */
  private static final String ADDONS_MANAGER_CATALOG_ID = "exo-addons-manager"

  private static final STATUS_FILE_EXT = ".status"

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

  private VersionScheme versionScheme = new GenericVersionScheme()

  /**
   * You should use the singleton
   */
  private AddonService() {
  }

  /**
   * List add-ons given the current environment {@code env} and command line {@code parameters}.
   * @param env The environment
   * @param parameters Command line parameters
   * @return a return code defined in {@link AddonsManagerConstants}
   */
  int listAddons(EnvironmentSettings env, CommandLineParameters.ListCommandParameters parameters) {
    if (parameters.installed) {
      return listInstalledAddons(env)
    } else if (parameters.outdated) {
      return listOutdatedAddons(
          env,
          parameters.unstable,
          parameters.snapshots,
          parameters.noCache,
          parameters.offline,
          parameters.catalog)
    } else {
      return listAddonsFromCatalogs(
          env,
          parameters.unstable,
          parameters.snapshots,
          parameters.noCache,
          parameters.offline,
          parameters.catalog)
    }
  }

  /**
   * Describe an add-on given the current environment {@code env} and command line {@code parameters}.
   * @param env The environment
   * @param parameters Command line parameters
   * @return a return code defined in {@link AddonsManagerConstants}
   */
  int describeAddon(
      EnvironmentSettings env,
      CommandLineParameters.InfoCommandParameters parameters) {
    List<Addon> availableAddons = loadAddons(
        parameters.catalog ?: env.remoteCatalogUrl,
        parameters.noCache,
        env.catalogsCacheDirectory,
        parameters.offline,
        env.localAddonsCatalogFile,
        env.platform.distributionType,
        env.platform.appServerType)
    Addon addon = findAddon(
        availableAddons,
        parameters.addonId,
        parameters.addonVersion,
        parameters.snapshots,
        parameters.unstable)
    if (addon == null) {
      return AddonsManagerConstants.RETURN_CODE_ADDON_NOT_FOUND
    } else {
      displayAddon(addon)
      return AddonsManagerConstants.RETURN_CODE_OK
    }
  }

  /**
   * Install an add-on given the current environment {@code env} and command line {@code parameters}.
   * @param env The environment
   * @param parameters Command line parameters
   * @return a return code defined in {@link AddonsManagerConstants}
   */
  int installAddon(
      EnvironmentSettings env,
      CommandLineParameters.InstallCommandParameters parameters) {
    List<Addon> availableAddons = loadAddons(
        parameters.catalog ?: env.remoteCatalogUrl,
        parameters.noCache,
        env.catalogsCacheDirectory,
        parameters.offline,
        env.localAddonsCatalogFile,
        env.platform.distributionType,
        env.platform.appServerType)
    Addon addon = findAddon(
        availableAddons,
        parameters.addonId,
        parameters.addonVersion,
        parameters.snapshots,
        parameters.unstable)
    if (addon == null) {
      return AddonsManagerConstants.RETURN_CODE_ADDON_NOT_FOUND
    } else {
      installAddon(env, addon, parameters.force, parameters.noCache, parameters.offline, parameters.noCompat)
      return AddonsManagerConstants.RETURN_CODE_OK
    }
  }

  /**
   * Uninstall an add-on given the current environment {@code env} and command line {@code parameters}.
   * @param env The environment
   * @param parameters Command line parameters
   * @return a return code defined in {@link AddonsManagerConstants}
   */
  int uninstallAddon(EnvironmentSettings env, CommandLineParameters.UninstallCommandParameters parameters) {
    File statusFile = getAddonStatusFile(env.statusesDirectory, parameters.addonId)
    if (statusFile.exists()) {
      Addon addon
      LOG.withStatus("Loading add-on details") {
        addon = parseJSONAddon(statusFile.text);
      }
      uninstallAddon(env, addon)
      return AddonsManagerConstants.RETURN_CODE_OK
    } else {
      LOG.error("Add-on not installed. It cannot be uninstalled.")
      return AddonsManagerConstants.RETURN_CODE_ADDON_NOT_INSTALLED
    }
  }

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
                               filterAddonsByVersion(addons, allowSnapshots, allowUnstable))
      if (result == null) {
        if (!addons.find { it.id == addonId }) {
          LOG.error "No add-on with identifier ${addonId} found in local or remote catalogs, check your add-on identifier"
        } else {
          LOG.error "No add-on with identifier ${addonId} found in local or remote catalogs"
          // Let's try to find an unstable version of the addon
          if (!allowUnstable && findNewestAddon(addonId,
                                                filterAddonsByVersion(addons, allowSnapshots, true))) {
            LOG.error(
                "This add-on exists but doesn't have a stable released version yet! add --unstable option to use an unstable version")
          }
          // Let's try to find a snapshot version of the addon
          if (!allowSnapshots && findNewestAddon(addonId,
                                                 filterAddonsByVersion(addons, true, allowUnstable))) {
            LOG.error(
                "This add-on exists but doesn't have a stable released version yet! add --snapshots option to use a development version")
          }
        }
      }
    } else {
      result = addons.find { it.id == addonId && it.version == addonVersion }
      if (result == null) {
        if (!addons.find { it.id == addonId }) {
          LOG.error "No add-on with identifier ${addonId} found in local or remote catalogs, check your add-on identifier"
        } else {
          LOG.error "No add-on with identifier ${addonId} and version ${addonVersion} found in local or remote catalogs"
          List<Addon> stableAddons = filterAddonsByVersion(addons.findAll { it.id == addonId }, false, false)
          if (!stableAddons.empty) {
            LOG.error "Stable version(s) currently available : ${stableAddons.sort().reverse().collect { it.version }.join(', ')}"
          }
          List<Addon> unstableAddons = filterAddonsByVersion(addons.findAll { it.id == addonId }, false, true)
          if (!unstableAddons.empty) {
            LOG.error "Unstable version(s) currently available : ${unstableAddons.sort().reverse().collect { it.version }.join(', ')}"
          }
          List<Addon> snapshotAddons = filterAddonsByVersion(addons.findAll { it.id == addonId }, false, false)
          if (!snapshotAddons.empty) {
            LOG.error "Development version(s) currently available : ${snapshotAddons.sort().reverse().collect { it.version }.join(', ')}"
          }
        }
      }
    }
    return result
  }

  protected void displayAddon(final Addon addon) {
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd")
    LOG.infoHR("=")
    LOG.info "Informations about add-on @|bold,yellow ${addon.id}|@@|bold :${addon.version}|@"
    LOG.infoHR()
    Map map = ["Identifier"                        : addon.id,
               "Version"                           : addon.version,
               "Name"                              : addon.name,
               "Description"                       : addon.description,
               "Release date (YYYY-MM-DD)"         : addon.releaseDate ? sdf.format(sdf.parse(addon.releaseDate)) : null,
               "Sources URL"                       : addon.sourceUrl ? URLDecoder.decode(addon.sourceUrl, "UTF-8") : null,
               "Screenshot URL"                    : addon.screenshotUrl ? URLDecoder.decode(addon.screenshotUrl, "UTF-8") : null,
               "Thumbnail URL"                     : addon.thumbnailUrl ? URLDecoder.decode(addon.thumbnailUrl, "UTF-8") : null,
               "Documentation URL"                 : addon.documentationUrl ? URLDecoder.decode(addon.documentationUrl,
                                                                                                "UTF-8") : null,
               "Download URL"                      : addon.downloadUrl ? URLDecoder.decode(addon.downloadUrl, "UTF-8") : null,
               "Vendor"                            : addon.vendor,
               "Author"                            : addon.author,
               "Author email"                      : addon.authorEmail,
               "License"                           : addon.license,
               "License URL"                       : addon.licenseUrl ? URLDecoder.decode(addon.licenseUrl, "UTF-8") : null,
               "License must be accepted"          : addon.mustAcceptLicense,
               "Supported application Server(s)"   : addon.supportedApplicationServers,
               "Supported platform distribution(s)": addon.supportedDistributions,
               "Supported platform version(s)"     : addon.compatibility] as LinkedHashMap
//LinkedHashMap to keep the insertion order
    map.keySet().findAll { map.get(it) }.each {
      LOG.info String.format("@|bold %-${map.keySet()*.size().max()}s|@ : @|bold,yellow %s|@", it, map.get(it))
    }
    LOG.infoHR("=")
  }

  protected int listInstalledAddons(EnvironmentSettings env) {
    // Display only installed add-ons
    List<Addon> installedAddons = getInstalledAddonsList(env)
    if (installedAddons.size() > 0) {
      LOG.info "\n@|bold Installed add-ons:|@"
      installedAddons.each {
        LOG.info String.format(
            "\n+ @|bold,yellow %-${installedAddons.id*.size().max() + installedAddons.version*.size().max()}s|@ : @|bold %s|@, %s",
            "${it.id} ${it.version}", it.name, it.description)
      }
      LOG.info String.format("""
To uninstall an add-on:
    ${env.manager.scriptName} uninstall @|yellow <addonId>|@
  """)
    } else {
      LOG.info "No add-on installed"
    }
    return AddonsManagerConstants.RETURN_CODE_OK
  }

  protected int listOutdatedAddons(
      EnvironmentSettings env,
      Boolean allowUnstable,
      Boolean allowSnapshot,
      Boolean noCache,
      Boolean offline,
      URL alternateCatalog
  ) {
    List<Addon> installedAddons = getInstalledAddonsList(env)
    if (installedAddons.size() > 0) {
      List<Addon> availableAddons = loadAddons(
          alternateCatalog ?: env.remoteCatalogUrl,
          noCache,
          env.catalogsCacheDirectory,
          offline,
          env.localAddonsCatalogFile,
          env.platform.distributionType,
          env.platform.appServerType,
          allowSnapshot,
          allowUnstable)
      List<Addon> outdatedAddons = getOutdatedAddonsList(installedAddons, availableAddons)
      if (outdatedAddons.size() > 0) {
        LOG.info "\n@|bold Outdated add-ons:|@"
        outdatedAddons.groupBy { it.id }.sort().each {
          Addon anAddon = it.value.first()
          LOG.info String.format(
              "\n+ @|bold,yellow %-${outdatedAddons.id*.size().max() + outdatedAddons.version*.size().max() + 1}s|@ : @|bold %s|@, %s",
              "${anAddon.id} ${anAddon.version}", anAddon.name, anAddon.description)
          LOG.info String.format(
              "     Newest Version(s) : %s",
              findNewerAddons(anAddon, availableAddons).sort().reverse().collect {
                newestAddon -> "@|yellow ${newestAddon.version}|@"
              }.join(', '))
        }
        LOG.info String.format("""
    To update an add-on:
        ${env.manager.scriptName} install @|yellow <addonId:[version]>|@ --force
      """)
      } else {
        LOG.info "No outdated add-on found"
      }
    } else {
      LOG.info "No add-on installed"
    }
    return AddonsManagerConstants.RETURN_CODE_OK
  }

  protected int listAddonsFromCatalogs(
      EnvironmentSettings env,
      Boolean allowUnstable,
      Boolean allowSnapshot,
      Boolean noCache,
      Boolean offline,
      URL alternateCatalog
  ) {
    // Display add-ons in remote+local catalogs
    List<Addon> availableAddons = loadAddons(
        alternateCatalog ?: env.remoteCatalogUrl,
        noCache,
        env.catalogsCacheDirectory,
        offline,
        env.localAddonsCatalogFile,
        env.platform.distributionType,
        env.platform.appServerType,
        allowSnapshot,
        allowUnstable)
    if (availableAddons.size() > 0) {
      LOG.info "\n@|bold Available add-ons:|@"
      availableAddons.groupBy { it.id }.sort().each {
        Addon anAddon = it.value.first()
        LOG.info String.format("\n+ @|bold,yellow %-${availableAddons.id*.size().max()}s|@ : @|bold %s|@, %s", anAddon.id,
                               anAddon.name, anAddon.description)
        LOG.info String.format("     Available Version(s) : %s", it.value.sort().reverse().collect { "@|yellow ${it.version}|@" }
            .join(', '))
      }
      LOG.info String.format("""
To install an add-on:
    ${env.manager.scriptName} install @|yellow <addonId:[version]>|@
  """)
    } else {
      LOG.warn "No add-on found in remote and local catalogs"
    }
    return AddonsManagerConstants.RETURN_CODE_OK
  }

  protected void installAddon(
      EnvironmentSettings env,
      Addon addon,
      Boolean force,
      Boolean noCache,
      Boolean offline,
      Boolean noCompat) {
    // if a compatibility rule is defined
    if (addon.compatibility && !noCompat) {
      Version plfVersion = versionScheme.parseVersion(env.platform.version)
      VersionConstraint addonConstraint = versionScheme.parseVersionConstraint(addon.compatibility)
      LOG.debug("Checking compatibility for PLF version ${plfVersion} with constraint ${addonConstraint}")
      if (!addonConstraint.containsVersion(plfVersion)) {
        throw new CompatibilityException(addon, env.platform.version)
      }
    } else {
      LOG.debug("Compatibility check deactivated")
    }
    if (isInstalled(env.statusesDirectory, addon)) {
      if (!force) {
        Addon oldAddon = parseJSONAddon(getAddonStatusFile(env.statusesDirectory, addon).text);
        throw new AddonAlreadyInstalledException(oldAddon)
      } else {
        Addon oldAddon = parseJSONAddon(getAddonStatusFile(env.statusesDirectory, addon).text);
        uninstallAddon(env, oldAddon)
      }
    }
    if (noCache && getLocalArchive(env.archivesDirectory, addon).exists()) {
      LOG.withStatus("Deleting ${addon.name} ${addon.version} archive") {
        getLocalArchive(env.archivesDirectory, addon).delete()
      }
    }
    LOG.info("Installing @|yellow ${addon.name} ${addon.version}|@")
    if (!getLocalArchive(env.archivesDirectory, addon).exists()) {
      // Let's download it
      if (addon.downloadUrl.startsWith("http")) {
        if (offline) throw new AddonsManagerException(
            "${addon.name} ${addon.version} archive not found locally and offline mode activated")
        LOG.withStatus("Downloading add-on ${addon.name} ${addon.version}") {
          FileUtils.downloadFile(addon.downloadUrl, getLocalArchive(env.archivesDirectory, addon))
        }
      } else if (addon.downloadUrl.startsWith("file://")) {
        // Let's see if it is a relative path
        File originFile = new File(env.addonsDirectory, addon.downloadUrl.replaceAll("file://", ""))
        if(!originFile.exists()){
          //Let's test if it is an absolute path
          originFile = new File(addon.downloadUrl.replaceAll("file://", ""))
        }
        if(!originFile.exists()){
          throw new AddonsManagerException("File not found : ${addon.downloadUrl}")
        }
        LOG.withStatus("Copying add-on ${addon.name} ${addon.version}") {
          FileUtils.copyFile(originFile,
                             getLocalArchive(env.archivesDirectory, addon))
        }
      } else {
        throw new AddonsManagerException("Invalid or not supported download URL : ${addon.downloadUrl}")
      }
    }
    addon.installedLibraries = FileUtils.flatExtractFromZip(getLocalArchive(env.archivesDirectory, addon),
                                                            env.platform.librariesDirectory, '^.*jar$')
    addon.installedWebapps = FileUtils.flatExtractFromZip(getLocalArchive(env.archivesDirectory, addon),
                                                          env.platform.webappsDirectory, '^.*war$')
    // Update application.xml if it exists
    File applicationDescriptorFile = new File(env.platform.webappsDirectory, "META-INF/application.xml")
    if (applicationDescriptorFile.exists()) {
      processFileInplace(applicationDescriptorFile) { text ->
        GPathResult applicationXmlContent = new XmlSlurper(false, false).parseText(text)
        addon.installedWebapps.each { file ->
          String webContext = file.substring(0, file.length() - 4)
          LOG.withStatus("Adding context declaration /${webContext} for ${file} in application.xml") {
            applicationXmlContent.depthFirst().findAll {
              (it.name() == 'module') && (it.'web'.'web-uri'.text() == file)
            }.each { node ->
              // remove existing node
              node.replaceNode {}
            }
            applicationXmlContent."initialize-in-order" + {
              module {
                web {
                  'web-uri'(file)
                  'context-root'(webContext)
                }
              }
            }
          }
        }
        serializeXml(applicationXmlContent)
      }
    }
    LOG.withStatus("Recording installation details into ${getAddonStatusFile(env.statusesDirectory, addon).name}") {
      new FileWriter(getAddonStatusFile(env.statusesDirectory, addon)).withWriter { w ->
        StreamingJsonBuilder builder = new StreamingJsonBuilder(w)
        builder(
            id: addon.id,
            version: addon.version,
            unstable: addon.unstable,
            name: addon.name,
            description: addon.description,
            releaseDate: addon.releaseDate,
            sourceUrl: addon.sourceUrl,
            screenshotUrl: addon.screenshotUrl,
            thumbnailUrl: addon.thumbnailUrl,
            documentationUrl: addon.documentationUrl,
            downloadUrl: addon.downloadUrl,
            vendor: addon.vendor,
            author: addon.author,
            authorEmail: addon.authorEmail,
            license: addon.license,
            licenseUrl: addon.licenseUrl,
            supportedDistributions: addon.supportedDistributions,
            supportedApplicationServers: addon.supportedApplicationServers,
            compatibility: addon.compatibility,
            installedLibraries: addon.installedLibraries,
            installedWebapps: addon.installedWebapps
        )
      }
    }
    LOG.withStatusOK("Add-on ${addon.name} ${addon.version} installed.")
  }

  protected void uninstallAddon(EnvironmentSettings env, Addon addon) {
    LOG.info("Uninstalling @|yellow ${addon.name} ${addon.version}|@")

    addon.installedLibraries.each {
      library ->
        File fileToDelete = new File(env.platform.librariesDirectory, library)
        if (!fileToDelete.exists()) {
          LOG.warn("No library ${library} to delete")
        } else {
          LOG.withStatus("Deleting library ${library}") {
            fileToDelete.delete()
            assert !fileToDelete.exists()
          }
        }
    }

    // Update application.xml if it exists
    File applicationDescriptorFile = new File(env.platform.webappsDirectory, "META-INF/application.xml")

    addon.installedWebapps.each {
      webapp ->
        File fileToDelete = new File(env.platform.webappsDirectory, webapp)
        String webContext = webapp.substring(0, webapp.length() - 4)
        if (!fileToDelete.exists()) {
          LOG.warn("No web application ${webapp} to delete")
        } else {
          LOG.withStatus("Deleting web application ${webapp}") {
            fileToDelete.delete()
            assert !fileToDelete.exists()
          }
        }
        if (applicationDescriptorFile.exists()) {
          LOG.withStatus("Adding context declaration /${webContext} for ${webapp} in application.xml") {
            processFileInplace(applicationDescriptorFile) { text ->
              GPathResult applicationXmlContent = new XmlSlurper(false, false).parseText(text)
              applicationXmlContent.depthFirst().findAll {
                (it.name() == 'module') && (it.'web'.'web-uri'.text() == webapp)
              }.each { node ->
                // remove existing node
                node.replaceNode {}
              }
              serializeXml(applicationXmlContent)
            }
          }
        }
    }
    LOG.withStatus("Deleting installation details ${getAddonStatusFile(env.statusesDirectory, addon).name}") {
      getAddonStatusFile(env.statusesDirectory, addon).delete()
      assert !getAddonStatusFile(env.statusesDirectory, addon).exists()
    }
    LOG.withStatusOK("Add-on ${addon.name} ${addon.version} uninstalled")
  }

  /**
   * Load addons from local and remote catalogs
   * @param remoteCatalogUrl The remote catalog URL
   * @param noCache If the 1h cache must be used for the remote catalog
   * @param catalogsCacheDirectory The directory where are cached remote catalogs
   * @param offline If the operation must be done offline (nothing will be downloaded)
   * @param localCatalogFile The local catalog file
   * @param distributionType The distribution type which addons listed must be compatible with
   * @param appServerType The application seerver type which addons listed must be compatible with
   * @return a list of addons
   */
  protected List<Addon> loadAddons(URL remoteCatalogUrl,
                                   Boolean noCache,
                                   File catalogsCacheDirectory,
                                   Boolean offline,
                                   File localCatalogFile,
                                   PlatformSettings.DistributionType distributionType,
                                   PlatformSettings.AppServerType appServerType
  ) {
    return loadAddons(remoteCatalogUrl, noCache, catalogsCacheDirectory, offline, localCatalogFile, distributionType,
                      appServerType, true, true)
  }

  /**
   * Load addons from local and remote catalogs
   * @param remoteCatalogUrl The remote catalog URL
   * @param noCache If the 1h cache must be used for the remote catalog
   * @param catalogsCacheDirectory The directory where are cached remote catalogs
   * @param offline If the operation must be done offline (nothing will be downloaded)
   * @param localCatalogFile The local catalog file
   * @param distributionType The distribution type which addons listed must be compatible with
   * @param appServerType The application seerver type which addons listed must be compatible with
   * @param allowSnapshot allow addons with snapshot version
   * @param allowUnstable allow addons with unstable version
   * @return a list of addons
   */
  protected List<Addon> loadAddons(URL remoteCatalogUrl,
                                   Boolean noCache,
                                   File catalogsCacheDirectory,
                                   Boolean offline,
                                   File localCatalogFile,
                                   PlatformSettings.DistributionType distributionType,
                                   PlatformSettings.AppServerType appServerType,
                                   Boolean allowSnapshot,
                                   Boolean allowUnstable
  ) {
    return filterAddonsByVersion(
        mergeCatalogs(
            loadAddonsFromUrl(remoteCatalogUrl, noCache, offline, catalogsCacheDirectory),
            loadAddonsFromFile(localCatalogFile),
            distributionType,
            appServerType).findAll { !ADDONS_MANAGER_CATALOG_ID.equals(it.id) },
        allowSnapshot,
        allowUnstable)
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
      Boolean noCache,
      Boolean offline,
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
   * Parse a JSON String representing an Add-on to build an {@link Addon} object
   * @param text the JSON text to parse
   * @return an Addon object
   */
  protected Addon parseJSONAddon(String text) {
    return fromJSON(new JsonSlurper().parseText(text))
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

  protected List<Addon> getInstalledAddonsList(EnvironmentSettings env) {
    return env.statusesDirectory.list(
        { dir, file -> file ==~ /.*?\${AddonService.STATUS_FILE_EXT}/ } as FilenameFilter
    ).toList().collect { it -> parseJSONAddon(new File(env.statusesDirectory, it).text) }
  }

  protected List<Addon> getOutdatedAddonsList(List<Addon> installedAddons, List<Addon> availableAddons) {
    return installedAddons.findAll { installedAddon ->
      findNewerAddons(installedAddon, availableAddons).size() > 0
    }
  }

  /**
   * Find in the list {@code addons} all addons with the same identifier {@link Addon#id} and a higher version number
   * {@link Addon#version} than {@code addonRef}
   * @param addonRef The addon reference
   * @param addons The list to filter
   * @return A list of addons
   */
  protected List<Addon> findNewerAddons(Addon addonRef, List<Addon> addons) {
    assert addonRef
    assert addonRef.id
    assert addonRef.version
    return addons.findAll { it.id == addonRef.id && it > addonRef }
  }

  /**
   * Find in the list {@code addons} the addon with the identifier {@code addonId} and the highest version number
   * @param addonId The addon identifier
   * @param addons The list to filter
   * @return The addon matching constraints or null if none.
   */
  protected Addon findNewestAddon(String addonId, List<Addon> addons) {
    assert addonId
    return addons.findAll { it.id == addonId }.max()
  }

  /**
   * Filter entries in {@code addons} to keep only stable versions. Return also snapshot versions if {@code allowSnapshot} is
   * true and unstable versions if {@code allowUnstable} is true
   * @param addons The list of addons to filter
   * @param allowSnapshot Also return addons with snapshot versions (-SNAPSHOT)
   * @param allowUnstable Also return addons with unstable versions (alpha, beta, RC, ...)
   * @return the list of addons.
   */
  protected List<Addon> filterAddonsByVersion(List<Addon> addons, Boolean allowSnapshot, Boolean allowUnstable) {
    return addons.findAll {
      !it.unstable && !it.isSnapshot() || it.unstable && !it.isSnapshot() && allowUnstable || it.isSnapshot() && allowSnapshot
    }
  }

  /**
   * Returns all add-ons supporting a distributionType+appServerType
   * @param addons The catalog to filter entries
   * @param distributionType The distribution type to support
   * @param appServerType The application server type to support
   * @return
   */
  protected List<Addon> filterAddonsByCompatibility(
      final List<Addon> addons,
      PlatformSettings.DistributionType distributionType,
      PlatformSettings.AppServerType appServerType) {
    return addons.findAll {
      it.supportedDistributions.contains(distributionType) && it.supportedApplicationServers.contains(appServerType)
    }
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
    List<Addon> filteredCentralCatalog = filterAddonsByCompatibility(remoteCatalog, distributionType, appServerType)
    List<Addon> filteredLocalCatalog = filterAddonsByCompatibility(localCatalog, distributionType, appServerType)
    // Let's initiate a new list from the filtered list of the remote catalog
    List<Addon> mergedCatalog = filteredCentralCatalog.clone()
    // Let's add entries from the filtered local catalog which aren't already in the catalog (based on id+version identifiers)
    filteredLocalCatalog.findAll { !mergedCatalog.contains(it) }.each { mergedCatalog.add(it) }
    return mergedCatalog
  }

  protected File getAddonStatusFile(File statusesDirectory, String addonId) {
    return new File(statusesDirectory, "${addonId}${STATUS_FILE_EXT}")
  }

  protected File getLocalArchive(File archivesDirectory, Addon addon) {
    return new File(archivesDirectory, "${addon.id}-${addon.version}.zip")
  }

  protected File getAddonStatusFile(File statusesDirectory, Addon addon) {
    return getAddonStatusFile(statusesDirectory, addon.id)
  }

  protected Boolean isInstalled(File statusesDirectory, Addon addon) {
    return getAddonStatusFile(statusesDirectory, addon).exists()
  }

  protected String serializeXml(GPathResult xml) {
    XmlUtil.serialize(new StreamingMarkupBuilder().bind {
      mkp.yield xml
    })
  }

  protected processFileInplace(File file, Closure processText) {
    String text = file.text
    file.write(processText(text))
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
