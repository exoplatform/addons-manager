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

import spock.lang.Specification

/**
 * Command line parameters parsing
 * @author Arnaud Héritier <aheritier@exoplatform.com>
 */
class CLITest extends Specification {
  def settings = new ManagerSettings()

  def "Test command line parameters to display help"(String[] args) {
    when:
    println "Input parameters : $args"
    CLI.initialize(args, settings)
    then:
    ManagerSettings.Action.HELP == settings.action
    where:
    args << [
        [],
        ["-h"],
        ["--help"]]
  }

  def "Test command line parameters to list all add-ons"(String[] args) {
    when:
    println "Input parameters : $args"
    CLI.initialize(args, settings)
    then:
    ManagerSettings.Action.LIST == settings.action
    !settings.snapshots
    where:
    args << [
        ["--list", "-l"],
        ["--verbose", "-v", ""]].combinations().collect { it.minus("") }
  }

  def "Test command line parameters to list all add-ons including snapshots"(String[] args) {
    when:
    println "Input parameters : $args"
    CLI.initialize(args, settings)
    then:
    ManagerSettings.Action.LIST == settings.action
    settings.snapshots
    where:
    args << [
        ["--list", "-l"],
        ["--snapshots", "-s"],
        ["--verbose", "-v", ""]].combinations().collect { it.minus("") }
  }

  def "Test command line parameters to install the latest version of an add-on"(String[] args) {
    when:
    println "Input parameters : $args"
    CLI.initialize(args, settings)
    then:
    ManagerSettings.Action.INSTALL == settings.action
    !settings.snapshots
    !settings.force
    "my-addon".equals(settings.addonId)
    settings.addonVersion == null
    where:
    args << [
        ["--install", "-i"],
        ["my-addon"],
        ["--verbose", "-v", ""]].combinations().collect { it.minus("") }
  }

  def "Test command line parameters to install a given version of an add-on"(String[] args) {
    when:
    println "Input parameters : $args"
    CLI.initialize(args, settings)
    then:
    ManagerSettings.Action.INSTALL == settings.action
    !settings.snapshots
    !settings.force
    "my-addon".equals(settings.addonId)
    "42".equals(settings.addonVersion)
    where:
    args << [
        ["--install", "-i"],
        ["my-addon:42"],
        ["--verbose", "-v", ""]].combinations().collect { it.minus("") }
  }

  def "Test command line parameters to force to install an add-on"(String[] args) {
    when:
    println "Input parameters : $args"
    CLI.initialize(args, settings)
    then:
    ManagerSettings.Action.INSTALL == settings.action
    !settings.snapshots
    settings.force
    "my-addon".equals(settings.addonId)
    where:
    args << [
        ["--install", "-i"],
        ["my-addon", "my-addon:42"],
        ["--force", "-f"],
        ["--verbose", "-v", ""]].combinations().collect { it.minus("") }
  }

  def "Test command line parameters to install a SNAPSHOT version of an add-on"(String[] args) {
    when:
    println "Input parameters : $args"
    CLI.initialize(args, settings)
    then:
    ManagerSettings.Action.INSTALL == settings.action
    settings.snapshots
    !settings.force
    "my-addon".equals(settings.addonId)
    where:
    args << [
        ["--install", "-i"],
        ["my-addon", "my-addon:42-SNAPSHOT"],
        ["--snapshots", "-s"],
        ["--verbose", "-v", ""]].combinations().collect { it.minus("") }
  }

  def "Test command line parameters to uninstall an add-on"(String[] args) {
    when:
    println "Input parameters : $args"
    CLI.initialize(args, settings)
    then:
    ManagerSettings.Action.UNINSTALL == settings.action
    "my-addon".equals(settings.addonId)
    where:
    args << [
        ["--uninstall", "-u"],
        ["my-addon"],
        ["--verbose", "-v", ""]].combinations().collect { it.minus("") }
  }

}
