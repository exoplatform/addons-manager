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

import org.exoplatform.platform.am.utils.Console
import spock.lang.Specification

/**
 * Command line parameters parsing
 * @author Arnaud Héritier <aheritier@exoplatform.com>
 */
class CommandLineParserTest extends Specification {
  private static final String validCatalogUrl = "http://somewhere.com/catalog"
  private static final String invalidCatalogUrl = "thisIsNotAnUrl"

  CommandLineParser clp = new CommandLineParser("FAKE.sh", Console.get().width)


  def cleanSpec() {
    Console.get().reset()
  }

  def "Test command line parameters to display help"(String[] args) {
    when:
    CommandLineParameters cliArgs = clp.parse(args)
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
    CommandLineParameters cliArgs = clp.parse(args)
    then:
    CommandLineParameters.Command.LIST == cliArgs.command
    !cliArgs.commandList.catalog
    !cliArgs.commandList.noCache
    !cliArgs.commandList.offline
    !cliArgs.commandList.snapshots
    !cliArgs.help
    !cliArgs.verbose
    where:
    args << [
        ["list"],
    ]
  }

  def "Test command line parameters to list all add-ons without using cache"(String[] args) {
    when:
    CommandLineParameters cliArgs = clp.parse(args)
    then:
    CommandLineParameters.Command.LIST == cliArgs.command
    !cliArgs.commandList.catalog
    cliArgs.commandList.noCache
    !cliArgs.commandList.offline
    !cliArgs.commandList.snapshots
    !cliArgs.help
    !cliArgs.verbose
    where:
    args << [
        ["list", "--no-cache"],
    ]
  }

  def "Test command line parameters to list all add-ons while being offline"(String[] args) {
    when:
    CommandLineParameters cliArgs = clp.parse(args)
    then:
    CommandLineParameters.Command.LIST == cliArgs.command
    !cliArgs.commandList.catalog
    !cliArgs.commandList.noCache
    cliArgs.commandList.offline
    !cliArgs.commandList.snapshots
    !cliArgs.help
    !cliArgs.verbose
    where:
    args << [
        ["list", "--offline"],
    ]
  }

  def "Test command line parameters to list all add-ons with verbose logs"(String[] args) {
    when:
    CommandLineParameters cliArgs = clp.parse(args)
    then:
    CommandLineParameters.Command.LIST == cliArgs.command
    !cliArgs.commandList.catalog
    !cliArgs.commandList.noCache
    !cliArgs.commandList.offline
    !cliArgs.commandList.snapshots
    !cliArgs.help
    cliArgs.verbose
    where:
    args << [
        ["list", "-v"],
        ["list", "--verbose"],
        ["-v", "list"],
        ["--verbose", "list"],
    ]
  }

  def "Test command line parameters to list all add-ons including snapshots"(String[] args) {
    when:
    CommandLineParameters cliArgs = clp.parse(args)
    then:
    CommandLineParameters.Command.LIST == cliArgs.command
    !cliArgs.commandList.catalog
    !cliArgs.commandList.noCache
    !cliArgs.commandList.offline
    cliArgs.commandList.snapshots
    !cliArgs.help
    !cliArgs.verbose
    where:
    args << [
        ["list", "--snapshots"],
        ["list", "-s"],
    ]
  }

  def "Test command line parameters to list all add-ons including snapshots with verbose logs"(String[] args) {
    when:
    CommandLineParameters cliArgs = clp.parse(args)
    then:
    CommandLineParameters.Command.LIST == cliArgs.command
    !cliArgs.commandList.catalog
    !cliArgs.commandList.noCache
    !cliArgs.commandList.offline
    cliArgs.commandList.snapshots
    !cliArgs.help
    cliArgs.verbose
    where:
    args << [
        ["list", "-v", "--snapshots"],
        ["list", "--verbose", "--snapshots"],
        ["-v", "list", "--snapshots"],
        ["--verbose", "list", "--snapshots"],
        ["list", "-v", "-s"],
        ["list", "--verbose", "-s"],
        ["-v", "list", "-s"],
        ["--verbose", "list", "-s"],
    ]
  }

  def "Test command line parameters to list add-ons with a valid catalog parameter"(String[] args) {
    when:
    CommandLineParameters cliArgs = clp.parse(args)
    then:
    CommandLineParameters.Command.LIST == cliArgs.command
    validCatalogUrl.equals(cliArgs.commandList.catalog.toString())
    !cliArgs.commandList.noCache
    !cliArgs.commandList.offline
    !cliArgs.commandList.snapshots
    !cliArgs.help
    !cliArgs.verbose
    where:
    args << [
        ["list", "--catalog=${validCatalogUrl}"],
    ]
  }

  def "Test command line parameters to list add-ons with a invalid catalog parameter"(String[] args) {
    when:
    clp.parse(args)
    then:
    thrown(CommandLineParsingException)
    where:
    args << [
        ["list", "--catalog=${invalidCatalogUrl}"],
    ]
  }

  def "Test command line parameters to install the latest version of an add-on"(String[] args) {
    when:
    CommandLineParameters cliArgs = clp.parse(args)
    then:
    CommandLineParameters.Command.INSTALL == cliArgs.command
    "my-addon".equals(cliArgs.commandInstall.addonId)
    !cliArgs.commandInstall.addonVersion
    !cliArgs.commandInstall.catalog
    !cliArgs.commandInstall.force
    !cliArgs.commandInstall.noCache
    !cliArgs.commandInstall.offline
    !cliArgs.commandInstall.snapshots
    !cliArgs.help
    !cliArgs.verbose
    where:
    args << [
        ["install", "my-addon"],
    ]
  }

