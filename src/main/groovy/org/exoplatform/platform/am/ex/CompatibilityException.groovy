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
package org.exoplatform.platform.am.ex

import org.exoplatform.platform.am.Addon
import org.exoplatform.platform.am.AddonService
import org.exoplatform.platform.am.settings.PlatformSettings

import static org.exoplatform.platform.am.AddonsManagerConstants.RETURN_CODE_INCOMPATIBILITY_ERROR

/**
 * @author Arnaud Héritier <aheritier@exoplatform.com>
 */
class CompatibilityException extends AddonsManagerException {

  CompatibilityException(Addon addon, PlatformSettings plfSettings) {
    super(
        "The add-on ${addon.id}:${addon.version} is not compatible : " +
            (!addon.supportedDistributions.contains(plfSettings.distributionType) ?
                addon.supportedDistributions.size() == 1 ?
                    "Only distribution ${addon.supportedDistributions[0]} is supported. " :
                    "Only distributions ${addon.supportedDistributions.join(", ")} are supported. " :
                "") +
            (!AddonService.instance.testAppServerTypeCompatibility(plfSettings.appServerType, addon.supportedApplicationServers) ?
                addon.supportedApplicationServers.size() == 1 ?
                    "Only application server ${addon.supportedApplicationServers[0]} is supported. " :
                    "Only application servers ${addon.supportedApplicationServers} are supported. " :
                "") +
            (!AddonService.instance.testVersionCompatibility(
                plfSettings.version, addon.compatibility) ?
                "Only eXo platform versions ${addon.compatibility} are supported. " :
                "") +
            "Use --no-compat to bypass this compatibility check and install anyway"
    )
  }

  @Override
  int getErrorCode() {
    return RETURN_CODE_INCOMPATIBILITY_ERROR
  }
}
