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
