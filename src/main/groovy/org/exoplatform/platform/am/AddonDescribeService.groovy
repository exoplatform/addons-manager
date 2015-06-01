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
import org.exoplatform.platform.am.utils.Logger

import java.text.SimpleDateFormat

/**
 * All services to describe add-ons
 *
 * @author Arnaud Héritier <aheritier@exoplatform.com>
 */
public class AddonDescribeService {
  /**
   * Logger
   */
  private static final Logger LOG = Logger.getInstance()

  /**
   * Add-on Services
   */
  private static final AddonService ADDON_SERVICE = AddonService.getInstance()

  /**
   * Singleton
   */
  private static final AddonDescribeService singleton = new AddonDescribeService()

  /**
   * Factory
   *
   * @return The {@link AddonDescribeService} singleton instance
   */
  static AddonDescribeService getInstance() {
    return singleton
  }

  /**
   * You should use the singleton
   */
  private AddonDescribeService() {
  }

  /**
   * Describe an add-on given the current environment {@code env} and command line {@code parameters}.
   * @param env The execution environment
   * @param parameters Command line parameters for a describe action
   */
  void describeAddon(
      EnvironmentSettings env,
      CommandLineParameters.DescribeCommandParameters parameters) {
    List<Addon> availableAddons = ADDON_SERVICE.loadAddons(
        env,
        parameters.catalog,
        parameters.noCache,
        parameters.offline,
        true,
        true)
    Addon addon = ADDON_SERVICE.findAddon(
        availableAddons,
        parameters.addonId,
        parameters.addonVersion,
        true,
        true)
    boolean displayIncompatibleAddonsNote
    describeAddon(
        addon,
        ADDON_SERVICE.filterAddonsByVersion(
            availableAddons.findAll { parameters.addonId == it.id }, true, false, false).collect {
          displayIncompatibleAddonsNote = !ADDON_SERVICE.isCompatible(it, env.platform) || displayIncompatibleAddonsNote
          String version = addon.version == it.version ? "@|yellow ${it.version}|@" : it.version
          String compatibility = ADDON_SERVICE.isCompatible(it, env.platform) ? "" : " @|red,bold (*)|@"
          return "${version}${compatibility}"
        },
        ADDON_SERVICE.filterAddonsByVersion(
            availableAddons.findAll { parameters.addonId == it.id }, false, true, false).collect {
          displayIncompatibleAddonsNote = !ADDON_SERVICE.isCompatible(it, env.platform) || displayIncompatibleAddonsNote
          String version = addon.version == it.version ? "@|yellow ${it.version}|@" : it.version
          String compatibility = ADDON_SERVICE.isCompatible(it, env.platform) ? "" : " @|red,bold (*)|@"
          return "${version}${compatibility}"
        },
        ADDON_SERVICE.filterAddonsByVersion(
            availableAddons.findAll { parameters.addonId == it.id }, false, false, true).collect {
          displayIncompatibleAddonsNote = !ADDON_SERVICE.isCompatible(it, env.platform) || displayIncompatibleAddonsNote
          String version = addon.version == it.version ? "@|yellow ${it.version}|@" : it.version
          String compatibility = ADDON_SERVICE.isCompatible(it, env.platform) ? "" : " @|red,bold (*)|@"
          return "${version}${compatibility}"
        })
    if (displayIncompatibleAddonsNote) {
      LOG.info " @|red,bold (*)|@ Your eXo Platform instance is not compatible with this version of the add-on"
    }
    LOG.info String.format("""
To install this add-on:
  ${env.manager.scriptName} install @|yellow ${addon.id}:${addon.version}|@
""")

  }

  /**
   * Describe an add-on
   * @param addon The add-on to describe
   */
  protected void describeAddon(
      final Addon addon,
      List<String> stableVersions,
      List<String> unstableVersions,
      List<String> snapshotsVersions) {
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd")
    LOG.infoHR("=")
    LOG.info "Informations about add-on @|bold,yellow ${addon.id}:${addon.version}|@"
    LOG.infoHR("=")
    Map map = [
        "Identifier"                        : addon.id,
        "Stable versions"                   : stableVersions.join(", "),
        "Unstable versions"                 : unstableVersions.join(", "),
        "Development versions"              : snapshotsVersions.join(", "),
        "Name"                              : addon.name,
        "Description"                       : addon.description,
        "Release date (YYYY-MM-DD)"         : addon.releaseDate ? sdf.format(sdf.parse(addon.releaseDate)) : null,
        "Sources URL"                       : addon.sourceUrl ? URLDecoder.decode(addon.sourceUrl, "UTF-8") : null,
        "Screenshot URL"                    : addon.screenshotUrl ? URLDecoder.decode(addon.screenshotUrl, "UTF-8") : null,
        "Thumbnail URL"                     : addon.thumbnailUrl ? URLDecoder.decode(addon.thumbnailUrl, "UTF-8") : null,
        "Documentation URL"                 : addon.documentationUrl ? URLDecoder.decode(addon.documentationUrl, "UTF-8") : null,
        "Download URL"                      : addon.downloadUrl ? URLDecoder.decode(addon.downloadUrl, "UTF-8") : null,
        "Vendor"                            : addon.vendor,
        "Author"                            : addon.author,
        "Author email"                      : addon.authorEmail,
        "License"                           : addon.license,
        "License URL"                       : addon.licenseUrl ? URLDecoder.decode(addon.licenseUrl, "UTF-8") : null,
        "License must be accepted"          : addon.mustAcceptLicense,
        "Supported application Server(s)"   : addon.supportedApplicationServers.join(", "),
        "Supported platform distribution(s)": addon.supportedDistributions.join(", "),
        "Supported platform version(s)"     : addon.compatibility,
        "eXo Support Services"              : addon.supported ? "yes": null] as LinkedHashMap //LinkedHashMap to keep the insertion order
    map.keySet().findAll { map.get(it) }.each {
      LOG.info String.format("@|bold %-${map.keySet()*.size().max()}s|@ : %s", it, map.get(it))
    }
    LOG.infoHR()
  }
}
