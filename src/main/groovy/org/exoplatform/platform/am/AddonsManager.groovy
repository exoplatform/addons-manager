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
import org.exoplatform.platform.am.utils.*

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
Logger log = Logger.getInstance()
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

  AddonService addonService = AddonService.getInstance()

  switch (commandLineParameters.command) {
    case CommandLineParameters.Command.LIST:
      returnCode = addonService.listAddons(env, commandLineParameters.commandList)
      break
    case CommandLineParameters.Command.DESCRIBE:
      returnCode = addonService.describeAddon(env, commandLineParameters.commandDescribe)
      break
    case CommandLineParameters.Command.INSTALL:
      returnCode = addonService.installAddon(env, commandLineParameters.commandInstall)
      break
    case CommandLineParameters.Command.UNINSTALL:
      returnCode = addonService.uninstallAddon(env, commandLineParameters.commandUninstall)
      break
  }
} catch (CommandLineParsingException clpe) {
  log.error("Invalid command line parameter(s) : ${clpe.message}")
  clp.usage()
  returnCode = AddonsManagerConstants.RETURN_CODE_INVALID_COMMAND_LINE_PARAMS
} catch (AddonAlreadyInstalledException aaie) {
  log.error aaie.message
  returnCode = AddonsManagerConstants.RETURN_CODE_ADDON_ALREADY_INSTALLED
} catch (CompatibilityException aie) {
  log.error aie.message
  returnCode = AddonsManagerConstants.RETURN_CODE_ADDON_INCOMPATIBLE
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
