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

import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters

/**
 * CommandLineParser Arguments passed to the manager
 * @author Arnaud Héritier <aheritier@exoplatform.com>
 */
class CommandLineParameters {
  public static final String LIST_COMMAND = "list"
  public static final String INSTALL_COMMAND = "install"
  public static final String UNINSTALL_COMMAND = "uninstall"

  def ListCommandParameters commandList = new ListCommandParameters()
  def InstallCommandParameters commandInstall = new InstallCommandParameters()
  def UninstallCommandParameters commandUninstall = new UninstallCommandParameters()
  def Command command

  @Parameter(names = ["-v", "--verbose"], description = "Show verbose logs")
  private def boolean verbose
  @Parameter(names = ["-h", "--help"], description = "Show usage information", help = true)
  private def boolean help

  /**
   * Verify if is the verbose option is activated as main parameter or command parameter
   * @return true if the verbose option is activated as main parameter or command parameter
   */
  public boolean isVerbose() {
    return verbose || commandList.verbose || commandInstall.verbose || commandUninstall.verbose
  }

  /**
   * Verify if is the help option is activated as main parameter or command parameter
   * @return true if the help option is activated as main parameter or command parameter
   */
  public boolean isHelp() {
    return help || commandList.help || commandInstall.help || commandUninstall.help
  }

  enum Command {
    LIST(LIST_COMMAND), INSTALL(INSTALL_COMMAND), UNINSTALL(UNINSTALL_COMMAND)
    final String name

    Command(String name) {
      this.name = name
    }
  }

  @Parameters(commandDescription = "List add-ons", commandNames = CommandLineParameters.LIST_COMMAND)
  class ListCommandParameters {
    @Parameter(names = ["-s", "--snapshots"], description = "List also add-ons SNAPSHOTs")
    def boolean snapshots
    @Parameter(names = ["-v", "--verbose"], hidden = true)
    def boolean verbose
    @Parameter(names = ["-h", "--help"], help = true, hidden = true)
    def boolean help
  }

  @Parameters(commandDescription = "Install an add-on", commandNames = CommandLineParameters.INSTALL_COMMAND)
  class InstallCommandParameters {
    @Parameter(names = ["-f", "--force"], description = "Enforce to download again and reinstall an add-on already deployed")
    def boolean force
    @Parameter(names = ["-s", "--snapshots"], description = "List also add-ons SNAPSHOTs")
    def boolean snapshots
    @Parameter(description = "addon[:version]", arity = 1, required = true)
    def List<String> addon;
    @Parameter(names = ["-v", "--verbose"], hidden = true)
    def boolean verbose
    @Parameter(names = ["-h", "--help"], help = true, hidden = true)
    def boolean help
    def String addonId
    def String addonVersion
  }

  @Parameters(commandDescription = "Uninstall an add-on", commandNames = CommandLineParameters.UNINSTALL_COMMAND)
  class UninstallCommandParameters {
    @Parameter(description = "addon ", arity = 1, required = true)
    def List<String> addon;
    @Parameter(names = ["-v", "--verbose"], hidden = true)
    def boolean verbose
    @Parameter(names = ["-h", "--help"], help = true, hidden = true)
    def boolean help
    def String addonId
  }

}
