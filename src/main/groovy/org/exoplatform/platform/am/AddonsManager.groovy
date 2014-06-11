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
import org.exoplatform.platform.am.utils.Logging

import static org.fusesource.jansi.Ansi.ansi

/**
 * Command line utility to manage Platform addons.
 */

CommandLineParser clp

try {
// Initialize logging system
  Logging.initialize()

// Initialize environment settings
  EnvironmentSettings env = new EnvironmentSettings()

// display header
  Logging.displayHeader(env.manager.version)

// Initialize Add-ons manager settings
  clp = new CommandLineParser(env.manager.scriptName, Logging.CONSOLE_WIDTH)

// Parse command line parameters and fill settings with user inputs
  CommandLineParameters commandLineParameters = clp.parse(args)

// Display verbose details
  Logging.displayMsgVerbose("Environment Settings : ${env}")
  Logging.displayMsgVerbose("Command Line Settings : ${commandLineParameters}")

  // Show usage text when -h or --help option is used.
  if (commandLineParameters.help) {
    clp.usage()
    Logging.dispose()
    System.exit AddonsManagerConstants.RETURN_CODE_OK
  }

  List<Addon> addons = new ArrayList<Addon>()
  // Load add-ons list when listing them or installing one
  switch (commandLineParameters.command) {
    case [CommandLineParameters.Command.LIST, CommandLineParameters.Command.INSTALL]:
      // Let's load the list of available add-ons
      String catalog
      // Load the optional local list
      if (env.localAddonsCatalogFile.exists()) {
        Logging.logWithStatus("Reading local add-ons list...") {
          catalog = env.localAddonsCatalogFile.text
        }
        Logging.logWithStatus("Loading add-ons...") {
          addons.addAll(Addon.parseJSONAddonsList(catalog))
        }
      } else {
        Logging.displayMsgVerbose("No local catalog to load")
      }
      // Load the central list
      Logging.logWithStatus("Downloading central add-ons list...") {
        catalog = env.centralCatalogUrl.text
      }
      Logging.logWithStatus("Loading add-ons...") {
        addons.addAll(Addon.parseJSONAddonsList(catalog))
      }
      break
  }

  //
  switch (commandLineParameters.command) {
    case CommandLineParameters.Command.LIST:
      println ansi().render("\n@|bold Available add-ons:|@\n")
      addons.findAll { it.isStable() || commandLineParameters.commandList.snapshots }.groupBy { it.id }.each {
        Addon anAddon = it.value.first()
        printf(ansi().render("+ @|bold,yellow %-${addons.id*.size().max()}s|@ : @|bold %s|@, %s\n").toString(), anAddon.id,
               anAddon.name, anAddon.description)
        printf(ansi().render("     Available Version(s) : @|bold,yellow %-${addons.version*.size().max()}s|@ \n\n").toString(),
               ansi().render(it.value.collect { "@|yellow ${it.version}|@" }.join(', ')))
      }
      println ansi().render("""
  To install an add-on:
    ${env.manager.scriptName} install @|yellow addon|@
  """).toString()
      break
    case CommandLineParameters.Command.INSTALL:
      Addon addon
      if (commandLineParameters.commandInstall.addonVersion == null) {
        // Let's find the first add-on with the given id (including or not snapshots depending of the option)
        addon = addons.find {
          (it.isStable() || commandLineParameters.commandInstall.snapshots) && commandLineParameters.commandInstall.addonId.equals(
              it.id)
        }
        if (addon == null) {
          Logging.displayMsgError("No add-on with identifier ${commandLineParameters.commandInstall.addonId} found")
          Logging.dispose()
          System.exit AddonsManagerConstants.RETURN_CODE_KO
        }
      } else {
        // Let's find the add-on with the given id and version
        addon = addons.find {
          commandLineParameters.commandInstall.addonId.equals(
              it.id) && commandLineParameters.commandInstall.addonVersion.equalsIgnoreCase(
              it.version)
        }
        if (addon == null) {
          Logging.displayMsgError(
              "No add-on with identifier ${commandLineParameters.commandInstall.addonId} and version ${commandLineParameters.commandInstall.addonVersion} found")
          Logging.dispose()
          System.exit AddonsManagerConstants.RETURN_CODE_KO
        }
      }
      addon.install(env.addonsDirectory, env.archivesDirectory,
                    env.statusesDirectory,
                    env.platform.librariesDirectory,
                    env.platform.webappsDirectory, commandLineParameters.commandInstall.force)
      break
    case CommandLineParameters.Command.UNINSTALL:
      File statusFile = Addon.getAddonStatusFile(env.statusesDirectory,
                                                commandLineParameters.commandUninstall.addonId)
      if (statusFile.exists()) {
        Addon addon
        Logging.logWithStatus("Loading add-on details...") {
          addon = Addon.parseJSONAddon(statusFile.text);
        }
        addon.uninstall(env.statusesDirectory,
                        env.platform.librariesDirectory,
                        env.platform.webappsDirectory)
      } else {
        Logging.logWithStatusKO("Add-on not installed. Exiting.")
        Logging.dispose()
        System.exit AddonsManagerConstants.RETURN_CODE_KO
      }
      break
  }
} catch (CommandLineParsingException clpe) {
  Logging.displayMsgError("Invalid command line parameter(s) : ${clpe.message}")
  println()
  clp.usage()
  System.exit AddonsManagerConstants.RETURN_CODE_KO
} catch (AddonsManagerException ame) {
  Logging.displayMsgError ame.message
  println()
  System.exit AddonsManagerConstants.RETURN_CODE_KO
} catch (Throwable t) {
  Logging.displayThrowable(t)
  println()
  System.exit AddonsManagerConstants.RETURN_CODE_KO
} finally {
  Logging.dispose()
}
println()
System.exit AddonsManagerConstants.RETURN_CODE_OK
