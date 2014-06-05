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
package org.exoplatform.platform.am.cli

import com.beust.jcommander.JCommander
import com.beust.jcommander.ParameterException
import org.exoplatform.platform.am.utils.Logging

class CommandLineParser {

  final static int RETURN_CODE_OK = 0
  final static int RETURN_CODE_KO = 1

  private JCommander _jCommander
  private CommandLineParameters _cliArgs

  CommandLineParser(String scriptName) {
    _cliArgs = new CommandLineParameters()
    _jCommander = new JCommander(_cliArgs);
    _jCommander.addCommand(_cliArgs.commandList)
    _jCommander.addCommand(_cliArgs.commandInstall)
    _jCommander.addCommand(_cliArgs.commandUninstall)
    _jCommander.setColumnSize(Logging.CONSOLE_WIDTH)
    _jCommander.setProgramName(scriptName)
  }

  void usage() {
    _jCommander.usage();
  }

  /**
   * Initialize settings from command line parameters
   * @param args Command line parameters
   * @return a EnvironmentSettings instance or null if something went wrong
   */
  CommandLineParameters parse(String[] args) {
    Logging.displayMsgVerbose("Parameters to parse : ${args}")
    _jCommander.parse(args);

    if (_cliArgs.verbose) {
      Logging.activateVerboseMessages()
    }

    // Show usage text when -h or --help option is used.
    if (_cliArgs.help) {
      return _cliArgs
    }

    if (CommandLineParameters.LIST_COMMAND.equals(_jCommander.getParsedCommand())) {
      _cliArgs.command = CommandLineParameters.Command.LIST
    } else if (CommandLineParameters.INSTALL_COMMAND.equals(_jCommander.getParsedCommand())) {
      _cliArgs.command = CommandLineParameters.Command.INSTALL
      if (_cliArgs?.commandInstall?.addon?.size() != 1) {
        throw new ParameterException(
            "Command ${CommandLineParameters.Command.INSTALL} must have one and only one value (found : ${_cliArgs?.commandInstall?.addon})");
      }
      if (_cliArgs.commandInstall.addon[0].indexOf(':') > 0) {
        // A specific version is asked
        _cliArgs.commandInstall.addonId = _cliArgs.commandInstall.addon[0].substring(0, _cliArgs.commandInstall.addon[0].indexOf
            (':'))
        _cliArgs.commandInstall.addonVersion = _cliArgs.commandInstall.addon[0].substring(
            _cliArgs.commandInstall.addon[0].indexOf(':') + 1,
            _cliArgs.commandInstall.addon[0].length())
      } else {
        _cliArgs.commandInstall.addonId = _cliArgs.commandInstall.addon[0]
      }
      if (_cliArgs.commandInstall.force) {
        Logging.displayMsgVerbose("Force mode activated")
      }
    } else if (CommandLineParameters.UNINSTALL_COMMAND.equals(_jCommander.getParsedCommand())) {
      _cliArgs.command = CommandLineParameters.Command.UNINSTALL
      if (_cliArgs?.commandUninstall?.addon?.size() != 1) {
        throw new ParameterException(
            "Command ${CommandLineParameters.Command.UNINSTALL} must have one and only one value (found : ${_cliArgs?.commandUninstall?.addon})");
      }
      _cliArgs.commandUninstall.addonId = _cliArgs.commandUninstall.addon[0]
    } else {
      throw new ParameterException("No command defined")
    }
    return _cliArgs
  }

}