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

import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters

/**
 * Command line parameters
 * @author Arnaud Héritier <aheritier@exoplatform.com>
 */
class CommandLineParameters {

  // CLI Options
  static final String VERBOSE_SHORT_OPT = "-v"
  static final String VERBOSE_LONG_OPT = "--verbose"
  static final String HELP_SHORT_OPT = "-h"
  static final String HELP_LONG_OPT = "--help"
  static final String BATCH_SHORT_OPT = "-B"
  static final String BATCH_LONG_OPT = "--batch-mode"
  static final String SNAPSHOTS_LONG_OPT = "--snapshots"
  static final String UNSTABLE_LONG_OPT = "--unstable"
  static final String INSTALLED_LONG_OPT = "--installed"
  static final String OUTDATED_LONG_OPT = "--outdated"
  static final String NO_COMPAT_LONG_OPT = "--no-compat"
  static final String CATALOG_LONG_OPT = "--catalog"
  static final String NO_CACHE_LONG_OPT = "--no-cache"
  static final String OFFLINE_LONG_OPT = "--offline"
  static final String CONFLICT_LONG_OPT = "--conflict"
  static final String FORCE_LONG_OPT = "--force"
  static final String CRCCHECK_OPT = "--crc"

  // CLI Commands
  static final String LIST_CMD = "list"
  static final String DESCRIBE_CMD = "describe"
  static final String INSTALL_CMD = "install"
  static final String UNINSTALL_CMD = "uninstall"
  static final String APPLY_CMD = "apply"

  /**
   * The enumeration of all possible commands
   */
  enum Command {
    LIST(LIST_CMD), DESCRIBE(DESCRIBE_CMD), INSTALL(INSTALL_CMD), UNINSTALL(UNINSTALL_CMD), APPLY(APPLY_CMD)
    final String name

    Command(String name) {
      this.name = name
    }
  }

  /**
   * List command parameters
   */
  ListCommandParameters commandList = new ListCommandParameters()
  /**
   * Describe command parameters
   */
  DescribeCommandParameters commandDescribe = new DescribeCommandParameters()
  /**
   * Install command parameters
   */
  InstallCommandParameters commandInstall = new InstallCommandParameters()
  /**
   * Uninstall command parameters
   */
  UninstallCommandParameters commandUninstall = new UninstallCommandParameters()
  /**
   * Offline install command parameters
   */
  ApplyCommandParameters commandApply = new ApplyCommandParameters()
  /**
   * The command asked by the user
   */
  Command command
  /**
   * To activate verbose logs
   */
  @Parameter(names = [CommandLineParameters.VERBOSE_SHORT_OPT, CommandLineParameters.VERBOSE_LONG_OPT], description = "Show verbose logs")
  private Boolean _verbose
  /**
   * To display the help message
   */
  @Parameter(names = [CommandLineParameters.HELP_SHORT_OPT, CommandLineParameters.HELP_LONG_OPT], description = "Show usage information", help = true)
  private Boolean _help
  /**
   * Run in non-interactive mode
   */
  @Parameter(names = [CommandLineParameters.BATCH_SHORT_OPT, CommandLineParameters.BATCH_LONG_OPT], description = "Run in non-interactive (batch) mode")
  private Boolean _batchMode

  /**
   * Verifies if is the verbose option is activated as main parameter or command parameter
   * @return true if the verbose option is activated as main parameter or command parameter
   */
  Boolean isVerbose() {
    return _verbose || commandList.verbose || commandDescribe.verbose || commandInstall.verbose || commandUninstall.verbose || commandApply.verbose
  }

  /**
   * Verifies if is the help option is activated as main parameter or command parameter
   * @return true if the help option is activated as main parameter or command parameter
   */
  Boolean isHelp() {
    return _help || commandList.help || commandDescribe.help || commandInstall.help || commandUninstall.help || commandApply.help
  }

  /**
   * Verifies if is the batch option is activated as main parameter or command parameter
   * @return true if the batch option is activated as main parameter or command parameter
   */
  Boolean isBatchMode() {
    return _batchMode || commandList.batchMode || commandDescribe.batchMode || commandInstall.batchMode || commandUninstall.batchMode || commandApply.batchMode
  }

