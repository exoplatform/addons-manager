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
import groovy.transform.Canonical
import groovy.util.slurpersupport.GPathResult
import groovy.xml.StreamingMarkupBuilder
import groovy.xml.XmlUtil
import org.exoplatform.platform.am.utils.AddonsManagerException
import org.exoplatform.platform.am.utils.FileUtils
import org.exoplatform.platform.am.utils.Logging

@Canonical
class Addon {

  String id
  String version
  String name
  String description
  String releaseDate
  String sourceUrl
  String screenshotUrl
  String thumbnailUrl
  String documentationUrl
  String downloadUrl
  String vendor
  String license
  List<String> supportedDistributions
  List<String> supportedApplicationServers
  List<String> installedLibraries
  List<String> installedWebapps

  static Addon fromJSON(anAddon) {
    Addon addonObj = new Addon(
        id: anAddon.id ? anAddon.id : 'N/A',
        version: anAddon.version ? anAddon.version : 'N/A');
    addonObj.name = anAddon.name ? anAddon.name : 'N/A'
    addonObj.description = anAddon.description ? anAddon.description : 'N/A'
    addonObj.releaseDate = anAddon.releaseDate ? anAddon.releaseDate : 'N/A'
    addonObj.sourceUrl = anAddon.sourceUrl ? anAddon.sourceUrl : 'N/A'
    addonObj.screenshotUrl = anAddon.screenshotUrl ? anAddon.screenshotUrl : 'N/A'
    addonObj.thumbnailUrl = anAddon.thumbnailUrl ? anAddon.thumbnailUrl : 'N/A'
    addonObj.documentationUrl = anAddon.documentationUrl ? anAddon.documentationUrl : 'N/A'
    addonObj.downloadUrl = anAddon.downloadUrl ? anAddon.downloadUrl : 'N/A'
    addonObj.vendor = anAddon.vendor ? anAddon.vendor : 'N/A'
    addonObj.license = anAddon.license ? anAddon.license : 'N/A'
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
    addonObj.installedLibraries = anAddon.installedLibraries ? anAddon.installedLibraries : []
    addonObj.installedWebapps = anAddon.installedWebapps ? anAddon.installedWebapps : []
    // TODO : Add some validations here
    return addonObj
  }

  static List<Addon> parseJSONAddonsList(String text) {
    List<Addon> addonsList = new ArrayList<Addon>();
    new JsonSlurper().parseText(text).each { anAddon ->
      addonsList.add(fromJSON(anAddon))
    }
    return addonsList
  }

  static Addon parseJSONAddon(String text) {
    return fromJSON(new JsonSlurper().parseText(text))
  }

  File getLocalArchive(File archivesDirectory) {
    return new File(archivesDirectory, "${id}-${version}.zip")
  }

  File getAddonStatusFile(File statusesDirectory) {
    return Addon.getAddonStatusFile(statusesDirectory, id)
  }

  static File getAddonStatusFile(File statusesDirectory, String id) {
    return new File(statusesDirectory, "${id}.status")
  }


  boolean isInstalled(File statusesDirectory) {
    return getAddonStatusFile(statusesDirectory).exists()
  }

  boolean isStable() {
    return !(version =~ '.*SNAPSHOT$')
  }

