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

import org.exoplatform.platform.am.settings.PlatformSettings
import spock.lang.Shared
import spock.lang.Specification

/**
 * @author Arnaud Héritier <aheritier@exoplatform.com>
 */
class CatalogServiceTest extends Specification {

  @Shared
  CatalogService catalogService = new CatalogService()

  @Shared
  Addon addon1 = new Addon(
      id: "addon1", version: "42",
      supportedApplicationServers: [PlatformSettings.AppServerType.JBOSS, PlatformSettings.AppServerType.TOMCAT],
      supportedDistributions: [PlatformSettings.DistributionType.COMMUNITY, PlatformSettings.DistributionType.ENTERPRISE])

  @Shared
  Addon addon2 = new Addon(
      id: "addon2", version: "42",
      supportedApplicationServers: [PlatformSettings.AppServerType.JBOSS],
      supportedDistributions: [PlatformSettings.DistributionType.ENTERPRISE])

  @Shared
  Addon addon3 = new Addon(
      id: "addon3", version: "42",
      supportedApplicationServers: [PlatformSettings.AppServerType.TOMCAT],
      supportedDistributions: [PlatformSettings.DistributionType.COMMUNITY])

  @Shared
  Addon addon4 = new Addon(
      id: "addon4", version: "42",
      supportedApplicationServers: [PlatformSettings.AppServerType.TOMCAT],
      supportedDistributions: [PlatformSettings.DistributionType.ENTERPRISE])
  @Shared
  Addon addon4b = new Addon(
      id: "addon4", version: "43",
      supportedApplicationServers: [PlatformSettings.AppServerType.TOMCAT],
      supportedDistributions: [PlatformSettings.DistributionType.ENTERPRISE])
  @Shared
  Addon addon5 = new Addon(
      id: "addon5", version: "42",
      supportedApplicationServers: [PlatformSettings.AppServerType.TOMCAT],
      supportedDistributions: [PlatformSettings.DistributionType.ENTERPRISE])

  /**
   * [AM_CAT_07] At merge, de-duplication of add-on entries of the local and remote catalogs is done using  ID, Version,
   * Distributions, Application Servers as the identifier. In case of duplication, the remote entry takes precedence
   */
  def "mergeCatalogs must implement [AM_CAT_07]"() {
    when:
    List<Addon> remoteCatalog = [addon1, addon2, addon3, addon4]
    List<Addon> localCatalog = [addon1, addon4b, addon5]
    then:
    catalogService.mergeCatalogs(remoteCatalog, localCatalog, PlatformSettings.DistributionType.ENTERPRISE,
                                 PlatformSettings.AppServerType.TOMCAT) == [addon1, addon4, addon4b, addon5]
  }

  def "filterCatalog must keep addons supporting a given application server and distribution type"() {
    when:
    List<Addon> addonsCatalog = [addon1, addon2, addon3, addon4]
    then:
    // addons 1 and 3 are supporting appsrv tomcat on community edition
    catalogService.filterCatalog(addonsCatalog,
                                 PlatformSettings.DistributionType.COMMUNITY,
                                 PlatformSettings.AppServerType.TOMCAT) == [addon1, addon3]
    // addons 1 and 3 are supporting appsrv tomcat on enterprise edition
    catalogService.filterCatalog(addonsCatalog,
                                 PlatformSettings.DistributionType.ENTERPRISE,
                                 PlatformSettings.AppServerType.TOMCAT) == [addon1, addon4]
    // addon 1 is supporting appsrv jboss on community edition
    // TODO : The current model doesn't let us know that it is an impossible combination
    catalogService.filterCatalog(addonsCatalog,
                                 PlatformSettings.DistributionType.COMMUNITY,
                                 PlatformSettings.AppServerType.JBOSS) == [addon1]
    // addons 1 and 2 are supporting appsrv jboss on enterprise edition
    catalogService.filterCatalog(addonsCatalog,
                                 PlatformSettings.DistributionType.ENTERPRISE,
                                 PlatformSettings.AppServerType.JBOSS) == [addon1, addon2]
  }
}
