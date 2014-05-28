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

import static org.fusesource.jansi.Ansi.ansi

/**
 * Copyright (C) 2003-2013 eXo Platform SAS.
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
  static ManagerSettings initialize(String[] args, ManagerSettings managerSettings) {

    def cli = new CliBuilder(
        posix: false,
        stopAtNonOption: true,
        width: Logging.CONSOLE_WIDTH,
        usage: ansi().render("""
    ${CLI.getScriptName()} --list [-v] [-s]
    ${CLI.getScriptName()} --install @|yellow addon[:version]|@ [-v] [-s] [-f]
    ${CLI.getScriptName()} --uninstall @|yellow addon|@ [-v]
    """).toString(),
        header: "options :")

    // Create the list of options.
    cli.with {
      h longOpt: 'help', 'Show usage information'
      l longOpt: 'list', 'List all available add-ons'
      i longOpt: 'install', args: 1, argName: 'addon', 'Install an add-on'
      u longOpt: 'uninstall', args: 1, argName: 'addon', 'Uninstall an add-on'
      f longOpt: 'force', 'Enforce to download again and reinstall an add-on already deployed'
      v longOpt: 'verbose', 'Show verbose logs'
      s longOpt: 'snapshots', 'List also add-ons SNAPSHOTs'
    }

    def options = cli.parse(args)

    // Erroneous command line
    if (!options) {
      Logging.displayMsgError("Invalid command line parameter(s)")
      cli.usage()
      return null
    }

    if (options.v) {
      managerSettings.verbose = true
      Logging.verbose = true
      Logging.displayMsgVerbose("Verbose logs activated")
    }

    // Show usage text when -h or --help option is used.
    if (args.length == 0 || options.h) {
      managerSettings.action = ManagerSettings.Action.HELP
      cli.usage()
      return managerSettings
    }

    // Unknown parameter(s)
    // And validate parameters constraints (only one)
    if (options.arguments() || [options.l, options.i, options.u].findAll { it }.size() != 1) {
      Logging.displayMsgError("Invalid command line parameter(s)")
      cli.usage()
      return null
    }

    if (options.l) {
      managerSettings.action = ManagerSettings.Action.LIST
      if (options.s) {
        managerSettings.snapshots = true
      }
    } else if (options.i) {
      managerSettings.action = ManagerSettings.Action.INSTALL
      if (options.i.indexOf(':') > 0) {
        // A specific version is asked
        managerSettings.addonId = options.i.substring(0, options.i.indexOf(':'))
        managerSettings.addonVersion = options.i.substring(options.i.indexOf(':') + 1, options.i.length())
      } else {
        managerSettings.addonId = options.i
      }
      if (options.f) {
        managerSettings.force = true
        Logging.displayMsgVerbose("Force mode activated")
      }
      if (options.s) {
        managerSettings.snapshots = true
      }
    } else if (options.u) {
      managerSettings.action = ManagerSettings.Action.UNINSTALL
      managerSettings.addonId = options.u
    } else {
      Logging.displayMsgError("Invalid command line parameter(s)")
      cli.usage()
      return null
    }
    return managerSettings
  }
}