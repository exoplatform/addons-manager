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
import com.beust.jcommander.ParameterDescription
import com.beust.jcommander.ParameterException
import org.exoplatform.platform.am.ex.CommandLineParsingException
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

  private String _scriptName

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
    _scriptName = scriptName
  }

  /**
   * Display in the output the usage message to explain how to use the program and its parameters
   */
  void usage() {
    LOG.info("@|underline Usage:|@")
    LOG.info("")
    LOG.info("  ${_scriptName} [options] @|yellow command|@ @|bold [command parameter(s)]|@ [command options]")
    LOG.info("")
    LOG.info("@|underline Options:|@")
    LOG.info("")
    _jCommander.parameters.findAll { !it.parameter.hidden() }.sort { it.names }.each { globalParam ->
      LOG.info(
          String.format("  @|yellow %-${_jCommander.parameters.collect { computeOptionSyntax(it) }*.size().max()}s|@ : %s %s",
                        computeOptionSyntax(globalParam),
                        globalParam.description,
                        computeDefaultValue(globalParam) ? "(Default: ${computeDefaultValue(globalParam)})" : ""))
    }
    LOG.info("")
    LOG.info("@|underline Commands:|@")
    _jCommander.commands.each { command, commandDescription ->
      LOG.info("")
      List<String> commandHelp = new ArrayList<>()
      commandHelp << _scriptName
      commandHelp << "[options]"
      commandHelp << "@|yellow ${command}|@"
      if (commandDescription.mainParameter) {
        commandHelp << "@|bold ${commandDescription.mainParameter?.description}|@"
      }
      if (_jCommander.commands[command].parameters.findAll { !it.parameter.hidden() }.size() > 0) {
        commandHelp << "[${command} options]"
      }
      LOG.info("  ${commandHelp.join(" ")}")
      commandDescription.parameters.findAll { !it.parameter.hidden() }.sort { it.names }.each { commandParam ->
        LOG.info(
            String.format(
                "    @|yellow %-${commandDescription.parameters.findAll { !it.parameter.hidden() }.collect { computeOptionSyntax(it) }*.trim()*.size().max()}s|@ : %s %s",
                computeOptionSyntax(commandParam),
                commandParam.description,
                computeDefaultValue(commandParam) ? "(Default: ${computeDefaultValue(commandParam)})" : ""))
      }
    }
    LOG.info("")
  }

  private String computeOptionSyntax(ParameterDescription parameterDescription) {
    parameterDescription.parameter.names().collect { paramName ->
      if (parameterDescription.parameterized.type == Boolean) {
        "${paramName}"
      } else if (parameterDescription.parameterized.type == URL) {
        "${paramName}=URL"
      } else if (parameterDescription.parameterized.type.isEnum()) {
        "${paramName}=[${parameterDescription.parameterized.type.enumConstants.collect { it.toString().toLowerCase() }.join("|")}]"
      } else {
        "${paramName}=value"
      }
    }.join(", ")
  }

  private String computeDefaultValue(ParameterDescription parameterDescription) {
    if (parameterDescription.parameterized.type == Boolean && !parameterDescription.default) {
      "false"
    } else {
      parameterDescription.default ? parameterDescription.default?.toString()?.toLowerCase() : ""
    }
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
      throw new CommandLineParsingException("Invalid command line parameter(s) : ${pe.message}", pe);
    }
    if (_cliArgs.verbose) {
      LOG.enableDebug()
    }

    // Show usage text when -h or --help option is used.
    if (_cliArgs.help) {
      return _cliArgs
    }

    if (CommandLineParameters.LIST_CMD.equals(_jCommander.getParsedCommand())) {
      _cliArgs.command = CommandLineParameters.Command.LIST
    } else if (CommandLineParameters.DESCRIBE_CMD.equals(_jCommander.getParsedCommand())) {
      _cliArgs.command = CommandLineParameters.Command.DESCRIBE
      if (_cliArgs?.commandDescribe?.addon?.size() != 1) {
        throw new CommandLineParsingException(
            "Invalid command line parameter(s) : Command ${CommandLineParameters.Command.DESCRIBE} must have one and only one value (found : ${_cliArgs?.commandDescribe?.addon})");
      }
      if (_cliArgs.commandDescribe.addon[0].indexOf(':') > 0) {
        // A specific version is asked
        _cliArgs.commandDescribe.addonId = _cliArgs.commandDescribe.addon[0].substring(0,
                                                                                       _cliArgs.commandDescribe.addon[0].indexOf(
                                                                                           ':'))
        _cliArgs.commandDescribe.addonVersion = _cliArgs.commandDescribe.addon[0].substring(
            _cliArgs.commandDescribe.addon[0].indexOf(':') + 1,
            _cliArgs.commandDescribe.addon[0].length())
      } else {
        _cliArgs.commandDescribe.addonId = _cliArgs.commandDescribe.addon[0]
      }
    } else if (CommandLineParameters.INSTALL_CMD.equals(_jCommander.getParsedCommand())) {
      _cliArgs.command = CommandLineParameters.Command.INSTALL
      if (_cliArgs?.commandInstall?.addon?.size() != 1) {
        throw new CommandLineParsingException(
            "Invalid command line parameter(s) : Command ${CommandLineParameters.Command.INSTALL} must have one and only one value (found : ${_cliArgs?.commandInstall?.addon})");
      }
      if (_cliArgs.commandInstall.addon[0].indexOf(':') > 0) {
        // A specific version is asked
        _cliArgs.commandInstall.addonId = _cliArgs.commandInstall.addon[0].substring(0, _cliArgs.commandInstall.addon[0].indexOf(
            ':'))
        _cliArgs.commandInstall.addonVersion = _cliArgs.commandInstall.addon[0].substring(
            _cliArgs.commandInstall.addon[0].indexOf(':') + 1,
            _cliArgs.commandInstall.addon[0].length())
      } else {
        _cliArgs.commandInstall.addonId = _cliArgs.commandInstall.addon[0]
      }
      if (_cliArgs.commandInstall.force) {
        LOG.debug("Force mode activated")
      }
    } else if (CommandLineParameters.UNINSTALL_CMD.equals(_jCommander.getParsedCommand())) {
      _cliArgs.command = CommandLineParameters.Command.UNINSTALL
      if (_cliArgs?.commandUninstall?.addon?.size() != 1) {
        throw new CommandLineParsingException(
            "Invalid command line parameter(s) : Command ${CommandLineParameters.Command.UNINSTALL} must have one and only one value (found : ${_cliArgs?.commandUninstall?.addon})");
      }
      if (_cliArgs.commandUninstall.addon[0].indexOf(':') > 0) {
        // A specific version is asked
        _cliArgs.commandUninstall.addonId = _cliArgs.commandUninstall.addon[0].substring(0, _cliArgs.commandUninstall.addon
        [0].indexOf(':'))
        // Version is useless. Let's warn
        String addonVersion = _cliArgs.commandUninstall.addon[0].substring(
            _cliArgs.commandUninstall.addon[0].indexOf(':') + 1,
            _cliArgs.commandUninstall.addon[0].length())
        LOG.warn(
            "Command line parameter(s) : The add-on version (${addonVersion}) is useless for command ${CommandLineParameters.Command.UNINSTALL} and won't be used")
      } else {
        _cliArgs.commandUninstall.addonId = _cliArgs.commandUninstall.addon[0]
      }
    } else {
      throw new CommandLineParsingException("Invalid command line parameter(s) : No command defined")
    }
    return _cliArgs
  }

}