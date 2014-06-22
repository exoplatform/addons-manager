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

import org.exoplatform.platform.am.cli.CommandLineParameters
import org.exoplatform.platform.am.cli.CommandLineParser
import org.exoplatform.platform.am.cli.CommandLineParsingException
import org.exoplatform.platform.am.settings.EnvironmentSettings
import org.exoplatform.platform.am.utils.AddonAlreadyInstalledException
import org.exoplatform.platform.am.utils.AddonsManagerException
import org.exoplatform.platform.am.utils.Console
import org.exoplatform.platform.am.utils.Logger

/*
 * Command line utility to manage Platform addons.
 */

CommandLineParser clp
EnvironmentSettings env
CommandLineParameters commandLineParameters
int returnCode = AddonsManagerConstants.RETURN_CODE_OK
/**
 * Logger
 */
Logger log = Logger.get()
try {
// Initialize environment settings
  env = new EnvironmentSettings()

// display header
  log.displayHeader(env.manager.version)

// Initialize Add-ons manager settings
  clp = new CommandLineParser(env.manager.scriptName, Console.get().width)

// Parse command line parameters and fill settings with user inputs
  commandLineParameters = clp.parse(args)

  // Show usage text when -h or --help option is used.
  if (commandLineParameters.help) {
    clp.usage()
    System.exit AddonsManagerConstants.RETURN_CODE_OK
  }

  AddonService addonService = new AddonService(env)
  CatalogService catalogService = new CatalogService()

  switch (commandLineParameters.command) {
    case CommandLineParameters.Command.LIST:
      List<Addon> addons = catalogService.loadAddons(
          commandLineParameters.commandList.catalog ? commandLineParameters.commandList.catalog : env.remoteCatalogUrl,
          commandLineParameters.commandList.noCache,
          env.catalogsCacheDirectory,
          commandLineParameters.commandList.offline,
          env.localAddonsCatalogFile,
          env.platform.distributionType,
          env.platform.appServerType)
      if (addons.size() > 0) {
        log.info "\n@|bold Available add-ons:|@"
        addons.findAll {
          (!it.isSnapshot() || commandLineParameters.commandList.snapshots) && (!it.unstable || commandLineParameters.commandList.unstable)
        }.groupBy { it.id }.each {
          Addon anAddon = it.value.first()
          log.info String.format("\n+ @|bold,yellow %-${addons.id*.size().max()}s|@ : @|bold %s|@, %s", anAddon.id,
                                 anAddon.name, anAddon.description)
          log.info String.format("     Available Version(s) : %s", it.value.collect { "@|yellow ${it.version}|@" }.join(', '))
        }
        log.info String.format("""
  To install an add-on:
    ${env.manager.scriptName} install @|yellow addon|@
  """)
      } else {
        log.warn("No add-on found in remote and local catalogs")
      }
      break
    case CommandLineParameters.Command.INSTALL:
      Addon addon
      List<Addon> addons = catalogService.loadAddons(
          commandLineParameters.commandInstall.catalog ? commandLineParameters.commandInstall.catalog : env.remoteCatalogUrl,
          commandLineParameters.commandInstall.noCache,
          env.catalogsCacheDirectory,
          commandLineParameters.commandInstall.offline,
          env.localAddonsCatalogFile,
          env.platform.distributionType,
          env.platform.appServerType)
      if (commandLineParameters.commandInstall.addonVersion == null) {
        // Let's find the first add-on with the given id (including or not snapshots depending of the option)
        addon = addons.find {
          (!it.isSnapshot() || commandLineParameters.commandInstall.snapshots) && (!it.unstable || commandLineParameters
              .commandInstall.unstable) && commandLineParameters.commandInstall.addonId.equals(it.id)
        }
        if (addon == null) {
          log.error("No add-on with identifier ${commandLineParameters.commandInstall.addonId} found")
          returnCode = AddonsManagerConstants.RETURN_CODE_ADDON_NOT_FOUND
          break
        }
      } else {
        // Let's find the add-on with the given id and version
        addon = addons.find {
          commandLineParameters.commandInstall.addonId.equals(
              it.id) && commandLineParameters.commandInstall.addonVersion.equalsIgnoreCase(
              it.version)
        }
        if (addon == null) {
          log.error(
              "No add-on with identifier ${commandLineParameters.commandInstall.addonId} and version ${commandLineParameters.commandInstall.addonVersion} found")
          returnCode = AddonsManagerConstants.RETURN_CODE_ADDON_NOT_FOUND
          break
        }
      }
      addonService.install(
          addon,
          commandLineParameters.commandInstall.force,
          commandLineParameters.commandInstall.noCache,
          commandLineParameters.commandInstall.offline)
      break
    case CommandLineParameters.Command.UNINSTALL:
      File statusFile = addonService.getAddonStatusFile(commandLineParameters.commandUninstall.addonId)
      if (statusFile.exists()) {
        Addon addon
        log.withStatus("Loading add-on details") {
          addon = catalogService.parseJSONAddon(statusFile.text);
        }
        addonService.uninstall(addon)
      } else {
        log.error("Add-on not installed. It cannot be uninstalled.")
        returnCode = AddonsManagerConstants.RETURN_CODE_ADDON_NOT_INSTALLED
      }
      break
  }
} catch (CommandLineParsingException clpe) {
  log.error("Invalid command line parameter(s) : ${clpe.message}")
  clp.usage()
  returnCode = AddonsManagerConstants.RETURN_CODE_INVALID_COMMAND_LINE_PARAMS
} catch (AddonAlreadyInstalledException aaie) {
  log.error aaie.message
  returnCode = AddonsManagerConstants.RETURN_CODE_ADDON_ALREADY_INSTALLED
} catch (AddonsManagerException ame) {
  log.error ame.message
  returnCode = AddonsManagerConstants.RETURN_CODE_UNKNOWN_ERROR
} catch (Throwable t) {
  log.error(t)
  returnCode = AddonsManagerConstants.RETURN_CODE_UNKNOWN_ERROR
}
// Display various details for debug purposes
log.debug("Console", Console.get()?.properties, ["class", "err", "out", "in"])
log.debug("Environment Settings", env?.properties, ["class", "platform", "manager"])
log.debug("Platform Settings", env?.platform?.properties, ["class"])
log.debug("Manager Settings", env?.manager, ["class"])
log.debug("Command Line Global Parameters", commandLineParameters?.properties,
          ["class", "commandList", "commandInstall", "commandUninstall"])
log.debug("Command Line List Parameters", commandLineParameters?.commandList?.properties, ["class"])
log.debug("Command Line Install Parameters", commandLineParameters?.commandInstall?.properties, ["class"])
log.debug("Command Line Uninstall Parameters", commandLineParameters?.commandUninstall?.properties, ["class"])
log.debug("System Properties", System.properties, ["class"])
System.exit returnCode