  def "Test command line parameters to install the latest version of an add-on without using cache"(String[] args) {
    when:
    CommandLineParameters cliArgs = clp.parse(args)
    then:
    CommandLineParameters.Command.INSTALL == cliArgs.command
    "my-addon".equals(cliArgs.commandInstall.addonId)
    !cliArgs.commandInstall.addonVersion
    !cliArgs.commandInstall.catalog
    !cliArgs.commandInstall.force
    cliArgs.commandInstall.noCache
    !cliArgs.commandInstall.offline
    !cliArgs.commandInstall.snapshots
    !cliArgs.help
    !cliArgs.verbose
    where:
    args << [
        ["install", "my-addon", "--no-cache"],
    ]
  }

  def "Test command line parameters to install the latest version of an add-on while being offline"(String[] args) {
    when:
    CommandLineParameters cliArgs = clp.parse(args)
    then:
    CommandLineParameters.Command.INSTALL == cliArgs.command
    "my-addon".equals(cliArgs.commandInstall.addonId)
    !cliArgs.commandInstall.addonVersion
    !cliArgs.commandInstall.catalog
    !cliArgs.commandInstall.force
    !cliArgs.commandInstall.noCache
    cliArgs.commandInstall.offline
    !cliArgs.commandInstall.snapshots
    !cliArgs.help
    !cliArgs.verbose
    where:
    args << [
        ["install", "my-addon", "--offline"],
    ]
  }

  def "Test command line parameters to install a given version of an add-on"(String[] args) {
    when:
    CommandLineParameters cliArgs = clp.parse(args)
    then:
    "my-addon".equals(cliArgs.commandInstall.addonId)
    "42".equals(cliArgs.commandInstall.addonVersion)
    !cliArgs.commandInstall.catalog
    !cliArgs.commandInstall.force
    !cliArgs.commandInstall.noCache
    !cliArgs.commandInstall.offline
    !cliArgs.commandInstall.snapshots
    !cliArgs.help
    !cliArgs.verbose
    where:
    args << [
        ["install", "my-addon:42"],
    ]
  }

  def "Test command line parameters to force to install an add-on"(String[] args) {
    when:
    CommandLineParameters cliArgs = clp.parse(args)
    then:
    CommandLineParameters.Command.INSTALL == cliArgs.command
    "my-addon".equals(cliArgs.commandInstall.addonId)
    !cliArgs.commandInstall.catalog
    cliArgs.commandInstall.force
    !cliArgs.commandInstall.noCache
    !cliArgs.commandInstall.offline
    !cliArgs.commandInstall.snapshots
    !cliArgs.help
    !cliArgs.verbose
    where:
    args << [
        ["install", "my-addon", "--force"],
        ["install", "my-addon:42", "--force"],
        ["install", "my-addon", "-f"],
        ["install", "my-addon:42", "-f"],
    ]
  }

  def "Test command line parameters to install a SNAPSHOT version of an add-on"(String[] args) {
    when:
    CommandLineParameters cliArgs = clp.parse(args)
    then:
    CommandLineParameters.Command.INSTALL == cliArgs.command
    "my-addon".equals(cliArgs.commandInstall.addonId)
    !cliArgs.commandInstall.catalog
    !cliArgs.commandInstall.force
    !cliArgs.commandInstall.noCache
    !cliArgs.commandInstall.offline
    cliArgs.commandInstall.snapshots
    !cliArgs.help
    !cliArgs.verbose
    where:
    args << [
        ["install", "my-addon", "--snapshots"],
        ["install", "my-addon:42-SNAPSHOT", "--snapshots"],
        ["install", "my-addon", "-s"],
        ["install", "my-addon:42-SNAPSHOT", "-s"],
    ]
  }

  def "Test command line parameters to install an add-on with a valid catalog parameter"(String[] args) {
    when:
    CommandLineParameters cliArgs = clp.parse(args)
    then:
    CommandLineParameters.Command.INSTALL == cliArgs.command
    "my-addon".equals(cliArgs.commandInstall.addonId)
    validCatalogUrl.equals(cliArgs.commandInstall.catalog.toString())
    !cliArgs.commandInstall.force
    !cliArgs.commandInstall.noCache
    !cliArgs.commandInstall.offline
    !cliArgs.commandInstall.snapshots
    !cliArgs.help
    !cliArgs.verbose
    where:
    args << [
        ["install", "my-addon", "--catalog=${validCatalogUrl}"],
        ["install", "my-addon:42", "--catalog=${validCatalogUrl}"],
    ]
  }

  def "Test command line parameters to install an add-on with a invalid catalog parameter"(String[] args) {
    when:
    clp.parse(args)
    then:
    thrown(CommandLineParsingException)
    where:
    args << [
        ["install", "my-addon", "--catalog=${invalidCatalogUrl}"],
        ["install", "my-addon:42", "--catalog=${invalidCatalogUrl}"],
    ]
  }

  def "Test command line parameters to uninstall an add-on"(String[] args) {
    when:
    CommandLineParameters cliArgs = clp.parse(args)
    then:
    CommandLineParameters.Command.UNINSTALL == cliArgs.command
    "my-addon".equals(cliArgs.commandUninstall.addonId)
    !cliArgs.help
    !cliArgs.verbose
    where:
    args << [
        ["uninstall", "my-addon"],
    ]
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
        ["install", "foo", "--catalog=http://firstURL.com", "--catalog=http://secondURL.com"],
        ["uninstall", "foo", "bar"],
    ]
  }

}
