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
package org.exoplatform.platform.am

import org.exoplatform.platform.am.cli.CommandLineParameters
import org.exoplatform.platform.am.settings.EnvironmentSettings
import org.exoplatform.platform.am.settings.PlatformSettings
import org.exoplatform.platform.am.utils.Console
import org.exoplatform.platform.am.utils.Logger

/**
 * All services related to list add-ons
 *
 * @author Arnaud Héritier <aheritier@exoplatform.com>
 */
public class AddonListService {
  /**
   * Logger
   */
  private static final Logger LOG = Logger.getInstance()

  /**
   * Addon Services
   */
  private static final AddonService ADDON_SERVICE = AddonService.getInstance()

  /**
   * Singleton
   */
  private static final AddonListService singleton = new AddonListService()

  /**
   * Factory
   *
   * @return The {@link AddonListService} singleton instance
   */
  static AddonListService getInstance() {
    return singleton
  }

  /**
   * You should use the singleton
   */
  private AddonListService() {
  }

  /**
   * List add-ons given the current environment {@code env} and command line {@code parameters}.
   * @param env The execution environment
   * @param parameters Command line parameters for a list action
   */
  void listAddons(
      EnvironmentSettings env,
      CommandLineParameters.ListCommandParameters parameters) {
    if (parameters.installed) {
      listInstalledAddons(env)
    } else if (parameters.outdated) {
      listOutdatedAddons(
          env,
          parameters.unstable,
          parameters.snapshots,
          parameters.noCompat,
          parameters.noCache,
          parameters.offline,
          parameters.catalog)
    } else {
      listAddonsFromCatalogs(
          env,
          parameters.unstable,
          parameters.snapshots,
          parameters.noCompat,
          parameters.noCache,
          parameters.offline,
          parameters.catalog)
    }
  }

  /**
   * List add-ons installed in the current environment {@code env}.
   * @param env The execution environment
   */
  protected void listInstalledAddons(
      EnvironmentSettings env) {
    // Display only installed add-ons
    List<Addon> installedAddons = ADDON_SERVICE.getInstalledAddons(env)
    if (installedAddons.size() > 0) {
      LOG.infoHR("=")
      LOG.info "@|bold Installed add-ons|@"
      LOG.infoHR("=")
      boolean displayIncompatibleAddonsNote
      installedAddons.each {
        Addon anAddon ->
          LOG.info String.format("@|bold,yellow %s|@ - @|bold %s|@", anAddon.id, anAddon.name)
          LOG.wrapLine(anAddon.description, Console.get().width - Logger.Level.INFO.prefix.length()).each {
            LOG.info(it)
          }
          displayIncompatibleAddonsNote = displayVersion(
              "Installed version",
              anAddon,
              env.platform
          ) || displayIncompatibleAddonsNote
          LOG.infoHR()
      }
      if (displayIncompatibleAddonsNote) {
        LOG.info " @|red,bold (*)|@ This version of the add-on is referenced has incompatible with your platform instance"
      }
      LOG.info String.format("""
To uninstall an add-on:
    ${env.manager.scriptName} uninstall @|yellow <addonId>|@
  """)
    } else {
      LOG.info "No add-on installed"
    }
  }

  /**
   * List add-ons installed with a more recent version available in the current environment {@code env}.
   *
   * @param env The execution environment
   * @param allowUnstable List also unstable versions ?
   * @param allowSnapshot List also development versions ?
   * @param noCompat List also add-ons not referenced as compatible with the PLF instance
   * @param noCache Don't use catalog's cache if exist ?
   * @param offline Don't download anything ?
   * @param alternateCatalog Specific remote catalog URL to use
   */
  protected void listOutdatedAddons(
      EnvironmentSettings env,
      Boolean allowUnstable,
      Boolean allowSnapshot,
      Boolean noCompat,
      Boolean noCache,
      Boolean offline,
      URL alternateCatalog) {
    List<Addon> installedAddons = ADDON_SERVICE.getInstalledAddons(env)
    if (installedAddons.size() > 0) {
      List<Addon> availableAddons = ADDON_SERVICE.loadAddons(
          env,
          alternateCatalog,
          noCache,
          offline,
          allowSnapshot,
          allowUnstable
      )
      if (!noCompat) {
        availableAddons = ADDON_SERVICE.filterCompatibleAddons(availableAddons, env.platform)
      }
      List<Addon> outdatedAddons = ADDON_SERVICE.getOutdatedAddons(installedAddons, availableAddons)
      if (outdatedAddons.size() > 0) {
        LOG.infoHR("=")
        LOG.info "@|bold Outdated add-ons|@"
        LOG.infoHR("=")
        boolean displayIncompatibleAddonsNote
        outdatedAddons.groupBy { it.id }.sort().each {
          Addon anAddon = it.value.first()
          LOG.info String.format("@|bold,yellow %s|@ - @|bold %s|@", anAddon.id, anAddon.name)
          LOG.wrapLine(anAddon.description, Console.get().width - Logger.Level.INFO.prefix.length()).each {
            LOG.info(it)
          }
          displayIncompatibleAddonsNote = displayVersion(
              "Installed version",
              anAddon,
              env.platform
          ) || displayIncompatibleAddonsNote
          displayIncompatibleAddonsNote = displayVersion(
              "Latest stable version",
              ADDON_SERVICE.findNewestAddon(
                  anAddon.id,
                  ADDON_SERVICE.findAddonsNewerThan(
                      anAddon,
                      ADDON_SERVICE.filterAddonsByVersion(availableAddons, true, false, false))),
              env.platform
          ) || displayIncompatibleAddonsNote
          displayIncompatibleAddonsNote = displayVersion(
              "Latest unstable version",
              ADDON_SERVICE.findNewestAddon(
                  anAddon.id,
                  ADDON_SERVICE.findAddonsNewerThan(
                      anAddon,
                      ADDON_SERVICE.filterAddonsByVersion(availableAddons, false, true, false))),
              env.platform
          ) || displayIncompatibleAddonsNote
          displayIncompatibleAddonsNote = displayVersion(
              "Latest development version",
              ADDON_SERVICE.findNewestAddon(
                  anAddon.id,
                  ADDON_SERVICE.findAddonsNewerThan(
                      anAddon,
                      ADDON_SERVICE.filterAddonsByVersion(availableAddons, false, false, true))),
              env.platform
          ) || displayIncompatibleAddonsNote
          LOG.infoHR()
        }
        LOG.info String.format("""
    To update an add-on:
        ${env.manager.scriptName} install @|yellow <addonId:[version]>|@ --force
      """)
      } else {
        LOG.info "No outdated add-on found"
      }
    } else {
      LOG.info "No add-on installed"
    }
  }

