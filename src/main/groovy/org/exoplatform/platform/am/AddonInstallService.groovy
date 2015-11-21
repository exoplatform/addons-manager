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

import groovy.json.StreamingJsonBuilder
import groovy.util.slurpersupport.GPathResult
import org.exoplatform.platform.am.cli.CommandLineParameters
import org.exoplatform.platform.am.cli.Conflict
import org.exoplatform.platform.am.ex.AddonAlreadyInstalledException
import org.exoplatform.platform.am.ex.LicenseValidationException
import org.exoplatform.platform.am.ex.UnknownErrorException
import org.exoplatform.platform.am.settings.EnvironmentSettings
import org.exoplatform.platform.am.utils.Console
import org.exoplatform.platform.am.utils.FileUtils
import org.exoplatform.platform.am.utils.Logger

import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

import static org.exoplatform.platform.am.utils.FileUtils.copyFile
import static org.exoplatform.platform.am.utils.FileUtils.downloadFile

/**
 * All services to install add-ons
 *
 * @author Arnaud Héritier <aheritier@exoplatform.com>
 */
public class AddonInstallService {
  /**
   * Logger
   */
  private static final Logger LOG = Logger.getInstance()

  /**
   * Add-on Services
   */
  private static final AddonService ADDON_SERVICE = AddonService.getInstance()

  /**
   * Singleton
   */
  private static final AddonInstallService singleton = new AddonInstallService()

  /**
   * Factory
   *
   * @return The {@link AddonInstallService} singleton instance
   */
  static AddonInstallService getInstance() {
    return singleton
  }

  /**
   * You should use the singleton
   */
  private AddonInstallService() {
  }

  /**
   * Install an add-on given the current environment {@code env} and command line {@code parameters}.
   * @param env The execution environment
   * @param parameters Command line parameters for an install action
   */
  void installAddon(
      EnvironmentSettings env,
      CommandLineParameters.InstallCommandParameters parameters,
      Boolean batchMode) {
    List<Addon> availableAddons = ADDON_SERVICE.loadAddons(
        env,
        parameters.catalog,
        parameters.noCache,
        parameters.offline,
        true,
        true)
    Addon addon = ADDON_SERVICE.findAddon(
        availableAddons,
        parameters.addonId,
        parameters.addonVersion,
        parameters.snapshots,
        parameters.unstable)
    installAddon(env,
                 addon,
                 parameters.force,
                 parameters.offline,
                 parameters.noCompat,
                 parameters.conflict,
                 batchMode)
  }

