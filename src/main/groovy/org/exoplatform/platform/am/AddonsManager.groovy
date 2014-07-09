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
import org.exoplatform.platform.am.ex.AddonsManagerException
import org.exoplatform.platform.am.ex.CommandLineParsingException
import org.exoplatform.platform.am.settings.EnvironmentSettings
import org.exoplatform.platform.am.utils.Console
import org.exoplatform.platform.am.utils.Logger

/*
 * Command line utility to manage add-ons inside a Platform installation.
 */

int returnCode = AddonsManagerConstants.RETURN_CODE_OK
Logger log = Logger.getInstance()
CommandLineParser clp
EnvironmentSettings env
CommandLineParameters commandLineParameters

try {
  // Initialize environment settings
  env = new EnvironmentSettings()

  // display header
  log.displayHeader(env.manager.version)

  // Initialize Add-ons manager settings
  clp = new CommandLineParser(env.manager.scriptName, Console.get().width)

  // Parse command line parameters and fill settings with user inputs
  commandLineParameters = clp.parse(args)

  // Show usage text when -h or --help option is used and exit
  if (commandLineParameters.help) {
    clp.usage()
    System.exit returnCode
  }

  // Instantiate our Service class to manage add-ons
  AddonService addonService = AddonService.getInstance()

  // And execute the required action
  switch (commandLineParameters.command) {
    case CommandLineParameters.Command.LIST:
      addonService.listAddons(env, commandLineParameters.commandList)
      break
    case CommandLineParameters.Command.DESCRIBE:
      addonService.describeAddon(env, commandLineParameters.commandDescribe)
      break
    case CommandLineParameters.Command.INSTALL:
      addonService.installAddon(env, commandLineParameters.commandInstall)
      break
    case CommandLineParameters.Command.UNINSTALL:
      addonService.uninstallAddon(env, commandLineParameters.commandUninstall)
      break
  }
} catch (CommandLineParsingException clpe) {
  log.error clpe.message
  clp.usage()
  returnCode = clpe.errorCode
} catch (AddonsManagerException ame) {
  log.error ame.message
  returnCode = ame.errorCode
} catch (Throwable t) {
  log.error t
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
