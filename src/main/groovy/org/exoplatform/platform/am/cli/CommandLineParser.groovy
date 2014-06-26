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
package org.exoplatform.platform.am.cli

import com.beust.jcommander.JCommander
import com.beust.jcommander.ParameterException
import org.exoplatform.platform.am.utils.Logger

/**
 * Parser for command line arguments
 * @author Arnaud Héritier <aheritier@exoplatform.com>
 */
class CommandLineParser {

  /**
   * Logger
   */
  private static final Logger LOG = Logger.getInstance()

  /**
   * JCommander instance used to process args
   */
  private JCommander _jCommander

  /**
   * Object to populate from CL args
   */
  private CommandLineParameters _cliArgs

  /**
   * Default Constructor
   * @param scriptName The name of the script (used to display usage message)
   * @param columnSize The number of characters to in an output line  (used to display usage message)
   */
  CommandLineParser(String scriptName, int columnSize) {
    // Output
    _cliArgs = new CommandLineParameters()
    _jCommander = new JCommander(_cliArgs);
    _jCommander.addCommand(_cliArgs.commandList)
    _jCommander.addCommand(_cliArgs.commandDescribe)
    _jCommander.addCommand(_cliArgs.commandInstall)
    _jCommander.addCommand(_cliArgs.commandUninstall)
    _jCommander.setColumnSize(columnSize)
    _jCommander.setProgramName(scriptName)
  }

  /**
   * Display in the output the usage message to explain how to use the program and its parameters
   */
  void usage() {
    _jCommander.usage();
  }

  /**
   * Initialize settings from command line parameters
   * @param args Command line parameters to analyze
   * @return a CommandLineParameters instance populated with data coming from the command line
   * @thows CommandLineParsingException if something goes wrong while analyzing CL parameters
   */
  CommandLineParameters parse(String[] args) {
    LOG.debug("Parameters to parse : ${args}")
    try {
      _jCommander.parse(args);
    } catch (ParameterException pe) {
      throw new CommandLineParsingException(pe.message, pe);
    }
    if (_cliArgs.verbose) {
      LOG.enableDebug()
    }

    // Show usage text when -h or --help option is used.
    if (_cliArgs.help) {
      return _cliArgs
    }

    if (CommandLineParameters.LIST_COMMAND.equals(_jCommander.getParsedCommand())) {
      _cliArgs.command = CommandLineParameters.Command.LIST
    } else if (CommandLineParameters.DESCRIBE_COMMAND.equals(_jCommander.getParsedCommand())) {
      _cliArgs.command = CommandLineParameters.Command.DESCRIBE
      if (_cliArgs?.commandDescribe?.addon?.size() != 1) {
        throw new CommandLineParsingException(
            "Command ${CommandLineParameters.Command.DESCRIBE} must have one and only one value (found : ${_cliArgs?.commandDescribe?.addon})");
      }
      if (_cliArgs.commandDescribe.addon[0].indexOf(':') > 0) {
        // A specific version is asked
        _cliArgs.commandDescribe.addonId = _cliArgs.commandDescribe.addon[0].substring(0, _cliArgs.commandDescribe.addon[0].indexOf
            (':'))
        _cliArgs.commandDescribe.addonVersion = _cliArgs.commandDescribe.addon[0].substring(
            _cliArgs.commandDescribe.addon[0].indexOf(':') + 1,
            _cliArgs.commandDescribe.addon[0].length())
      } else {
        _cliArgs.commandDescribe.addonId = _cliArgs.commandDescribe.addon[0]
      }
    } else if (CommandLineParameters.INSTALL_COMMAND.equals(_jCommander.getParsedCommand())) {
      _cliArgs.command = CommandLineParameters.Command.INSTALL
      if (_cliArgs?.commandInstall?.addon?.size() != 1) {
        throw new CommandLineParsingException(
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
        LOG.debug("Force mode activated")
      }
    } else if (CommandLineParameters.UNINSTALL_COMMAND.equals(_jCommander.getParsedCommand())) {
      _cliArgs.command = CommandLineParameters.Command.UNINSTALL
      if (_cliArgs?.commandUninstall?.addon?.size() != 1) {
        throw new CommandLineParsingException(
            "Command ${CommandLineParameters.Command.UNINSTALL} must have one and only one value (found : ${_cliArgs?.commandUninstall?.addon})");
      }
      _cliArgs.commandUninstall.addonId = _cliArgs.commandUninstall.addon[0]
    } else {
      throw new CommandLineParsingException("No command defined")
    }
    return _cliArgs
  }

}