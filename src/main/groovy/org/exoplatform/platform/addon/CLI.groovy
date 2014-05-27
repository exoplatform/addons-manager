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

import com.beust.jcommander.JCommander
import com.beust.jcommander.ParameterException

class CLI {

  final static int RETURN_CODE_OK = 0
  final static int RETURN_CODE_KO = 1

  static String getScriptName() {
    def scriptBaseName = "addons"
    // Computes the script addon from the OS
    def scriptName = "${scriptBaseName}.sh"
    if (System.properties['os.name'].toLowerCase().contains('windows')) {
      scriptName = "${scriptBaseName}.bat"
    }
    return scriptName
  }

  /**
   * Initialize settings from command line parameters
   * @param args Command line parameters
   * @return a ManagerSettings instance or null if something went wrong
   */
  static ManagerCLIArgs initialize(String[] args, ManagerCLIArgs managerCLIArgs) {

    def JCommander cli = new JCommander(managerCLIArgs);
    cli.setColumnSize(Logging.CONSOLE_WIDTH);
    cli.setProgramName(CLI.getScriptName());
    try {
      cli.parse(args);
    } catch (ParameterException pe) {
      managerCLIArgs.action = ManagerCLIArgs.Action.HELP
      Logging.displayMsgError("Invalid command line parameter(s)")
      cli.usage()
      return null
    }

    if (managerCLIArgs.verbose) {
      Logging.verbose = true
      Logging.displayMsgVerbose("Verbose logs activated")
    }

    // Show usage text when -h or --help option is used.
    if (managerCLIArgs.helpAction) {
      managerCLIArgs.action = ManagerCLIArgs.Action.HELP
      cli.usage()
      return managerCLIArgs
    }

    // Unknown parameter(s)
    // And validate parameters constraints (only one)
    if ([managerCLIArgs.listAction, managerCLIArgs.installAction?.trim(), managerCLIArgs.uninstallAction?.trim()].findAll {
      it
    }.size() != 1) {
      managerCLIArgs.action = ManagerCLIArgs.Action.HELP
      Logging.displayMsgError("Invalid command line parameter(s)")
      cli.usage()
      return null
    }

    if (managerCLIArgs.listAction) {
      managerCLIArgs.action = ManagerCLIArgs.Action.LIST
    } else if (managerCLIArgs.installAction?.trim()) {
      managerCLIArgs.action = ManagerCLIArgs.Action.INSTALL
      if (managerCLIArgs.installAction.indexOf(':') > 0) {
        // A specific version is asked
        managerCLIArgs.addonId = managerCLIArgs.installAction.substring(0, managerCLIArgs.installAction.indexOf(':'))
        managerCLIArgs.addonVersion = managerCLIArgs.installAction.substring(managerCLIArgs.installAction.indexOf(':') + 1,
                                                                             managerCLIArgs.installAction.length())
      } else {
        managerCLIArgs.addonId = managerCLIArgs.installAction
      }
      if (managerCLIArgs.force) {
        Logging.displayMsgVerbose("Force mode activated")
      }
    } else if (managerCLIArgs.uninstallAction?.trim()) {
      managerCLIArgs.action = ManagerCLIArgs.Action.UNINSTALL
      managerCLIArgs.addonId = managerCLIArgs.uninstallAction
    } else {
      Logging.displayMsgError("Invalid command line parameter(s)")
      cli.usage()
      return null
    }
    return managerCLIArgs
  }
}