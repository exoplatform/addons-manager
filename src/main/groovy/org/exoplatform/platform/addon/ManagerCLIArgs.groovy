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

/**
 * CLI Arguments passed to the manager
 * @author Arnaud Héritier <aheritier@exoplatform.com>
 */
class ManagerCLIArgs {
  def Action action
  def String addonId
  def String addonVersion
  @Parameter(names = ["-v", "--verbose"], description = "Show verbose logs")
  def boolean verbose
  @Parameter(names = ["-f", "--force"], description = "Enforce to download again and reinstall an add-on already deployed")
  def boolean force
  @Parameter(names = ["-s", "--snapshots"], description = "List also add-ons SNAPSHOTs")
  def boolean snapshots
  @Parameter(names = ["-h", "--help"], description = "Show usage information", help = true)
  def boolean helpAction;
  @Parameter(names = ["-l", "--list"], description = "List all available add-ons")
  def boolean listAction
  @Parameter(names = ["-i", "--install"], description = "Install an add-on")
  def String installAction
  @Parameter(names = ["-u", "--uninstall"], description = "Uninstall an add-on")
  def String uninstallAction


  enum Action {
    LIST, INSTALL, UNINSTALL, HELP
  }

}
