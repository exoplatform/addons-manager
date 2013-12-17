package org.exoplatform.platform.addon

import static org.fusesource.jansi.Ansi.ansi

/**
 * Copyright (C) 2003-2013 eXo Platform SAS.
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

/**
 * Command line utility to manage Platform addons.
 */

try {

// Initialize logging system
  Logging.initialize()
// And display header
  Logging.displayHeader()
// Parse command line parameters and fill settings with user inputs
  if (!CLI.initialize(args)) {
    Logging.dispose()
    System.exit 1
  } else if (Settings.instance.action == Settings.Action.HELP) {
    Logging.dispose()
    System.exit 0
  }

// Validate execution settings
  Settings.instance.validate()

  def List<Addon> addons = new ArrayList<Addon>()
  def catalog
  if (Settings.instance.localAddonsCatalogFile.exists()) {
    Logging.logWithStatus("Reading local add-ons list...") {
      catalog = Settings.instance.localAddonsCatalog
    }
    Logging.logWithStatus("Loading add-ons...") {
      addons.addAll(Addon.parseJSONAddonsList(catalog))
    }
  } else {
    Logging.displayMsgVerbose("No local catalog to load")
  }
  Logging.logWithStatus("Downloading central add-ons list...") {
    catalog = Settings.instance.centralCatalog
  }
  Logging.logWithStatus("Loading add-ons...") {
    addons.addAll(Addon.parseJSONAddonsList(catalog))
  }

  switch (Settings.instance.action) {
    case Settings.Action.LIST:
      println ansi().render("\n@|bold Available add-ons:|@\n")
      addons.findAll { it.isStable() || Settings.instance.snapshots }.groupBy { it.id }.each {
        Addon anAddon = it.value.first()
        printf(ansi().render("+ @|bold,yellow %-${addons.id*.size().max()}s|@ : @|bold %s|@, %s\n").toString(), anAddon.id,
               anAddon.name, anAddon.description)
        printf(ansi().render("     Available Version(s) : @|bold,yellow %-${addons.version*.size().max()}s|@ \n\n").toString(),
               ansi().render(it.value.collect { "@|yellow ${it.version}|@" }.join(', ')))
      }
      println ansi().render("""
  To have more details about an add-on:
    ${CLI.getScriptName()} --info <@|yellow add-on|@>
  To install an add-on:
    ${CLI.getScriptName()} --install <@|yellow add-on|@>
  """).toString()
      break
    case Settings.Action.INSTALL:
      def addonList = addons.findAll { (it.isStable() || Settings.instance.snapshots) && Settings.instance.addonId.equals(it.id) }
      if (addonList.size() == 0) {
        Logging.logWithStatusKO("No add-on with identifier ${Settings.instance.addonId} found")
        break
      }
      def addon = addonList.first();
      addon.install()
      break
    case Settings.Action.UNINSTALL:
      def statusFile = new File(Settings.instance.addonsDirectory, "${Settings.instance.addonId}.status")
      if (statusFile.exists()) {
        def addon
        Logging.logWithStatus("Loading addon details...") {
          addon = Addon.parseJSONAddon(statusFile.text);
        }
        addon.uninstall()
      } else {
        Logging.logWithStatusKO("Add-on not installed. Exiting.")
      }
      break
    default:
      Logging.displayMsgError("Unsupported operation.")
      Logging.dispose()
      System.exit 1
  }
} catch (Exception e) {
  Logging.displayException(e)
  System.exit 1
} finally {
  Logging.dispose()
}

System.exit 0