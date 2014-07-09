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
import org.exoplatform.platform.am.cli.Conflict
import org.exoplatform.platform.am.ex.*
import org.exoplatform.platform.am.settings.EnvironmentSettings
import org.exoplatform.platform.am.settings.PlatformSettings
import org.exoplatform.platform.am.utils.Console
import org.exoplatform.platform.am.utils.FileUtils
import org.exoplatform.platform.am.utils.Logger

import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

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
   * @param env The execution environment
   * @param parameters Command line parameters for a list action
   */
  void listAddons(
      EnvironmentSettings env,
      CommandLineParameters.ListCommandParameters parameters) {
    if (parameters.installed) {
      listInstalledAddons(env)
    } else if (parameters.outdated) {
      listOutdatedAddons(
          env,
          parameters.unstable,
          parameters.snapshots,
          parameters.noCache,
          parameters.offline,
          parameters.catalog)
    } else {
      listAddonsFromCatalogs(
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
   * @param env The execution environment
   * @param parameters Command line parameters for a describe action
   */
  void describeAddon(
      EnvironmentSettings env,
      CommandLineParameters.DescribeCommandParameters parameters) {
    List<Addon> availableAddons = loadAddons(
        parameters.catalog ?: env.remoteCatalogUrl,
        parameters.noCache,
        env.catalogsCacheDirectory,
        parameters.offline,
        env.localAddonsCatalogFile,
        env.platform.distributionType,
        env.platform.appServerType,
        env.manager.version,
        env.addonsDirectory,
        env.versionsDirectory,
        env.archivesDirectory)
    Addon addon = findAddon(
        availableAddons,
        parameters.addonId,
        parameters.addonVersion,
        parameters.snapshots,
        parameters.unstable)
    describeAddon(addon)
  }

  /**
   * Install an add-on given the current environment {@code env} and command line {@code parameters}.
   * @param env The execution environment
   * @param parameters Command line parameters for an install action
   */
  void installAddon(
      EnvironmentSettings env,
      CommandLineParameters.InstallCommandParameters parameters) {
    List<Addon> availableAddons = loadAddons(
        parameters.catalog ?: env.remoteCatalogUrl,
        parameters.noCache,
        env.catalogsCacheDirectory,
        parameters.offline,
        env.localAddonsCatalogFile,
        env.platform.distributionType,
        env.platform.appServerType,
        env.manager.version,
        env.addonsDirectory,
        env.versionsDirectory,
        env.archivesDirectory)
    Addon addon = findAddon(
        availableAddons,
        parameters.addonId,
        parameters.addonVersion,
        parameters.snapshots,
        parameters.unstable)
    installAddon(env, addon, parameters.force, parameters.noCache, parameters.offline, parameters.noCompat, parameters.conflict)
  }

  /**
   * Uninstall an add-on given the current environment {@code env} and command line {@code parameters}.
   * @param env The execution environment
   * @param parameters Command line parameters for an uninstall action
   */
  void uninstallAddon(
      EnvironmentSettings env,
      CommandLineParameters.UninstallCommandParameters parameters) {
    File statusFile = getAddonStatusFile(env.statusesDirectory, parameters.addonId)
    if (statusFile.exists()) {
      Addon addon
      LOG.withStatus("Loading add-on installation details") {
        addon = createAddonFromJsonText(statusFile.text);
      }
      uninstallAddon(env, addon)
    } else {
      throw new AddonNotInstalledException("Add-on not installed. It cannot be uninstalled.")
    }
  }

  /**
   * List add-ons installed in the current environment {@code env}.
   * @param env The execution environment
   */
  protected void listInstalledAddons(
      EnvironmentSettings env) {
    // Display only installed add-ons
    List<Addon> installedAddons = getInstalledAddons(env)
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
  }

  /**
   * List add-ons installed with a more recent version available in the current environment {@code env}.
   *
   * @param env The execution environment
   * @param allowUnstable List also unstable versions ?
   * @param allowSnapshot List also development versions ?
   * @param noCache Don't use catalog's cache if exist ?
   * @param offline Don't download anything ?
   * @param alternateCatalog Specific remote catalog URL to use
   */
  protected void listOutdatedAddons(
      EnvironmentSettings env,
      Boolean allowUnstable,
      Boolean allowSnapshot,
      Boolean noCache,
      Boolean offline,
      URL alternateCatalog) {
    List<Addon> installedAddons = getInstalledAddons(env)
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
          allowUnstable,
          env.manager.version,
          env.addonsDirectory,
          env.versionsDirectory,
          env.archivesDirectory
      )
      List<Addon> outdatedAddons = getOutdatedAddons(installedAddons, availableAddons)
      if (outdatedAddons.size() > 0) {
        LOG.info "\n@|bold Outdated add-ons:|@"
        outdatedAddons.groupBy { it.id }.sort().each {
          Addon anAddon = it.value.first()
          LOG.info String.format(
              "\n+ @|bold,yellow %-${outdatedAddons.id*.size().max() + outdatedAddons.version*.size().max() + 1}s|@ : @|bold %s|@, %s",
              "${anAddon.id} ${anAddon.version}", anAddon.name, anAddon.description)
          LOG.info String.format(
              "     Newest Version(s) : %s",
              findAddonsNewerThan(anAddon, availableAddons).sort().reverse().collect {
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
  }

  /**
   * List add-ons from remote+local catalogs
   *
   * @param env The execution environment
   * @param allowUnstable List also unstable versions ?
   * @param allowSnapshot List also development versions ?
   * @param noCache Don't use catalog's cache if exist ?
   * @param offline Don't download anything ?
   * @param alternateCatalog Specific remote catalog URL to use
   */
  protected void listAddonsFromCatalogs(
      EnvironmentSettings env,
      Boolean allowUnstable,
      Boolean allowSnapshot,
      Boolean noCache,
      Boolean offline,
      URL alternateCatalog) {
    List<Addon> availableAddons = loadAddons(
        alternateCatalog ?: env.remoteCatalogUrl,
        noCache,
        env.catalogsCacheDirectory,
        offline,
        env.localAddonsCatalogFile,
        env.platform.distributionType,
        env.platform.appServerType,
        allowSnapshot,
        allowUnstable,
        env.manager.version,
        env.addonsDirectory,
        env.versionsDirectory,
        env.archivesDirectory)
    if (availableAddons.size() > 0) {
      LOG.infoHR("=")
      LOG.info "@|bold Available add-ons|@"
      LOG.infoHR("=")
      availableAddons.groupBy { it.id }.sort().each {
        Addon anAddon = it.value.first()
        //LOG.info String.format("\n+ @|bold,yellow %-${availableAddons.id*.size().max()}s|@ : @|bold %s|@", anAddon.id,anAddon.name)
        LOG.info String.format("@|bold,yellow %s|@ - @|bold %s|@", anAddon.id,anAddon.name)
        LOG.wrapLine(anAddon.description, Console.get().width - Logger.Level.INFO.prefix.length()).each {
          LOG.info(it)
        }
        Addon latestStableAddon = findNewestAddon(anAddon.id, availableAddons.findAll { !it.snapshot && !it.unstable })
        if (latestStableAddon) {
          LOG.info "@|bold + Latest stable version :|@ @|yellow ${latestStableAddon.version}|@"
        }
        Addon latestUnstableAddon = findNewestAddon(anAddon.id, availableAddons.findAll { !it.snapshot && it.unstable })
        if (latestUnstableAddon) {
          LOG.info "@|bold + Latest unstable version :|@ @|yellow ${latestUnstableAddon.version}|@"
        }
        Addon latestSnapshotAddon = findNewestAddon(anAddon.id, availableAddons.findAll { it.snapshot })
        if (latestSnapshotAddon) {
          LOG.info "@|bold + Latest development version :|@ @|yellow ${latestSnapshotAddon.version}|@"
        }
        LOG.infoHR()
      }
      LOG.info String.format("""
To install an add-on:
    ${env.manager.scriptName} install @|yellow <addonId:[version]>|@
  """)
    } else {
      LOG.warn "No add-on found in remote and local catalogs"
    }
  }

  /**
   * Describe an add-on
   * @param addon The add-on to describe
   */
  protected void describeAddon(
      final Addon addon) {
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd")
    LOG.infoHR("=")
    LOG.info "Informations about add-on @|bold,yellow ${addon.id}|@@|bold :${addon.version}|@"
    LOG.infoHR()
    Map map = [
        "Identifier"                        : addon.id,
        "Version"                           : addon.version,
        "Name"                              : addon.name,
        "Description"                       : addon.description,
        "Release date (YYYY-MM-DD)"         : addon.releaseDate ? sdf.format(sdf.parse(addon.releaseDate)) : null,
        "Sources URL"                       : addon.sourceUrl ? URLDecoder.decode(addon.sourceUrl, "UTF-8") : null,
        "Screenshot URL"                    : addon.screenshotUrl ? URLDecoder.decode(addon.screenshotUrl, "UTF-8") : null,
        "Thumbnail URL"                     : addon.thumbnailUrl ? URLDecoder.decode(addon.thumbnailUrl, "UTF-8") : null,
        "Documentation URL"                 : addon.documentationUrl ? URLDecoder.decode(addon.documentationUrl, "UTF-8") : null,
        "Download URL"                      : addon.downloadUrl ? URLDecoder.decode(addon.downloadUrl, "UTF-8") : null,
        "Vendor"                            : addon.vendor,
        "Author"                            : addon.author,
        "Author email"                      : addon.authorEmail,
        "License"                           : addon.license,
        "License URL"                       : addon.licenseUrl ? URLDecoder.decode(addon.licenseUrl, "UTF-8") : null,
        "License must be accepted"          : addon.mustAcceptLicense,
        "Supported application Server(s)"   : addon.supportedApplicationServers,
        "Supported platform distribution(s)": addon.supportedDistributions,
        "Supported platform version(s)"     : addon.compatibility] as LinkedHashMap //LinkedHashMap to keep the insertion order
    map.keySet().findAll { map.get(it) }.each {
      LOG.info String.format("@|bold %-${map.keySet()*.size().max()}s|@ : @|bold,yellow %s|@", it, map.get(it))
    }
    LOG.infoHR("=")
  }

  /**
   * Install the @{code addon} into the current @{code env}*
   * @param env The execution environment
   * @param addon The add-on to install
   * @param force Enforce to install it even if it was already installed
   * @param noCache Don't use catalog's cache if exist ?
   * @param offline Don't download anything ?
   * @param noCompat Bypass compatibility checks
   * @param conflict Conflict resolution mode
   */
  protected void installAddon(
      EnvironmentSettings env,
      Addon addon,
      Boolean force,
      Boolean noCache,
      Boolean offline,
      Boolean noCompat,
      Conflict conflict) {
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
    if (addon.mustAcceptLicense && addon.licenseUrl) {
      // Local license file
      File licenseFile = getAddonLicenseFile(env.statusesDirectory, addon)
      // [LICENSE_05] Don't prompt to validate a license already accepted
      if (!licenseFile.exists()) {
        // [LICENSE_01] Download and display license if mustAcceptLicenseTerms=true
        LOG.withStatus("Downloading license ${addon.license} from ${addon.licenseUrl}") {
          licenseFile << new URL(addon.licenseUrl).text
        }
        // [LICENSE_02] Split the license per page (click on a touch to advance)
        LOG.infoHR('=')
        LOG.info("License ${addon.license} :")
        LOG.infoHR()
        int i = 0
        licenseFile?.text?.split('\n').collect().each {
          LOG.wrapLine(it, Console.get().width - Logger.Level.INFO.prefix.length()).each {
            LOG.info(it)
            i++
            if (i == Console.get().height - 2) {
              LOG.info("@|yellow [Press any key to continue ...]|@")
              Console.get().in.read()
              i = 0
            }
          }
        }
        LOG.infoHR()
        // [LICENSE_03] [LICENSE_04] interactive validation of license
        LOG.info("You must accept the license above to install this add-on. Type \"yes\" to accept : ")
        String reply = Console.get().readLine()?.trim()?.toLowerCase()
        LOG.debug("REPLY : ${reply}")
        if (!"yes".equalsIgnoreCase(reply)) {
          licenseFile.delete()
          throw new LicenseValidationException("You didn't accept the license. Installation aborted.")
        }
      }
    } else {
      //[LICENSE_06] no licenseUrl or mustAcceptLicenseTerms=false
      LOG.warn("DISCLAIMER : You are about to install third-party software available on your eXo Platform instance.")
      LOG.warn(
          "This software is provided \"as is\" without warranty of any kind, either expressed or implied and such software is to be used at your own risk.")
    }
    if (isAddonInstalled(env.statusesDirectory, addon)) {
      if (!force) {
        Addon oldAddon = createAddonFromJsonText(getAddonStatusFile(env.statusesDirectory, addon).text);
        throw new AddonAlreadyInstalledException(oldAddon)
      } else {
        Addon oldAddon = createAddonFromJsonText(getAddonStatusFile(env.statusesDirectory, addon).text);
        uninstallAddon(env, oldAddon)
      }
    }
    if (noCache && getAddonLocalArchive(env.archivesDirectory, addon).exists()) {
      LOG.withStatus("Deleting ${addon.name} ${addon.version} archive") {
        getAddonLocalArchive(env.archivesDirectory, addon).delete()
      }
    }
    LOG.info("Installing @|yellow ${addon.name} ${addon.version}|@")
    if (!getAddonLocalArchive(env.archivesDirectory, addon).exists()) {
      // Let's download it
      if (addon.downloadUrl.startsWith("http")) {
        if (offline) throw new UnknownErrorException(
            "${addon.name} ${addon.version} archive not found locally and offline mode activated")
        LOG.withStatus("Downloading add-on ${addon.name} ${addon.version}") {
          downloadFile(addon.downloadUrl, getAddonLocalArchive(env.archivesDirectory, addon))
        }
      } else if (addon.downloadUrl.startsWith("file://")) {
        // Let's see if it is a relative path
        File originFile = new File(env.addonsDirectory, addon.downloadUrl.replaceAll("file://", ""))
        if (!originFile.exists()) {
          //Let's test if it is an absolute path
          originFile = new File(addon.downloadUrl.replaceAll("file://", ""))
        }
        if (!originFile.exists()) {
          throw new UnknownErrorException("File not found : ${addon.downloadUrl}")
        }
        LOG.withStatus("Copying add-on ${addon.name} ${addon.version}") {
          copyFile(originFile,
                   getAddonLocalArchive(env.archivesDirectory, addon))
        }
      } else {
        throw new UnknownErrorException("Invalid or not supported download URL : ${addon.downloadUrl}")
      }
    }
    addon.installedLibraries = new ArrayList<String>()
    addon.installedWebapps = new ArrayList<String>()
    addon.installedOthersFiles = new ArrayList<String>()
    addon.overwrittenFiles = new ArrayList<String>()
    File readmeFile = File.createTempFile("readme", "txt")
    readmeFile.deleteOnExit()
    try {
      ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(getAddonLocalArchive(env.archivesDirectory, addon)))
      zipInputStream.withStream {
        ZipEntry entry
        while (entry = zipInputStream.nextEntry) {
          File destinationFile
          List<String> installationList
          LOG.debug("ZIP entry : ${entry.name}")
          if (entry.isDirectory()) {
            // Do nothing
            continue
          } else if (entry.name?.equalsIgnoreCase("README")) {
            //[AM_STRUCT_05] a README file may be placed at the root of the archive. This readme file will be displayed after the install command.
            FileOutputStream output = new FileOutputStream(readmeFile)
            output.withStream {
              int len = 0;
              byte[] buffer = new byte[4096]
              while ((len = zipInputStream.read(buffer)) > 0) {
                output.write(buffer, 0, len);
              }
            }
            continue
          } else if (entry.name =~ '^.*jar$') {
            // [AM_STRUCT_02] Add-ons libraries target directory
            destinationFile = new File(env.platform.librariesDirectory, FileUtils.extractFilename(entry.name))
            installationList = addon.installedLibraries
          } else if (entry.name =~ '^.*war$') {
            // [AM_STRUCT_03] Add-ons webapps target directory
            destinationFile = new File(env.platform.webappsDirectory, FileUtils.extractFilename(entry.name))
            installationList = addon.installedWebapps
          } else {
            // see [AM_STRUCT_04] non war/jar files locations
            destinationFile = new File(env.platform.homeDirectory, entry.name)
            installationList = addon.installedOthersFiles
          }
          LOG.debug("Destination : ${destinationFile}")
          if (!destinationFile.parentFile.exists()) {
            FileUtils.mkdirs(destinationFile.parentFile)
          }
          String plfHomeRelativePath = env.platform.homeDirectory.toURI().relativize(destinationFile.toURI()).getPath()
          if (destinationFile.exists()) {
            switch (conflict) {
              case Conflict.FAIL:
                throw new UnknownErrorException(
                    "File ${plfHomeRelativePath} already exists. Installation aborted. Use --conflict=skip or --conflict=overwrite option to install it.")
                break
              case Conflict.OVERWRITE:
                LOG.warn("File ${plfHomeRelativePath} already exists. Overwritten.")
                // Let's save it before
                File backupFile = new File(env.overwrittenFilesDirectory, "${addon.id}/${plfHomeRelativePath}")
                if (!backupFile.parentFile.exists()) {
                  FileUtils.mkdirs(backupFile.parentFile)
                }
                copyFile(destinationFile, backupFile)
                addon.overwrittenFiles.add(plfHomeRelativePath)
                break
              case Conflict.SKIP:
                LOG.warn("File ${plfHomeRelativePath} already exists. Skipped.")
                continue // Next entry
            }
          }
          LOG.withStatus("Installing file ${plfHomeRelativePath}") {
            FileOutputStream output = new FileOutputStream(destinationFile)
            output.withStream {
              int len = 0;
              byte[] buffer = new byte[4096]
              while ((len = zipInputStream.read(buffer)) > 0) {
                output.write(buffer, 0, len);
              }
            }
          }
          installationList.add(plfHomeRelativePath)
        }
      }
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
    } finally {
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
              installedWebapps: addon.installedWebapps,
              installedOthersFiles: addon.installedOthersFiles,
              overwrittenFiles: addon.overwrittenFiles
          )
        }
      }
    }
    // [AM_INST_12] At the end of a successful install command, the README of the add-on is displayed in the console if present.
    if (readmeFile.text) {
      LOG.infoHR('=')
      LOG.info("README :")
      LOG.infoHR()
      int i = 0
      readmeFile.text.split('\n').collect().each {
        LOG.wrapLine(it, Console.get().width - Logger.Level.INFO.prefix.length()).each {
          LOG.info(it)
          i++
          if (i == Console.get().height - 2) {
            LOG.info("@|yellow [Press any key to continue ...]|@")
            Console.get().in.read()
            i = 0
          }
        }
      }
      LOG.infoHR()
      readmeFile.delete()
    }
    LOG.withStatusOK("Add-on ${addon.name} ${addon.version} installed.")
  }

  /**
   * Uninstall the @{code addon} from the current @{code env}* @param env The environment where the add-on must be uninstalled
   * @param addon The addon to remove
   */
  protected void uninstallAddon(
      EnvironmentSettings env,
      Addon addon) {
    LOG.info("Uninstalling @|yellow ${addon.name} ${addon.version}|@")

    addon.installedLibraries.each {
      library ->
        File fileToDelete = new File(env.platform.homeDirectory, library)
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
        File fileToDelete = new File(env.platform.homeDirectory, webapp)
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

    addon.installedOthersFiles.each {
      otherFile ->
        File fileToDelete = new File(env.platform.homeDirectory, otherFile)
        if (!fileToDelete.exists()) {
          LOG.warn("No file ${otherFile} to delete")
        } else {
          LOG.withStatus("Deleting file ${otherFile}") {
            fileToDelete.delete()
            assert !fileToDelete.exists()
          }
        }
    }

    // Restore overwritten files
    addon.overwrittenFiles.each {
      fileToRecover ->
        File backupFile = new File(env.overwrittenFilesDirectory, "${addon.id}/${fileToRecover}")
        File originalFile = new File(env.platform.homeDirectory, fileToRecover)
        LOG.withStatus("Reinstalling original file ${fileToRecover}") {
          copyFile(backupFile, originalFile)
        }
        LOG.withStatus("Deleting backup file of ${fileToRecover}") {
          backupFile.delete()
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
  protected List<Addon> loadAddons(
      URL remoteCatalogUrl,
      Boolean noCache,
      File catalogsCacheDirectory,
      Boolean offline,
      File localCatalogFile,
      PlatformSettings.DistributionType distributionType,
      PlatformSettings.AppServerType appServerType,
      String currentAddonsManagerVersion,
      File addonsDirectory,
      File versionsDirectory,
      File archivesDirectory) {
    return loadAddons(remoteCatalogUrl, noCache, catalogsCacheDirectory, offline, localCatalogFile, distributionType,
                      appServerType, true, true, currentAddonsManagerVersion, addonsDirectory, versionsDirectory,
                      archivesDirectory)
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
  protected List<Addon> loadAddons(
      URL remoteCatalogUrl,
      Boolean noCache,
      File catalogsCacheDirectory,
      Boolean offline,
      File localCatalogFile,
      PlatformSettings.DistributionType distributionType,
      PlatformSettings.AppServerType appServerType,
      Boolean allowSnapshot,
      Boolean allowUnstable,
      String currentAddonsManagerVersion,
      File addonsDirectory,
      File versionsDirectory,
      File archivesDirectory) {
    List<Addon> allAddons = mergeCatalogs(
        loadAddonsFromUrl(remoteCatalogUrl, noCache, offline, catalogsCacheDirectory),
        loadAddonsFromFile(localCatalogFile),
        distributionType,
        appServerType)
    Addon newerAddonManager = findAddonsNewerThan(
        new Addon(id: ADDONS_MANAGER_CATALOG_ID, version: currentAddonsManagerVersion),
        findAddonsByVersion(allAddons, false, false))?.max()
    if (newerAddonManager) {
      LOG.info(
          "New Addons Manager version @|yellow,bold ${newerAddonManager.version}|@ found. It will be automatically updated " +
              "after its restart.")
      // Backup the current library
      File backupDirectory = new File(versionsDirectory, currentAddonsManagerVersion)
      if (!backupDirectory.exists()) {
        FileUtils.mkdirs(backupDirectory)
      }
      LOG.withStatus("Backing up current addons manager library") {
        FileUtils.copyFile(new File(addonsDirectory, "addons-manager.jar"), new File(backupDirectory, "addons-manager.jar"),
                           false)
      }
      // Let's download the new one
      File newAddonsManagerArchive = new File(archivesDirectory, "${newerAddonManager.id}-${newerAddonManager.version}.zip")
      LOG.withStatus("Downloading Addons Manager version @|yellow,bold ${newerAddonManager.version}|@") {
        FileUtils.downloadFile(newerAddonManager.downloadUrl, newAddonsManagerArchive)
      }
      LOG.withStatus("Extracting Addons Manager version @|yellow,bold ${newerAddonManager.version}|@") {
        ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(newAddonsManagerArchive))
        zipInputStream.withStream {
          ZipEntry entry
          while (entry = zipInputStream.nextEntry) {
            if (entry.name == "addons/addons-manager.jar") {
              FileOutputStream output = new FileOutputStream(new File(addonsDirectory, "addons-manager.jar.new"))
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
    return findAddonsByVersion(
        allAddons.findAll { !ADDONS_MANAGER_CATALOG_ID.equals(it.id) },
        allowSnapshot,
        allowUnstable)
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
            downloadFile(catalogUrl, tempFile)
            // Read the catalog content
            catalogContent = tempFile.text
          } catch (FileNotFoundException fne) {
            throw new UnknownErrorException("Catalog ${catalogUrl} not found", fne)
          }
        }
        try {
          LOG.withStatus("Loading add-ons list") {
            addons.addAll(createAddonsFromJsonText(catalogContent))
          }
          // Everything was ok, let's store the cache
          LOG.withStatus("Updating local cache") {
            copyFile(tempFile, catalogCacheFile, false)
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
              addons.addAll(createAddonsFromJsonText(catalogContent))
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
        LOG.withStatus("Loading add-ons list") {
          addons.addAll(createAddonsFromJsonText(catalogContent))
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
   * Returns the list of add-ons installed in the current environment @{code env}* @param env The environment where the add-on must be uninstalled
   * @return A list of @{link Addon}
   */
  protected List<Addon> getInstalledAddons(
      EnvironmentSettings env) {
    return env.statusesDirectory.list(
        { dir, file -> file ==~ /.*?\${AddonService.STATUS_FILE_EXT}/ } as FilenameFilter
    ).toList().collect { it -> createAddonFromJsonText(new File(env.statusesDirectory, it).text) }
  }

  /**
   * Returns the list of outdated add-ons by comparing the list of @{code installedAddons} with the one of
   * @{code availableAddons}* @param installedAddons The list of installed add-ons
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
   * @{code addonVersion} isn't set it will find the more recent version (stable per default excepted if @{code allowUnstable}* or @{code allowSnapshot} are set.
   * @param addons The list of add-ons in wich to do the search
   * @param addonId The Identifier of the add-on to find
   * @param addonVersion The version of the add-on to find
   * @param allowSnapshot allows addons with snapshot version Allow to retrieve a snapshot version if it is the most recent and
   * @{code addonVersion} isn't set
   * @param allowUnstable allows addons with snapshot version Allow to retrieve an unstable version if it is the most recent and
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
                               findAddonsByVersion(addons, allowSnapshots, allowUnstable))
      if (result == null) {
        if (!addons.find { it.id == addonId }) {
          throw new AddonNotFoundException(
              "No add-on with identifier ${addonId} found in local or remote catalogs, check your add-on identifier")
        } else {
          // Let's try to find an unstable version of the addon
          if (!allowUnstable && findNewestAddon(addonId,
                                                findAddonsByVersion(addons, allowSnapshots, true))) {
            LOG.info(
                "This add-on exists but doesn't have a stable released version yet! add --unstable option to use an unstable version")
          }
          // Let's try to find a snapshot version of the addon
          if (!allowSnapshots && findNewestAddon(addonId,
                                                 findAddonsByVersion(addons, true, allowUnstable))) {
            LOG.info(
                "This add-on exists but doesn't have a stable released version yet! add --snapshots option to use a development version")
          }
          throw new AddonNotFoundException("No add-on with identifier ${addonId} found in local or remote catalogs")
        }
      }
    } else {
      result = addons.find { it.id == addonId && it.version == addonVersion }
      if (result == null) {
        if (!addons.find { it.id == addonId }) {
          throw new AddonNotFoundException(
              "No add-on with identifier ${addonId} found in local or remote catalogs, check your add-on identifier")
        } else {
          List<Addon> stableAddons = findAddonsByVersion(addons.findAll { it.id == addonId }, false, false)
          if (!stableAddons.empty) {
            LOG.info "Stable version(s) available for add-on @|bold,yellow ${addonId}|@ : ${stableAddons.sort().reverse().collect { it.version }.join(', ')}"
          }
          List<Addon> unstableAddons = findAddonsByVersion(addons.findAll { it.id == addonId }, false, true)
          if (!unstableAddons.empty) {
            LOG.info "Unstable version(s) available for add-on @|bold,yellow ${addonId}|@ : ${unstableAddons.sort().reverse().collect { it.version }.join(', ')}"
          }
          List<Addon> snapshotAddons = findAddonsByVersion(addons.findAll { it.id == addonId }, true, false)
          if (!snapshotAddons.empty) {
            LOG.info "Development version(s) available for add-on @|bold,yellow ${addonId}|@ : ${snapshotAddons.sort().reverse().collect { it.version }.join(', ')}"
          }
          throw new AddonNotFoundException(
              "No add-on with identifier ${addonId} and version ${addonVersion} found in local or remote catalogs")
        }
      }
    }
    return result
  }

  /**
   * Find in the list {@code addons} all addons with the same identifier {@link Addon#id} and a higher version number
   * {@link Addon#version} than {@code addonRef}
   * @param addonRef The addon reference
   * @param addons The list to filter
   * @return A list of addons
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
   * Find in the list {@code addons} the addon with the identifier {@code addonId} and the highest version number
   * @param addonId The addon identifier
   * @param addons The list to filter
   * @return The addon matching constraints or null if none.
   */
  protected Addon findNewestAddon(
      String addonId,
      List<Addon> addons) {
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
  protected List<Addon> findAddonsByVersion(
      List<Addon> addons,
      Boolean allowSnapshot,
      Boolean allowUnstable) {
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
  protected List<Addon> findAddonsByCompatibility(
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
    List<Addon> filteredCentralCatalog = findAddonsByCompatibility(remoteCatalog, distributionType, appServerType)
    List<Addon> filteredLocalCatalog = findAddonsByCompatibility(localCatalog, distributionType, appServerType)
    // Let's initiate a new list from the filtered list of the remote catalog
    List<Addon> mergedCatalog = filteredCentralCatalog.clone()
    // Let's add entries from the filtered local catalog which aren't already in the catalog (based on id+version identifiers)
    filteredLocalCatalog.findAll { !mergedCatalog.contains(it) }.each { mergedCatalog.add(it) }
    return mergedCatalog
  }

  /**
   * Parse a JSON String representing an Add-on to build an {@link Addon} object
   * @param text the JSON text to parse
   * @return an Addon object
   */
  protected Addon createAddonFromJsonText(
      String text) {
    return createAddonFromJsonObject(new JsonSlurper().parseText(text))
  }

  /**
   * Loads a list of Addon from its JSON text representation
   * @param text The JSON text to parse
   * @return A List of addons
   */
  protected List<Addon> createAddonsFromJsonText(
      String text) {
    List<Addon> addonsList = new ArrayList<Addon>();
    new JsonSlurper().parseText(text).each { anAddon ->
      try {
        addonsList.add(createAddonFromJsonObject(anAddon))
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
  protected Addon createAddonFromJsonObject(
      Object anAddon) {
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
    addonObj.installedOthersFiles = anAddon.installedOthersFiles
    addonObj.overwrittenFiles = anAddon.overwrittenFiles
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
}
