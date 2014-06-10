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
import org.exoplatform.platform.am.settings.AddonsManagerSettings
import org.exoplatform.platform.am.settings.EnvironmentSettings
import org.exoplatform.platform.am.settings.PlatformSettings
import org.exoplatform.platform.am.utils.AddonsManagerException
import org.exoplatform.platform.am.utils.Logging

import static org.fusesource.jansi.Ansi.ansi

/**
 * Command line utility to manage Platform addons.
 */

def clp

try {
// Initialize logging system
  Logging.initialize()
  def managerSettings = new AddonsManagerSettings()
// display header
  Logging.displayHeader(managerSettings.version)
  clp = new CommandLineParser(managerSettings.getScriptName(), Logging.CONSOLE_WIDTH)
  def platformSettings = new PlatformSettings()
  def environmentSettings = new EnvironmentSettings(managerSettings, platformSettings)
// Parse command line parameters and fill settings with user inputs
  environmentSettings.commandLineArgs = clp.parse(args)

  Logging.displayMsgVerbose("Manager Settings :\n${managerSettings.describe()}\n")
  Logging.displayMsgVerbose("Platform Settings :\n${platformSettings.describe()}\n")
  Logging.displayMsgVerbose("Environment Settings :\n${environmentSettings.describe()}\n")

  // Show usage text when -h or --help option is used.
  if (environmentSettings.commandLineArgs.help) {
    clp.usage()
    Logging.dispose()
    System.exit AddonsManagerConstants.RETURN_CODE_OK
  }

  if (!environmentSettings.validate()) {
    Logging.dispose()
    System.exit AddonsManagerConstants.RETURN_CODE_KO
  }

  def List<Addon> addons = new ArrayList<Addon>()
  // Load add-ons list when listing them or installing one
  switch (environmentSettings.commandLineArgs.command) {
    case [CommandLineParameters.Command.LIST, CommandLineParameters.Command.INSTALL]:
      // Let's load the list of available add-ons
      def catalog
      // Load the optional local list
      if (environmentSettings.localAddonsCatalogFile.exists()) {
        Logging.logWithStatus("Reading local add-ons list...") {
          catalog = environmentSettings.localAddonsCatalog
        }
        Logging.logWithStatus("Loading add-ons...") {
          addons.addAll(Addon.parseJSONAddonsList(catalog, environmentSettings))
        }
      } else {
        Logging.displayMsgVerbose("No local catalog to load")
      }
      // Load the central list
      Logging.logWithStatus("Downloading central add-ons list...") {
        catalog = environmentSettings.centralCatalog
      }
      Logging.logWithStatus("Loading add-ons...") {
        addons.addAll(Addon.parseJSONAddonsList(catalog, environmentSettings))
      }
  }

  //
  switch (environmentSettings.commandLineArgs.command) {
    case CommandLineParameters.Command.LIST:
      println ansi().render("\n@|bold Available add-ons:|@\n")
      addons.findAll { it.isStable() || environmentSettings.commandLineArgs.commandList.snapshots }.groupBy { it.id }.each {
        Addon anAddon = it.value.first()
        printf(ansi().render("+ @|bold,yellow %-${addons.id*.size().max()}s|@ : @|bold %s|@, %s\n").toString(), anAddon.id,
               anAddon.name, anAddon.description)
        printf(ansi().render("     Available Version(s) : @|bold,yellow %-${addons.version*.size().max()}s|@ \n\n").toString(),
               ansi().render(it.value.collect { "@|yellow ${it.version}|@" }.join(', ')))
      }
      println ansi().render("""
  To install an add-on:
    ${managerSettings.getScriptName()} --install @|yellow addon|@
  """).toString()
      break
    case CommandLineParameters.Command.INSTALL:
      def addon
      if (environmentSettings.commandLineArgs.commandInstall.addonVersion == null) {
        // Let's find the first add-on with the given id (including or not snapshots depending of the option)
        addon = addons.find {
          (it.isStable() || environmentSettings.commandLineArgs.commandInstall.snapshots) && environmentSettings.commandLineArgs.commandInstall.addonId.equals(
              it.id)
        }
        if (addon == null) {
          Logging.displayMsgError("No add-on with identifier ${environmentSettings.commandLineArgs.commandInstall.addonId} found")
          Logging.dispose()
          System.exit AddonsManagerConstants.RETURN_CODE_KO
        }
      } else {
        // Let's find the add-on with the given id and version
        addon = addons.find {
          environmentSettings.commandLineArgs.commandInstall.addonId.equals(
              it.id) && environmentSettings.commandLineArgs.commandInstall.addonVersion.equalsIgnoreCase(
              it.version)
        }
        if (addon == null) {
          Logging.displayMsgError(
              "No add-on with identifier ${environmentSettings.commandLineArgs.commandInstall.addonId} and version ${environmentSettings.commandLineArgs.commandInstall.addonVersion} found")
          Logging.dispose()
          System.exit AddonsManagerConstants.RETURN_CODE_KO
        }
      }
      addon.install()
      break
    case CommandLineParameters.Command.UNINSTALL:
      def statusFile = Addon.getAddonStatusFile(environmentSettings.addonsDirectory,
                                                environmentSettings.commandLineArgs.commandUninstall.addonId)
      if (statusFile.exists()) {
        def addon
        Logging.logWithStatus("Loading add-on details...") {
          addon = Addon.parseJSONAddon(statusFile.text, environmentSettings);
        }
        addon.uninstall()
      } else {
        Logging.logWithStatusKO("Add-on not installed. Exiting.")
        Logging.dispose()
        System.exit AddonsManagerConstants.RETURN_CODE_KO
      }
      break
  }
} catch (CommandLineParsingException clpe) {
  Logging.displayMsgError("Invalid command line parameter(s) : " + clpe.message)
  println()
  clp.usage()
  System.exit AddonsManagerConstants.RETURN_CODE_KO
} catch (AddonsManagerException ame) {
  Logging.displayMsgError "${ame.message}"
  println()
  System.exit AddonsManagerConstants.RETURN_CODE_KO
} catch (Exception e) {
  Logging.displayThrowable(e)
  println()
  System.exit AddonsManagerConstants.RETURN_CODE_KO
} finally {
  Logging.dispose()
}
println()
System.exit AddonsManagerConstants.RETURN_CODE_OK