  void install(File addonsDirectory, File archivesDirectory, File statusesDirectory, File librariesDirectory,
               File webappsDirectory, boolean force) {
    if (isInstalled(statusesDirectory)) {
      if (!force) {
        throw new AddonsManagerException("Add-on already installed. Use --force to enforce to override it")
      } else {
        Addon oldAddon = Addon.parseJSONAddon(getAddonStatusFile(statusesDirectory).text);
        oldAddon.uninstall(statusesDirectory, librariesDirectory, webappsDirectory)
      }
    }
    Logging.displayMsgInfo("Installing @|yellow ${name} ${version}|@ ...")
    if (!getLocalArchive(archivesDirectory).exists() || force) {
      // Let's download it
      if (downloadUrl.startsWith("http")) {
        Logging.logWithStatus("Downloading add-on ${name} ${version} ...") {
          FileUtils.downloadFile(downloadUrl, getLocalArchive(archivesDirectory))
        }
      } else if (downloadUrl.startsWith("file://")) {
        Logging.logWithStatus("Copying add-on ${name} ${version} ...") {
          FileUtils.copyFile(new File(addonsDirectory, downloadUrl.replaceAll("file://", "")),
                             getLocalArchive(archivesDirectory))
        }
      } else {
        throw new AddonsManagerException("Invalid or not supported download URL : ${downloadUrl}")
      }
    }
    installedLibraries = FileUtils.flatExtractFromZip(getLocalArchive(archivesDirectory), librariesDirectory, '^.*jar$')
    installedWebapps = FileUtils.flatExtractFromZip(getLocalArchive(archivesDirectory), webappsDirectory, '^.*war$')
    // Update application.xml if it exists
    File applicationDescriptorFile = new File(webappsDirectory, "META-INF/application.xml")
    if (applicationDescriptorFile.exists()) {
      processFileInplace(applicationDescriptorFile) { text ->
        GPathResult applicationXmlContent = new XmlSlurper(false, false).parseText(text)
        installedWebapps.each { file ->
          String webContext = file.substring(0, file.length() - 4)
          Logging.logWithStatus("Adding context declaration /${webContext} for ${file} in application.xml ... ") {
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
    Logging.logWithStatus("Recording installation details into ${getAddonStatusFile(statusesDirectory).name} ... ") {
      new FileWriter(getAddonStatusFile(statusesDirectory)).withWriter { w ->
        StreamingJsonBuilder builder = new StreamingJsonBuilder(w)
        builder(
            id: id,
            version: version,
            name: name,
            description: description,
            releaseDate: releaseDate,
            sourceUrl: sourceUrl,
            screenshotUrl: screenshotUrl,
            thumbnailUrl: thumbnailUrl,
            documentationUrl: documentationUrl,
            downloadUrl: downloadUrl,
            vendor: vendor,
            license: license,
            supportedDistributions: supportedDistributions,
            supportedApplicationServers: supportedApplicationServers,
            installedLibraries: installedLibraries,
            installedWebapps: installedWebapps
        )
      }
    }
    Logging.logWithStatusOK("Add-on ${name} ${version} installed.")
  }

  void uninstall(File statusesDirectory, File librariesDirectory, File webappsDirectory) {
    Logging.displayMsgInfo("Uninstalling @|yellow ${name} ${version}|@ ...")

    installedLibraries.each {
      library ->
        File fileToDelete = new File(librariesDirectory, library)
        if (!fileToDelete.exists()) {
          Logging.displayMsgWarn("No library ${library} to delete")
        } else {
          Logging.logWithStatus("Deleting library ${library} ... ") {
            fileToDelete.delete()
            assert !fileToDelete.exists()
          }
        }
    }

    // Update application.xml if it exists
    File applicationDescriptorFile = new File(webappsDirectory, "META-INF/application.xml")

    installedWebapps.each {
      webapp ->
        File fileToDelete = new File(webappsDirectory, webapp)
        String webContext = webapp.substring(0, webapp.length() - 4)
        if (!fileToDelete.exists()) {
          Logging.displayMsgWarn("No web application ${webapp} to delete")
        } else {
          Logging.logWithStatus("Deleting web application ${webapp} ... ") {
            fileToDelete.delete()
            assert !fileToDelete.exists()
          }
        }
        if (applicationDescriptorFile.exists()) {
          Logging.logWithStatus("Adding context declaration /${webContext} for ${webapp} in application.xml ...") {
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
    Logging.logWithStatus("Deleting installation details ${getAddonStatusFile(statusesDirectory).name} ... ") {
      getAddonStatusFile(statusesDirectory).delete()
      assert !getAddonStatusFile(statusesDirectory).exists()
    }
    Logging.logWithStatusOK("Add-on ${name} ${version} uninstalled.")
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