  /**
   * Specific parameters to list add-ons
   */
  @Parameters(commandDescription = "List add-ons", commandNames = CommandLineParameters.LIST_CMD, separators = "=")
  class ListCommandParameters {
    @Parameter(names = [CommandLineParameters.SNAPSHOTS_LONG_OPT], description = "List also development versions of add-ons")
    Boolean snapshots
    @Parameter(names = [CommandLineParameters.UNSTABLE_LONG_OPT], description = "List also unstable add-ons")
    Boolean unstable
    @Parameter(names = [CommandLineParameters.INSTALLED_LONG_OPT], description = "List all add-ons installed locally")
    Boolean installed
    @Parameter(names = [CommandLineParameters.OUTDATED_LONG_OPT], description = "List all add-ons installed locally for which a newer version is available")
    Boolean outdated
    @Parameter(names = [CommandLineParameters.NO_COMPAT_LONG_OPT], description = "Display also add-ons not marked as compatible with your platform instance")
    Boolean noCompat
    @Parameter(names = [CommandLineParameters.CATALOG_LONG_OPT], description = "Central catalog URL",
        validateWith = URLValidator.class,
        converter = URLConverter.class)
    URL catalog
    @Parameter(names = [CommandLineParameters.NO_CACHE_LONG_OPT], description = "Discard the remote catalog local cache")
    Boolean noCache
    @Parameter(names = [CommandLineParameters.OFFLINE_LONG_OPT], description = "Disable download of catalog")
    Boolean offline
    @Parameter(names = [CommandLineParameters.VERBOSE_SHORT_OPT, CommandLineParameters.VERBOSE_LONG_OPT], hidden = true)
    protected Boolean verbose
    @Parameter(names = [CommandLineParameters.HELP_SHORT_OPT, CommandLineParameters.HELP_LONG_OPT], help = true, hidden = true)
    protected Boolean help
    @Parameter(names = [CommandLineParameters.BATCH_SHORT_OPT, CommandLineParameters.BATCH_LONG_OPT], description = "Run in non-interactive (batch) mode", hidden = true)
    protected Boolean batchMode
  }

  /**
   * Specific parameters to print information about an add-on
   */
  @Parameters(commandDescription = "Describe an add-on", commandNames = CommandLineParameters.DESCRIBE_CMD, separators = "=")
  class DescribeCommandParameters {
    @Parameter(names = [CommandLineParameters.CATALOG_LONG_OPT], description = "Central catalog URL", validateWith = URLValidator.class,
        converter = URLConverter.class)
    URL catalog
    @Parameter(names = [CommandLineParameters.NO_CACHE_LONG_OPT], description = "Discard the remote catalog local cache")
    Boolean noCache
    @Parameter(names = [CommandLineParameters.OFFLINE_LONG_OPT], description = "Disable download of catalog")
    Boolean offline
    @Parameter(description = "addonIdentifier[:addonVersion]", arity = 1, required = true)
    protected List<String> addon;
    @Parameter(names = [CommandLineParameters.VERBOSE_SHORT_OPT, CommandLineParameters.VERBOSE_LONG_OPT], hidden = true)
    protected Boolean verbose
    @Parameter(names = [CommandLineParameters.HELP_SHORT_OPT, CommandLineParameters.HELP_LONG_OPT], help = true, hidden = true)
    protected Boolean help
    @Parameter(names = [CommandLineParameters.BATCH_SHORT_OPT, CommandLineParameters.BATCH_LONG_OPT], description = "Run in non-interactive (batch) mode", hidden = true)
    protected Boolean batchMode
    String addonId
    String addonVersion
  }

