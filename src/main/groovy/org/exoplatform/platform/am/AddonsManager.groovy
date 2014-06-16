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

  switch (commandLineParameters.command) {
    case CommandLineParameters.Command.LIST:
      List<Addon> addons = addonService.loadAddons(commandLineParameters.commandList.catalog ? commandLineParameters.commandList
          .catalog : env.centralCatalogUrl)
      log.info "\n@|bold Available add-ons:|@"
      addons.findAll { it.isStable() || commandLineParameters.commandList.snapshots }.groupBy { it.id }.each {
        Addon anAddon = it.value.first()
        log.info String.format("\n+ @|bold,yellow %-${addons.id*.size().max()}s|@ : @|bold %s|@, %s", anAddon.id,
                               anAddon.name, anAddon.description)
        log.info String.format("     Available Version(s) : %s", it.value.collect { "@|yellow ${it.version}|@" }.join(', '))
      }
      log.info String.format("""
  To install an add-on:
    ${env.manager.scriptName} install @|yellow addon|@
  """)
      break
    case CommandLineParameters.Command.INSTALL:
      Addon addon
      List<Addon> addons = addonService.loadAddons(commandLineParameters.commandInstall.catalog ? commandLineParameters
          .commandInstall.catalog : env.centralCatalogUrl)
      if (commandLineParameters.commandInstall.addonVersion == null) {
        // Let's find the first add-on with the given id (including or not snapshots depending of the option)
        addon = addons.find {
          (it.isStable() || commandLineParameters.commandInstall.snapshots) && commandLineParameters.commandInstall.addonId.equals(
              it.id)
        }
        if (addon == null) {
          log.error("No add-on with identifier ${commandLineParameters.commandInstall.addonId} found")
          returnCode = AddonsManagerConstants.RETURN_CODE_KO
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
          returnCode = AddonsManagerConstants.RETURN_CODE_KO
          break
        }
      }
      addonService.install(addon, commandLineParameters.commandInstall.force)
      break
    case CommandLineParameters.Command.UNINSTALL:
      File statusFile = addonService.getAddonStatusFile(commandLineParameters.commandUninstall.addonId)
      if (statusFile.exists()) {
        Addon addon
        log.withStatus("Loading add-on details") {
          addon = addonService.parseJSONAddon(statusFile.text);
        }
        addonService.uninstall(addon)
      } else {
        log.error("Add-on not installed. It cannot be uninstalled.")
        returnCode = AddonsManagerConstants.RETURN_CODE_KO
      }
      break
  }
} catch (CommandLineParsingException clpe) {
  log.error("Invalid command line parameter(s) : ${clpe.message}")
  clp.usage()
  returnCode = AddonsManagerConstants.RETURN_CODE_KO
} catch (AddonsManagerException ame) {
  log.error ame.message
  returnCode = AddonsManagerConstants.RETURN_CODE_KO
} catch (Throwable t) {
  log.error(t)
  returnCode = AddonsManagerConstants.RETURN_CODE_KO
}
// Display details if verbose enabled
log.debug("Console : ${Console.get().properties}")
log.debug("Environment Settings : ${env}")
log.debug("Command Line Settings : ${commandLineParameters}")
System.exit returnCode
