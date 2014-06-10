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

import org.exoplatform.platform.am.utils.Logging
import spock.lang.Specification
/**
 * Command line parameters parsing
 * @author Arnaud Héritier <aheritier@exoplatform.com>
 */
class CommandLineParserTest extends Specification {
  def clp = new CommandLineParser("FAKE.sh", Logging.CONSOLE_WIDTH)

  def setupSpec() {
    Logging.verbose = true
  }

  def "Test command line parameters to display help"(String[] args) {
    when:
    def cliArgs = clp.parse(args)
    then:
    cliArgs.help
    where:
    args << [
        ["-h"],
        ["--help"],
    ]
  }

  def "Test command line parameters to list all add-ons"(String[] args) {
    when:
    def cliArgs = clp.parse(args)
    then:
    CommandLineParameters.Command.LIST == cliArgs.command
    !cliArgs.commandList.snapshots
    where:
    args << [
        ["--verbose", "-v", ""],
        ["list"],
    ].combinations().collect { it.minus("") }
  }

  def "Test command line parameters to list all add-ons including snapshots"(String[] args) {
    when:
    def cliArgs = clp.parse(args)
    then:
    CommandLineParameters.Command.LIST == cliArgs.command
    cliArgs.commandList.snapshots
    where:
    args << [
        ["--verbose", "-v", ""],
        ["list"],
        ["--snapshots", "-s"],
    ].combinations().collect { it.minus("") }
  }

  def "Test command line parameters to install the latest version of an add-on"(String[] args) {
    when:
    def cliArgs = clp.parse(args)
    then:
    CommandLineParameters.Command.INSTALL == cliArgs.command
    !cliArgs.commandInstall.snapshots
    !cliArgs.commandInstall.force
    "my-addon".equals(cliArgs.commandInstall.addonId)
    cliArgs.commandInstall.addonVersion == null
    where:
    args << [
        ["--verbose", "-v", ""],
        ["install"],
        ["my-addon"],
    ].combinations().collect { it.minus("") }
  }

  def "Test command line parameters to install a given version of an add-on"(String[] args) {
    when:
    def cliArgs = clp.parse(args)
    then:
    CommandLineParameters.Command.INSTALL == cliArgs.command
    !cliArgs.commandInstall.snapshots
    !cliArgs.commandInstall.force
    "my-addon".equals(cliArgs.commandInstall.addonId)
    "42".equals(cliArgs.commandInstall.addonVersion)
    where:
    args << [
        ["--verbose", "-v", ""],
        ["install"],
        ["my-addon:42"],
    ].combinations().collect { it.minus("") }
  }

  def "Test command line parameters to force to install an add-on"(String[] args) {
    when:
    def cliArgs = clp.parse(args)
    then:
    CommandLineParameters.Command.INSTALL == cliArgs.command
    !cliArgs.commandInstall.snapshots
    cliArgs.commandInstall.force
    "my-addon".equals(cliArgs.commandInstall.addonId)
    where:
    args << [
        ["--verbose", "-v", ""],
        ["install"],
        ["my-addon", "my-addon:42"],
        ["--force", "-f"],
    ].combinations().collect { it.minus("") }
  }

  def "Test command line parameters to install a SNAPSHOT version of an add-on"(String[] args) {
    when:
    def cliArgs = clp.parse(args)
    then:
    CommandLineParameters.Command.INSTALL == cliArgs.command
    cliArgs.commandInstall.snapshots
    !cliArgs.commandInstall.force
    "my-addon".equals(cliArgs.commandInstall.addonId)
    where:
    args << [
        ["--verbose", "-v", ""],
        ["install"],
        ["my-addon", "my-addon:42-SNAPSHOT"],
        ["--snapshots", "-s"],
    ].combinations().collect { it.minus("") }
  }

  def "Test command line parameters to uninstall an add-on"(String[] args) {
    when:
    def cliArgs = clp.parse(args)
    then:
    CommandLineParameters.Command.UNINSTALL == cliArgs.command
    "my-addon".equals(cliArgs.commandUninstall.addonId)
    where:
    args << [
        ["--verbose", "-v", ""],
        ["uninstall"],
        ["my-addon"],
    ].combinations().collect { it.minus("") }
  }

  def "Test invalid command line parameters"(String[] args) {
    when:
    clp.parse(args)
    then:
    thrown(CommandLineParsingException)
    where:
    args << [
        // Missing params
        ["install"],
        ["uninstall"],
        // Too much params
        ["install", "foo", "bar"],
        ["uninstall", "foo", "bar"],
    ]
  }


}