  /**
   * Specific parameters to install an add-on
   */
  @Parameters(commandDescription = "Install an add-on", commandNames = CommandLineParameters.INSTALL_CMD, separators = "=")
  class InstallCommandParameters {
    @Parameter(names = [CommandLineParameters.FORCE_LONG_OPT], description = "Enforce to download again and reinstall an add-on already deployed")
    Boolean force
    @Parameter(names = [CommandLineParameters.SNAPSHOTS_LONG_OPT], description = "If no addonVersion specified, allows to find the latest one including development versions")
    Boolean snapshots
    @Parameter(names = [CommandLineParameters.UNSTABLE_LONG_OPT], description = "If no addonVersion specified, allows to find the latest one including unstable versions")
    Boolean unstable
    @Parameter(names = [CommandLineParameters.NO_COMPAT_LONG_OPT], description = "Install the add-on even if not marked as compatible with your platform instance")
    Boolean noCompat
    @Parameter(names = [CommandLineParameters.CONFLICT_LONG_OPT],
        description = "Behavior when installing a file already existing in your PLF instance",
        validateWith = ConflictValidator.class,
        converter = ConflictConverter.class)
    Conflict conflict = Conflict.FAIL
    @Parameter(names = [CommandLineParameters.CATALOG_LONG_OPT], description = "Central catalog URL", validateWith = URLValidator.class,
        converter = URLConverter.class)
    URL catalog
    @Parameter(names = [CommandLineParameters.NO_CACHE_LONG_OPT], description = "Discard the remote catalog local cache")
    Boolean noCache
    @Parameter(names = [CommandLineParameters.OFFLINE_LONG_OPT], description = "Disable downloads of catalog and add-on archive")
    Boolean offline
    @Parameter(description = "addonIdentifier[:addonVersion]", arity = 1, required = true)
    protected List<String> addon;
    @Parameter(names = [CommandLineParameters.VERBOSE_SHORT_OPT, CommandLineParameters.VERBOSE_LONG_OPT], hidden = true)
    protected Boolean verbose
    @Parameter(names = [CommandLineParameters.HELP_SHORT_OPT, CommandLineParameters.HELP_LONG_OPT], help = true, hidden = true)
    protected Boolean help
    @Parameter(names = [CommandLineParameters.BATCH_SHORT_OPT, CommandLineParameters.BATCH_LONG_OPT], description = "Run in non-interactive (batch) mode", hidden = true)
    protected Boolean batchMode
    String addonId
    String addonVersion
  }

  /**
   * Specific parameters to uninstall an add-on
   */
  @Parameters(commandDescription = "Uninstall an add-on", commandNames = CommandLineParameters.UNINSTALL_CMD, separators = "=")
  class UninstallCommandParameters {
    @Parameter(description = "addonIdentifier", arity = 1, required = true)
    protected List<String> addon;
    @Parameter(names = [CommandLineParameters.VERBOSE_SHORT_OPT, CommandLineParameters.VERBOSE_LONG_OPT], hidden = true)
    protected Boolean verbose
    @Parameter(names = [CommandLineParameters.HELP_SHORT_OPT, CommandLineParameters.HELP_LONG_OPT], help = true, hidden = true)
    protected Boolean help
    @Parameter(names = [CommandLineParameters.BATCH_SHORT_OPT, CommandLineParameters.BATCH_LONG_OPT], description = "Run in non-interactive (batch) mode", hidden = true)
    protected Boolean batchMode
    String addonId
  }

  /**
   * Specific parameters to apply a patch
   */
  @Parameters(commandDescription = "Apply an offline patch", commandNames = CommandLineParameters.APPLY_CMD, separators = "=")
  class ApplyCommandParameters {
    @Parameter(description = "patchFile keyFile", arity = 2, required = true)
    protected List<String> addon;
    @Parameter(names = [CommandLineParameters.VERBOSE_SHORT_OPT, CommandLineParameters.VERBOSE_LONG_OPT], hidden = true)
    protected Boolean verbose
    @Parameter(names = [CommandLineParameters.HELP_SHORT_OPT, CommandLineParameters.HELP_LONG_OPT], help = true, hidden = true)
    protected Boolean help
    @Parameter(names = [CommandLineParameters.BATCH_SHORT_OPT, CommandLineParameters.BATCH_LONG_OPT], description = "Run in non-interactive (batch) mode", hidden = true)
    protected Boolean batchMode
    String patchFile
    String keyFile
    @Parameter(names = [CommandLineParameters.FORCE_LONG_OPT], description = "Enforce to reinstall a patch already deployed")
    Boolean force
    @Parameter(names = [CommandLineParameters.NO_COMPAT_LONG_OPT], description = "Install the patch even if not marked as compatible with your platform instance")
    Boolean noCompat
    @Parameter(names = [CommandLineParameters.CRCCHECK_OPT], description = "Enable the CRC Verification of the patch")
    long crcCheck
    @Parameter(names = [CommandLineParameters.CONFLICT_LONG_OPT],
            description = "Behavior when installing a file already existing in your PLF instance",
            validateWith = ConflictValidator.class,
            converter = ConflictConverter.class)
    Conflict conflict = Conflict.FAIL
  }

}