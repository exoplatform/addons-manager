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
  /**
   * The enumeration of all possible commands
   */
  enum Command {
    LIST(LIST_COMMAND), DESCRIBE(DESCRIBE_COMMAND), INSTALL(INSTALL_COMMAND), UNINSTALL(UNINSTALL_COMMAND)
    final String name

    Command(String name) {
      this.name = name
    }
  }

  /**
   * The command name used to list add-ons
   */
  static final String LIST_COMMAND = "list"
  /**
   * The command name used to describe an add-on
   */
  static final String DESCRIBE_COMMAND = "describe"
  /**
   * The command name used to install an add-on
   */
  static final String INSTALL_COMMAND = "install"
  /**
   * The command name used to uninstall an add-on
   */
  static final String UNINSTALL_COMMAND = "uninstall"

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
   * The command asked by the user
   */
  Command command
  /**
   * To activate verbose logs
   */
  @Parameter(names = ["-v", "--verbose"], description = "Show verbose logs")
  private boolean _verbose
  /**
   * To display the help message
   */
  @Parameter(names = ["-h", "--help"], description = "Show usage information", help = true)
  private boolean _help
  /**
   * Run in non-interactive mode
   */
  @Parameter(names = ["-B", "--batch-mode"], description = "Run in non-interactive (batch) mode")
  private boolean _batchMode

  /**
   * Verifies if is the verbose option is activated as main parameter or command parameter
   * @return true if the verbose option is activated as main parameter or command parameter
   */
  boolean isVerbose() {
    return _verbose || commandList.verbose || commandDescribe.verbose || commandInstall.verbose || commandUninstall.verbose
  }

  /**
   * Verifies if is the help option is activated as main parameter or command parameter
   * @return true if the help option is activated as main parameter or command parameter
   */
  boolean isHelp() {
    return _help || commandList.help || commandDescribe.help || commandInstall.help || commandUninstall.help
  }

  /**
   * Verifies if is the batch option is activated as main parameter or command parameter
   * @return true if the batch option is activated as main parameter or command parameter
   */
  boolean isBatchMode() {
    return _batchMode || commandList.batchMode || commandDescribe.batchMode || commandInstall.batchMode || commandUninstall.batchMode
  }

  /**
   * Specific parameters to list add-ons
   */
  @Parameters(commandDescription = "List add-ons", commandNames = CommandLineParameters.LIST_COMMAND, separators = "=")
  class ListCommandParameters {
    @Parameter(names = ["--snapshots"], description = "List also add-ons SNAPSHOTs")
    boolean snapshots
    @Parameter(names = ["--unstable"], description = "List also unstable add-ons")
    boolean unstable
    @Parameter(names = ["--installed"], description = "List all add-ons installed locally")
    boolean installed
    @Parameter(names = ["--outdated"], description = "List all add-ons installed locally for which a newer version is available")
    boolean outdated
    @Parameter(names = ["--no-compat"], description = "Display also add-ons not marked as compatible with your platform instance")
    boolean noCompat
    @Parameter(names = ["--catalog"], description = "=<URL> - Central catalog URL", validateWith = URLValidator.class,
        converter = URLConverter.class)
    URL catalog
    @Parameter(names = ["--no-cache"], description = "Discard the remote catalog local cache")
    boolean noCache
    @Parameter(names = ["--offline"], description = "Do not download anything")
    boolean offline
    @Parameter(names = ["-v", "--verbose"], hidden = true)
    protected boolean verbose
    @Parameter(names = ["-h", "--help"], help = true, hidden = true)
    protected boolean help
    @Parameter(names = ["-B", "--batch-mode"], description = "Run in non-interactive (batch) mode", hidden = true)
    protected boolean batchMode
  }

  /**
   * Specific parameters to print informations about an add-on
   */
  @Parameters(commandDescription = "Describe an add-on", commandNames = CommandLineParameters.DESCRIBE_COMMAND, separators = "=")
  class DescribeCommandParameters {
    @Parameter(names = ["--catalog"], description = "=<URL> - Central catalog URL", validateWith = URLValidator.class,
        converter = URLConverter.class)
    URL catalog
    @Parameter(names = ["--no-cache"], description = "Discard the remote catalog local cache")
    boolean noCache
    @Parameter(names = ["--offline"], description = "Do not download anything")
    boolean offline
    @Parameter(description = "identifier[:version]", arity = 1, required = true)
    protected List<String> addon;
    @Parameter(names = ["-v", "--verbose"], hidden = true)
    protected boolean verbose
    @Parameter(names = ["-h", "--help"], help = true, hidden = true)
    protected boolean help
    @Parameter(names = ["-B", "--batch-mode"], description = "Run in non-interactive (batch) mode", hidden = true)
    protected boolean batchMode
    String addonId
    String addonVersion
  }

  /**
   * Specific parameters to install an add-on
   */
  @Parameters(commandDescription = "Install an add-on", commandNames = CommandLineParameters.INSTALL_COMMAND, separators = "=")
  class InstallCommandParameters {
    @Parameter(names = ["--force"], description = "Enforce to download again and reinstall an add-on already deployed")
    boolean force
    @Parameter(names = ["--snapshots"], description = "If no version specified, allows to find the latest one including development versions")
    boolean snapshots
    @Parameter(names = ["--unstable"], description = "If no version specified, allows to find the latest one including unstable versions")
    boolean unstable
    @Parameter(names = ["--no-compat"], description = "Disable compatibility check")
    boolean noCompat
    @Parameter(names = ["--conflict"],
        description = "=[skip|overwrite] - to control the behavior when installing a file already existing in your PLF instance. Failing by default",
        validateWith = ConflictValidator.class,
        converter = ConflictConverter.class)
    Conflict conflict = Conflict.FAIL
    @Parameter(names = ["--catalog"], description = "=<URL> - Central catalog URL", validateWith = URLValidator.class,
        converter = URLConverter.class)
    URL catalog
    @Parameter(names = ["--no-cache"], description = "Discard the remote catalog local cache")
    boolean noCache
    @Parameter(names = ["--offline"], description = "Do not download anything")
    boolean offline
    @Parameter(description = "identifier[:version]", arity = 1, required = true)
    protected List<String> addon;
    @Parameter(names = ["-v", "--verbose"], hidden = true)
    protected boolean verbose
    @Parameter(names = ["-h", "--help"], help = true, hidden = true)
    protected boolean help
    @Parameter(names = ["-B", "--batch-mode"], description = "Run in non-interactive (batch) mode", hidden = true)
    protected boolean batchMode
    String addonId
    String addonVersion
  }

  /**
   * Specific parameters to uninstall an add-on
   */
  @Parameters(commandDescription = "Uninstall an add-on", commandNames = CommandLineParameters.UNINSTALL_COMMAND, separators = "=")
  class UninstallCommandParameters {
    @Parameter(description = "identifier", arity = 1, required = true)
    protected List<String> addon;
    @Parameter(names = ["-v", "--verbose"], hidden = true)
    protected boolean verbose
    @Parameter(names = ["-h", "--help"], help = true, hidden = true)
    protected boolean help
    @Parameter(names = ["-B", "--batch-mode"], description = "Run in non-interactive (batch) mode", hidden = true)
    protected boolean batchMode
    String addonId
  }

}