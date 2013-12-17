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
public class CLI {

  static String getScriptName() {
    def scriptBaseName = "addon"
    // Computes the script addon from the OS
    def scriptName = "${scriptBaseName}.sh"
    if (System.properties['os.name'].toLowerCase().contains('windows')) {
      scriptName = "${scriptBaseName}.bat"
    }
    return scriptName
  }

  static boolean initialize(String[] args) {

    def cli = new CliBuilder(
        posix: false,
        stopAtNonOption: true,
        width: Logging.CONSOLE_WIDTH,
        usage: ansi().render("""
    ${CLI.getScriptName()} --list [-v] [-a]
    ${CLI.getScriptName()} --install <@|yellow add-on|@> [-v] [-f]
    ${CLI.getScriptName()} --uninstall <@|yellow add-on|@> [-v]
    """).toString(),
        header: "options :")

    // Create the list of options.
    cli.with {
      h longOpt: 'help', 'Show usage information'
      l longOpt: 'list', 'List all available add-ons'
      i longOpt: 'install', args: 1, argName: 'add-on', 'Install an add-on'
      u longOpt: 'uninstall', args: 1, argName: 'add-on', 'Uninstall an add-on'
      f longOpt: 'force', 'Enforce to download remote add-ons even if they are already available locally'
      v longOpt: 'verbose', 'Show verbose logs'
      s longOpt: 'snapshots', 'List also add-ons SNAPSHOTs'
    }

    def options = cli.parse(args)

    // Erroneous command line
    if (!options) {
      Logging.displayMsgError("Invalid command line parameter(s)")
      cli.usage()
      return false
    }

    if (options.v) {
      Settings.instance.verbose = true
      Logging.displayMsgVerbose("Verbose logs activated")
    }

    // Show usage text when -h or --help option is used.
    if (args.length == 0 || options.h) {
      Settings.instance.action = Settings.Action.HELP
      cli.usage()
      return true
    }

    // Unknown parameter(s)
    // And validate parameters constraints (only one)
    if (options.arguments() || [options.l, options.i, options.u].findAll { it }.size() != 1) {
      Logging.displayMsgError("Invalid command line parameter(s)")
      cli.usage()
      return false
    }

    if (options.l) {
      Settings.instance.action = Settings.Action.LIST
      if(options.s){
        Settings.instance.snapshots = true
      }
    } else if (options.i) {
      Settings.instance.action = Settings.Action.INSTALL
      Settings.instance.addonId = options.i
      if (options.f) {
        Settings.instance.force = true
        Logging.displayMsgVerbose("Force mode activated")
      }
      if(options.s){
        Settings.instance.snapshots = true
      }
    } else if (options.u) {
      Settings.instance.action = Settings.Action.UNINSTALL
      Settings.instance.addonId = options.u
    } else {
      Logging.displayMsgError("Invalid command line parameter(s)")
      cli.usage()
      return false
    }
    return true
  }
}