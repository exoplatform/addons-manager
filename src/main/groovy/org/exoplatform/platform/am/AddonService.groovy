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
import groovy.xml.StreamingMarkupBuilder
import groovy.xml.XmlUtil
import org.exoplatform.platform.am.settings.EnvironmentSettings
import org.exoplatform.platform.am.utils.AddonAlreadyInstalledException
import org.exoplatform.platform.am.utils.AddonsManagerException
import org.exoplatform.platform.am.utils.FileUtils
import org.exoplatform.platform.am.utils.Logger

/**
 * All services related to add-ons
 * @author Arnaud Héritier <aheritier@exoplatform.com>
 */
class AddonService {

  /**
   * Logger
   */
  private static final Logger LOG = Logger.get()

  EnvironmentSettings env

  CatalogService catalogService = new CatalogService()

  AddonService(EnvironmentSettings env) {
    this.env = env
  }

  File getLocalArchive(Addon addon) {
    return new File(env.archivesDirectory, "${addon.id}-${addon.version}.zip")
  }

  File getAddonStatusFile(Addon addon) {
    return getAddonStatusFile(addon.id)
  }

  File getAddonStatusFile(String addonId) {
    return new File(env.statusesDirectory, "${addonId}.status")
  }


  boolean isInstalled(Addon addon) {
    return getAddonStatusFile(addon).exists()
  }

  void install(Addon addon, boolean force, boolean noCache, boolean offline) {
    if (isInstalled(addon)) {
      if (!force) {
        throw new AddonAlreadyInstalledException(addon)
      } else {
        Addon oldAddon = catalogService.parseJSONAddon(getAddonStatusFile(addon).text);
        uninstall(oldAddon)
      }
    }
    if (noCache && getLocalArchive(addon).exists()) {
      LOG.withStatus("Deleting ${addon.name} ${addon.version} archive") {
        getLocalArchive(addon).delete()
      }
    }
    LOG.info("Installing @|yellow ${addon.name} ${addon.version}|@")
    if (!getLocalArchive(addon).exists()) {
      // Let's download it
      if (addon.downloadUrl.startsWith("http")) {
        if (offline) throw new AddonsManagerException(
            "${addon.name} ${addon.version} archive not found locally and offline mode activated")
        LOG.withStatus("Downloading add-on ${addon.name} ${addon.version}") {
          FileUtils.downloadFile(addon.downloadUrl, getLocalArchive(addon))
        }
      } else if (addon.downloadUrl.startsWith("file://")) {
        LOG.withStatus("Copying add-on ${addon.name} ${addon.version}") {
          FileUtils.copyFile(new File(env.addonsDirectory, addon.downloadUrl.replaceAll("file://", "")),
                             getLocalArchive(addon))
        }
      } else {
        throw new AddonsManagerException("Invalid or not supported download URL : ${addon.downloadUrl}")
      }
    }
    addon.installedLibraries = FileUtils.flatExtractFromZip(getLocalArchive(addon), env.platform.librariesDirectory, '^.*jar$')
    addon.installedWebapps = FileUtils.flatExtractFromZip(getLocalArchive(addon), env.platform.webappsDirectory, '^.*war$')
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
    LOG.withStatus("Recording installation details into ${getAddonStatusFile(addon).name}") {
      new FileWriter(getAddonStatusFile(addon)).withWriter { w ->
        StreamingJsonBuilder builder = new StreamingJsonBuilder(w)
        builder(
            id: addon.id,
            version: addon.version,
            name: addon.name,
            description: addon.description,
            releaseDate: addon.releaseDate,
            sourceUrl: addon.sourceUrl,
            screenshotUrl: addon.screenshotUrl,
            thumbnailUrl: addon.thumbnailUrl,
            documentationUrl: addon.documentationUrl,
            downloadUrl: addon.downloadUrl,
            vendor: addon.vendor,
            license: addon.license,
            supportedDistributions: addon.supportedDistributions,
            supportedApplicationServers: addon.supportedApplicationServers,
            installedLibraries: addon.installedLibraries,
            installedWebapps: addon.installedWebapps
        )
      }
    }
    LOG.withStatusOK("Add-on ${addon.name} ${addon.version} installed.")
  }

  void uninstall(Addon addon) {
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
    LOG.withStatus("Deleting installation details ${getAddonStatusFile(addon).name}") {
      getAddonStatusFile(addon).delete()
      assert !getAddonStatusFile(addon).exists()
    }
    LOG.withStatusOK("Add-on ${addon.name} ${addon.version} uninstalled")
  }

  private String serializeXml(GPathResult xml) {
    XmlUtil.serialize(new StreamingMarkupBuilder().bind {
      mkp.yield xml
    })
  }

  private processFileInplace(File file, Closure processText) {
    String text = file.text
    file.write(processText(text))
  }
}
