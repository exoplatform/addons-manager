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

import groovy.transform.Canonical
import org.exoplatform.platform.am.settings.PlatformSettings

@Canonical(includes = ['id', 'version'])
class Addon {

  private final static String NA = "N/A"

  String id = NA
  String version = NA
  Boolean unstable = Boolean.FALSE
  String name = NA
  String description = NA
  String releaseDate = NA
  String sourceUrl = NA
  String screenshotUrl = NA
  String thumbnailUrl = NA
  String documentationUrl = NA
  String downloadUrl = NA
  String vendor = NA
  String author = NA
  String authorEmail = NA
  String license = NA
  String licenseUrl = NA
  Boolean mustAcceptLicense = Boolean.FALSE
  List<PlatformSettings.DistributionType> supportedDistributions = Collections.EMPTY_LIST
  List<PlatformSettings.AppServerType> supportedApplicationServers = Collections.EMPTY_LIST
  String compatibility = NA
  List<String> installedLibraries = Collections.EMPTY_LIST
  List<String> installedWebapps = Collections.EMPTY_LIST

  boolean isSnapshot() {
    return version =~ '.*SNAPSHOT$'
  }
}