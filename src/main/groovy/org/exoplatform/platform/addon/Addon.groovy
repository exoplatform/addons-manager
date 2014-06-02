/**
 * Copyright (C) 2003-2014 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.exoplatform.platform.addon

import groovy.json.JsonSlurper
import groovy.json.StreamingJsonBuilder
import groovy.util.slurpersupport.GPathResult
import groovy.xml.StreamingMarkupBuilder
import groovy.xml.XmlUtil

@groovy.transform.Canonical
class Addon {

  def EnvironmentSettings environmentSettings
  def String id
  def String version
  def String name
  def String description
  def String releaseDate
  def String sourceUrl
  def String screenshotUrl
  def String thumbnailUrl
  def String documentationUrl
  def String downloadUrl
  def String vendor
  def String license
  def List<String> supportedDistributions
  def List<String> supportedApplicationServers
  def List<String> installedLibraries
  def List<String> installedWebapps

  static Addon fromJSON(anAddon, EnvironmentSettings environmentSettings) {
    def addonObj = new Addon(
        anAddon.id ? anAddon.id : 'N/A',
        anAddon.version ? anAddon.version : 'N/A',
        environmentSettings);
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

  static List<Addon> parseJSONAddonsList(String text, EnvironmentSettings environmentSettings) {
    List<Addon> addonsList = new ArrayList<Addon>();
    new JsonSlurper().parseText(text).each { anAddon ->
      addonsList.add(fromJSON(anAddon, environmentSettings))
    }
    return addonsList
  }

  static Addon parseJSONAddon(String text, EnvironmentSettings environmentSettings) {
    return fromJSON(new JsonSlurper().parseText(text), environmentSettings)
  }

  Addon(String id, String version, EnvironmentSettings environmentSettings) {
    this.id = id
    this.version = version
    this.environmentSettings = environmentSettings
  }

  File getLocalArchive() {
    File archivesDirectory = new File(environmentSettings.addonsDirectory, "archives")
    if (!archivesDirectory.exists()) {
      MiscUtils.mkdirs(archivesDirectory)
    }
    return new File(archivesDirectory, id + "-" + version + ".zip")
  }

  static File getAddonStatusFile(File addonsDir, String id) {
    File statusesDirectory = new File(addonsDir, "statuses")
    if (!statusesDirectory.exists()) {
      MiscUtils.mkdirs(statusesDirectory)
    }
    return new File(statusesDirectory, "${id}.status")
  }


  File getAddonStatusFile() {
    getAddonStatusFile(environmentSettings.addonsDirectory, id)
  }

  boolean isInstalled() {
    return addonStatusFile.exists()
  }

  boolean isStable() {
    return !(this.version =~ '.*SNAPSHOT$')
  }

  def install() {
    if (installed) {
      if (!environmentSettings.cliArgs.force) {
        throw new Exception("Add-on already installed. Use --force to enforce to override it")
      } else {
        Addon oldAddon = Addon.parseJSONAddon(addonStatusFile.text, environmentSettings);
        oldAddon.uninstall()
      }
    }
    Logging.displayMsgInfo("Installing @|yellow ${name} ${version}|@ ...")
    if (!localArchive.exists() || environmentSettings.cliArgs.force) {
      // Let's download it
      if (downloadUrl.startsWith("http")) {
        Logging.logWithStatus("Downloading add-on ${name} ${version} ...") {
          MiscUtils.downloadFile(downloadUrl, localArchive)
        }
      } else if (downloadUrl.startsWith("file://")) {
        Logging.logWithStatus("Copying add-on ${name} ${version} ...") {
          MiscUtils.copyFile(new File(environmentSettings.addonsDirectory, downloadUrl.replaceAll("file://", "")),
                             localArchive)
        }
      } else {
        throw new Exception("Invalid or not supported download URL : ${downloadUrl}")
      }
    }
    this.installedLibraries = MiscUtils.flatExtractFromZip(localArchive, environmentSettings.platformSettings.librariesDirectory,
                                                           '^.*jar$')
    this.installedWebapps = MiscUtils.flatExtractFromZip(localArchive, environmentSettings.platformSettings.webappsDirectory,
                                                         '^.*war$')
    // Update application.xml if it exists
    def applicationDescriptorFile = new File(environmentSettings.platformSettings.webappsDirectory, "META-INF/application.xml")
    if (applicationDescriptorFile.exists()) {
      processFileInplace(applicationDescriptorFile) { text ->
        def applicationXmlContent = new XmlSlurper(false, false).parseText(text)
        installedWebapps.each { file ->
          def webContext = file.substring(0, file.length() - 4)
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
    Logging.logWithStatus("Recording installation details into ${addonStatusFile.name} ... ") {
      new FileWriter(addonStatusFile).withWriter { w ->
        def builder = new StreamingJsonBuilder(w)
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

  void uninstall() {
    Logging.displayMsgInfo("Uninstalling @|yellow ${name} ${version}|@ ...")

    installedLibraries.each {
      library ->
        def File fileToDelete = new File(environmentSettings.platformSettings.librariesDirectory, library)
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
    def applicationDescriptorFile = new File(environmentSettings.platformSettings.webappsDirectory, "META-INF/application.xml")

    installedWebapps.each {
      webapp ->
        def File fileToDelete = new File(environmentSettings.platformSettings.webappsDirectory, webapp)
        def webContext = webapp.substring(0, webapp.length() - 4)
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
              def applicationXmlContent = new XmlSlurper(false, false).parseText(text)
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
    Logging.logWithStatus("Deleting installation details ${addonStatusFile.name} ... ") {
      addonStatusFile.delete()
      assert !addonStatusFile.exists()
    }
    Logging.logWithStatusOK("Add-on ${name} ${version} uninstalled.")
  }

  private String serializeXml(GPathResult xml) {
    XmlUtil.serialize(new StreamingMarkupBuilder().bind {
      mkp.yield xml
    })
  }

  private processFileInplace(File file, Closure processText) {
    def text = file.text
    file.write(processText(text))
  }

}

