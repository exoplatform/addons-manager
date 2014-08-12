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

import groovy.util.slurpersupport.GPathResult
import org.exoplatform.platform.am.cli.CommandLineParameters
import org.exoplatform.platform.am.ex.AddonNotInstalledException
import org.exoplatform.platform.am.settings.EnvironmentSettings
import org.exoplatform.platform.am.utils.Logger

import static org.exoplatform.platform.am.utils.FileUtils.copyFile

/**
 * All services to install add-ons
 *
 * @author Arnaud Héritier <aheritier@exoplatform.com>
 */
public class AddonUninstallService {
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
  private static final AddonUninstallService singleton = new AddonUninstallService()

  /**
   * Factory
   *
   * @return The {@link AddonUninstallService} singleton instance
   */
  static AddonUninstallService getInstance() {
    return singleton
  }

  /**
   * You should use the singleton
   */
  private AddonUninstallService() {
  }

  /**
   * Uninstall an add-on given the current environment {@code env} and command line {@code parameters}.
   * @param env The execution environment
   * @param parameters Command line parameters for an uninstall action
   */
  void uninstallAddon(
      EnvironmentSettings env,
      CommandLineParameters.UninstallCommandParameters parameters) {
    File statusFile = ADDON_SERVICE.getAddonStatusFile(env.statusesDirectory, parameters.addonId)
    if (statusFile.exists()) {
      Addon addon
      LOG.withStatus("Loading add-on installation details") {
        addon = ADDON_SERVICE.createAddonFromJsonText(statusFile.text);
      }
      uninstallAddon(env, addon)
    } else {
      throw new AddonNotInstalledException("The add-on ${parameters.addonId} was not installed")
    }
  }

  /**
   * Uninstall the @{code addon} from the current @{code env}.
   * @param env The environment where the add-on must be uninstalled
   * @param addon The add-on to remove
   */
  protected void uninstallAddon(
      EnvironmentSettings env,
      Addon addon) {
    LOG.info("Uninstalling @|yellow ${addon.id}:${addon.version}|@")

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
        String contextRoot = webapp.substring(webapp.lastIndexOf('/')+1, webapp.length() - 4)
        String webUri = webapp.substring(webapp.lastIndexOf('/')+1, webapp.length())
        File fileToDelete = new File(env.platform.homeDirectory, webapp)
        if (!fileToDelete.exists()) {
          LOG.warn("No web application ${webapp} to delete")
        } else {
          LOG.withStatus("Deleting web application ${webapp}") {
            fileToDelete.delete()
            assert !fileToDelete.exists()
          }
        }
        if (applicationDescriptorFile.exists()) {
          LOG.withStatus("Removing context declaration /${contextRoot} for ${webUri} in application.xml") {
            ADDON_SERVICE.processFileInplace(applicationDescriptorFile) { text ->
              GPathResult applicationXmlContent = new XmlSlurper(false, false).parseText(text)
              applicationXmlContent.depthFirst().findAll {
                (it.name() == 'module') && (it.'web'.'web-uri'.text() == webUri)
              }.each { node ->
                // remove existing node
                node.replaceNode {}
              }
              ADDON_SERVICE.serializeXml(applicationXmlContent)
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
        copyFile("Reinstalling original file ${fileToRecover}", backupFile, originalFile)
        LOG.withStatus("Deleting backup file of ${fileToRecover}") {
          backupFile.delete()
        }
        LOG.warn("File ${fileToRecover} has been restored")
    }

    LOG.withStatus("Deleting installation details ${ADDON_SERVICE.getAddonStatusFile(env.statusesDirectory, addon).name}") {
      ADDON_SERVICE.getAddonStatusFile(env.statusesDirectory, addon).delete()
      assert !ADDON_SERVICE.getAddonStatusFile(env.statusesDirectory, addon).exists()
    }
    LOG.withStatusOK("Add-on ${addon.id}:${addon.version} uninstalled")
  }
}