  /**
   * Install the @{code addon} into the current @{code env}* @param env The execution environment
   * @param addon The add-on to install
   * @param force Enforce to install it even if it was already installed
   * @param offline Don't download anything ?
   * @param noCompat Bypass compatibility checks
   * @param conflict Conflict resolution mode
   * @param batchMode Non-interactive behavior
   */
  protected void installAddon(
      EnvironmentSettings env,
      Addon addon,
      Boolean force,
      Boolean offline,
      Boolean noCompat,
      Conflict conflict,
      Boolean batchMode) {
    // Compatibility check
    if (!noCompat) {
      ADDON_SERVICE.validateCompatibility(addon, env.platform)
    } else {
      LOG.debug("Compatibility check deactivated")
    }
    if (ADDON_SERVICE.isAddonInstalled(env.statusesDirectory, addon)) {
      if (!force) {
        Addon oldAddon = ADDON_SERVICE.createAddonFromJsonText(
            ADDON_SERVICE.getAddonStatusFile(env.statusesDirectory, addon).text);
        throw new AddonAlreadyInstalledException(oldAddon)
      } else {
        Addon oldAddon = ADDON_SERVICE.createAddonFromJsonText(
            ADDON_SERVICE.getAddonStatusFile(env.statusesDirectory, addon).text);
        LOG.info("--force option activated, let's remove the previous installation of the add-on")
        AddonUninstallService.instance.uninstallAddon(env, oldAddon)
      }
    }
    if (addon.mustAcceptLicense && addon.licenseUrl) {
      // Local license file
      File licenseFile = ADDON_SERVICE.getAddonLicenseFile(env.statusesDirectory, addon)
      // [LICENSE_05] Don't prompt to validate a license already accepted
      if (!licenseFile.exists()) {
        // [LICENSE_01] Download and display license if mustAcceptLicenseTerms=true
        LOG.withStatus("Downloading license ${addon.license} from ${addon.licenseUrl}") {
          licenseFile << new URL(addon.licenseUrl).text
        }
        LOG.infoHR('=')
        LOG.info("License ${addon.license} :")
        LOG.infoHR('=')
        int i = 0
        licenseFile?.text?.readLines().each {
          LOG.wrapLine(it, Console.get().width - Logger.Level.INFO.prefix.length() - 1).each {
            LOG.info(it)
            i++
            // [LICENSE_02] Split the license per page (click on a touch to advance)
            if (!batchMode && i == Console.get().height - 2) {
              LOG.info("@|yellow [Press any key to continue ...]|@")
              Console.get().read()
              i = 0
            }
          }
        }
        LOG.infoHR()
        if (!batchMode) {
          // [LICENSE_03] [LICENSE_04] interactive validation of license
          LOG.info("You must accept the license above to install this add-on. Type \"yes\" to accept : ")
          String reply = Console.get().readLine()?.trim()?.toLowerCase()
          LOG.debug("REPLY : ${reply}")
          if (!"yes".equalsIgnoreCase(reply)) {
            licenseFile.delete()
            throw new LicenseValidationException("You didn't accept the license. Installation aborted.")
          }
        } else {
          LOG.warn("By installing this add-on, you are automatically accepting its license terms (${addon.licenseUrl})")
        }
      }
    } else {
      //[LICENSE_06] no licenseUrl or mustAcceptLicenseTerms=false
      LOG.warn("DISCLAIMER : You are about to install third-party software available on your eXo Platform instance.")
      LOG.warn(
          "This software is provided \"as is\" without warranty of any kind, either expressed or implied and such software is to be used at your own risk. Use of this software is governed by its license and/or terms and conditions and it is your sole responsibility to accept and respect them prior to installing and using this software.")
      LOG.warn("This software is not supported by eXo's Support Services.")
    }
    LOG.info("Installing @|yellow ${addon.id}:${addon.version}|@")
    // Let's download it
    if (addon.downloadUrl.startsWith("http")) {
      if (offline) {
        if (ADDON_SERVICE.getAddonLocalArchive(env.archivesDirectory, addon).exists()) {
          LOG.withStatusOK("Using ${addon.id}:${addon.version} archive from local archives directory")
        } else {
          LOG.withStatusKO("Using ${addon.id}:${addon.version} archive from local archives directory")
          throw new UnknownErrorException("Failed to install : ${addon.id}:${addon.version} not found in local archives. Remove --offline to download it.")
        }
      } else {
        downloadFile("Downloading add-on ${addon.id}:${addon.version} archive", addon.downloadUrl, ADDON_SERVICE.getAddonLocalArchive(env.archivesDirectory, addon))
      }
    } else if (addon.downloadUrl.startsWith("file://")) {
      // Let's see if it is a relative path
      File originFile = new File(env.addonsDirectory, addon.downloadUrl.replaceAll("file://", ""))
      if (!originFile.exists()) {
        //Let's test if it is an absolute path
        originFile = new File(addon.downloadUrl.replaceAll("file://", ""))
      }
      if (!originFile.exists()) {
        throw new UnknownErrorException("Failed to install : File not found ${addon.downloadUrl}")
      }
      copyFile("Copying add-on ${addon.id}:${addon.version} archive", originFile, ADDON_SERVICE.getAddonLocalArchive(env.archivesDirectory, addon))
    } else {
      throw new UnknownErrorException("Failed to install : Invalid or not supported download URL ${addon.downloadUrl}")
    }
    addon.installedLibraries = new ArrayList<String>()
    addon.installedWebapps = new ArrayList<String>()
    addon.installedProperties = new ArrayList<String>()
    addon.installedOthersFiles = new ArrayList<String>()
    addon.overwrittenFiles = new ArrayList<String>()
    File readmeFile = File.createTempFile("readme", "txt")
    readmeFile.deleteOnExit()
    if (Conflict.FAIL == conflict) {
      List<String> conflictingFiles = new ArrayList<>()
      ZipInputStream zipInputStream = new ZipInputStream(
          new FileInputStream(ADDON_SERVICE.getAddonLocalArchive(env.archivesDirectory, addon)))
      zipInputStream.withStream {
        ZipEntry entry
        while (entry = zipInputStream.nextEntry) {
          File destinationFile
          String fileName
          LOG.debug("ZIP entry : ${entry.name}")
          if (entry.isDirectory() || entry.name?.equalsIgnoreCase("README")) {
            // Do nothing
            continue
          } else if (entry.name ==~ /^.*jar$/) {
            // [AM_STRUCT_02] Add-ons libraries target directory
            fileName = FileUtils.extractFilename(entry.name)
            destinationFile = new File(env.platform.librariesDirectory, fileName)
          } else if (entry.name ==~ /^.*war$/) {
            // [AM_STRUCT_03] Add-ons webapps target directory
            fileName = FileUtils.extractFilename(entry.name)
            destinationFile = new File(env.platform.webappsDirectory, fileName)
          } else if (entry.name ==~ /^.*properties$/) {
            // [AM_STRUCT_07] Add-ons properties target directory
            fileName = FileUtils.extractParentAndFilename(entry.name)
            destinationFile = new File(env.platform.propertiesDirectory, fileName)
          } else {
            // see [AM_STRUCT_04] non war/jar files locations
            fileName = entry.name
            destinationFile = new File(env.platform.homeDirectory, fileName)
          }
          LOG.debug("Destination : ${destinationFile}")
          if (destinationFile.exists()) {
            conflictingFiles << fileName
          }
        }
      }
      if (conflictingFiles) {
        LOG.withStatusKO("Checking add-on archive")
        conflictingFiles.each { LOG.error("File ${it} already exists.") }
        throw new UnknownErrorException(
            "Installation aborted. Use --conflict=skip or --conflict=overwrite option to install it.")
      } else {
        LOG.withStatusOK("Checking add-on archive")
      }
    }
    // Process installation
    try {
      ZipInputStream zipInputStream = new ZipInputStream(
          new FileInputStream(ADDON_SERVICE.getAddonLocalArchive(env.archivesDirectory, addon)))
      zipInputStream.withStream {
        ZipEntry entry
        while (entry = zipInputStream.nextEntry) {
          String fileName
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
          } else if (entry.name ==~ /^.*jar$/) {
            // [AM_STRUCT_02] Add-ons libraries target directory
            fileName = FileUtils.extractFilename(entry.name)
            destinationFile = new File(env.platform.librariesDirectory, fileName)
            installationList = addon.installedLibraries
          } else if (entry.name ==~ /^.*war$/) {
            // [AM_STRUCT_03] Add-ons webapps target directory
            fileName = FileUtils.extractFilename(entry.name)
            destinationFile = new File(env.platform.webappsDirectory, fileName)
            installationList = addon.installedWebapps
          } else if (entry.name ==~ /^.*properties$/) {
            // [AM_STRUCT_07] Add-ons properties target directory
            fileName = FileUtils.extractParentAndFilename(entry.name)
            destinationFile = new File(env.platform.propertiesDirectory, fileName)
            installationList = addon.installedProperties
          } else {
            // see [AM_STRUCT_04] non war/jar files locations
            fileName = entry.name
            destinationFile = new File(env.platform.homeDirectory, fileName)
            installationList = addon.installedOthersFiles
          }
          LOG.debug("Destination : ${destinationFile}")
          if (!destinationFile.parentFile.exists()) {
            FileUtils.mkdirs(destinationFile.parentFile)
          }
          
          if (destinationFile.exists()) {
            switch (conflict) {
              case Conflict.OVERWRITE:
                LOG.warn("File ${destinationFile} already exists. Overwritten.")
                // Let's save it before
                File backupFile = new File(env.overwrittenFilesDirectory, "${addon.id}/${fileName}")
                if (!backupFile.parentFile.exists()) {
                  FileUtils.mkdirs(backupFile.parentFile)
                }
                copyFile("Archiving existing file ${destinationFile.name}", destinationFile, backupFile)
                addon.overwrittenFiles.add(fileName)
                break
              case Conflict.SKIP:
                LOG.warn("File ${destinationFile} already exists. Skipped.")
                continue // Next entry
            }
          }
          LOG.withStatus("Installing file ${destinationFile}") {
            FileOutputStream output = new FileOutputStream(destinationFile)
            output.withStream {
              int len = 0;
              byte[] buffer = new byte[4096]
              while ((len = zipInputStream.read(buffer)) > 0) {
                output.write(buffer, 0, len);
              }
            }
          }
          installationList.add(fileName)
        }
      }
      // Update application.xml if it exists
      File applicationDescriptorFile = new File(env.platform.webappsDirectory, "META-INF/application.xml")
      if (applicationDescriptorFile.exists()) {
        ADDON_SERVICE.processFileInplace(applicationDescriptorFile) { text ->
          GPathResult applicationXmlContent = new XmlSlurper(false, false).parseText(text)
          addon.installedWebapps.each { file ->
            String contextRoot = file.substring(file.lastIndexOf('/') + 1, file.length() - 4)
            String webUri = file.substring(file.lastIndexOf('/') + 1, file.length())
            LOG.withStatus("Adding context declaration /${contextRoot} for ${webUri} in application.xml") {
              applicationXmlContent.depthFirst().findAll {
                (it.name() == 'module') && (it.'web'.'web-uri'.text() == file)
              }.each { node ->
                // remove existing node
                node.replaceNode {}
              }
              applicationXmlContent."initialize-in-order" + {
                module {
                  web {
                    'web-uri'(webUri)
                    'context-root'(contextRoot)
                  }
                }
              }
            }
          }
          ADDON_SERVICE.serializeXml(applicationXmlContent)
        }
      }
    } finally {
      LOG.withStatus(
          "Recording installation details into ${ADDON_SERVICE.getAddonStatusFile(env.statusesDirectory, addon).name}") {
        new FileWriter(ADDON_SERVICE.getAddonStatusFile(env.statusesDirectory, addon)).withWriter { w ->
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
              installedProperties: addon.installedProperties,
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
      readmeFile.text.readLines().each {
        LOG.wrapLine(it, Console.get().width - Logger.Level.INFO.prefix.length() - 1).each {
          LOG.info(it)
          i++
          if (!batchMode && i == Console.get().height - 2) {
            LOG.info("@|yellow [Press any key to continue ...]|@")
            Console.get().read()
            i = 0
          }
        }
      }
      LOG.infoHR()
      readmeFile.delete()
    }
    LOG.withStatusOK("Add-on ${addon.id}:${addon.version} installed.")
  }
}