  /**
   * List add-ons from remote+local catalogs
   *
   * @param env The execution environment
   * @param allowUnstable List also unstable versions ?
   * @param allowSnapshot List also development versions ?
   * @param noCompat List also add-ons not referenced as compatible with the PLF instance
   * @param noCache Don't use catalog's cache if exist ?
   * @param offline Don't download anything ?
   * @param alternateCatalog Specific remote catalog URL to use
   */
  protected void listAddonsFromCatalogs(
      EnvironmentSettings env,
      Boolean allowUnstable,
      Boolean allowSnapshot,
      Boolean noCompat,
      Boolean noCache,
      Boolean offline,
      URL alternateCatalog) {
    List<Addon> availableAddons = ADDON_SERVICE.loadAddons(
        env,
        alternateCatalog,
        noCache,
        offline,
        allowSnapshot,
        allowUnstable
    )
    if (!noCompat) {
      availableAddons = ADDON_SERVICE.filterCompatibleAddons(availableAddons, env.platform)
    }
    if (availableAddons.size() > 0) {
      LOG.infoHR("=")
      LOG.info "@|bold Available add-ons|@"
      LOG.infoHR("=")
      boolean displayIncompatibleAddonsNote
      availableAddons.groupBy { it.id }.sort().each {
        Addon anAddon = it.value.first()
        LOG.info String.format("@|bold,yellow %s|@ - @|bold %s|@", anAddon.id, anAddon.name)
        LOG.wrapLine(anAddon.description, Console.get().width - Logger.Level.INFO.prefix.length()).each {
          LOG.info(it)
        }
        displayIncompatibleAddonsNote = displayVersion(
            "Latest stable version",
            ADDON_SERVICE.findNewestAddon(anAddon.id, ADDON_SERVICE.filterAddonsByVersion(availableAddons, true, false, false)),
            env.platform
        ) || displayIncompatibleAddonsNote
        displayIncompatibleAddonsNote = displayVersion(
            "Latest unstable version",
            ADDON_SERVICE.findNewestAddon(anAddon.id, ADDON_SERVICE.filterAddonsByVersion(availableAddons, false, true, false)),
            env.platform
        ) || displayIncompatibleAddonsNote
        displayIncompatibleAddonsNote = displayVersion(
            "Latest development version",
            ADDON_SERVICE.findNewestAddon(anAddon.id, ADDON_SERVICE.filterAddonsByVersion(availableAddons, false, false, true)),
            env.platform
        ) || displayIncompatibleAddonsNote
        LOG.infoHR()
      }
      if (displayIncompatibleAddonsNote) {
        LOG.info " @|red,bold (*)|@ This version of the add-on is referenced has incompatible with your platform instance"
      }
      LOG.info String.format("""
To install an add-on:
    ${env.manager.scriptName} install @|yellow <addonId:[version]>|@
  """)
    } else {
      LOG.warn "No add-on found in remote and local catalogs"
    }
  }

  /**
   * Display for a given add-on its version, a description and an optional mark if it is incompatible with the current PLF
   * instance
   * @param description The description of the version displayed
   * @param addon The addon on which we extract the version and check the compatibility
   * @param plfSettings The PLF settings to check the compatibility
   * @return true if the version is incompatible
   */
  protected boolean displayVersion(String description, Addon addon, PlatformSettings plfSettings) {
    boolean isIncompatible
    if (addon) {
      String incompatibilityMark = ""
      if (!ADDON_SERVICE.isCompatible(addon, plfSettings)) {
        incompatibilityMark = " @|red,bold (*)|@"
        isIncompatible = true
      }
      LOG.info "@|bold + ${description} :|@ @|yellow ${addon.version}|@${incompatibilityMark}"
    }
    return isIncompatible
  }
}
