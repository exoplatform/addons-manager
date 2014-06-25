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
class AddonServiceTest extends Specification {

  @Shared
  AddonService addonService = AddonService.getInstance()

  /**
   * [AM_CAT_07] At merge, de-duplication of add-on entries of the local and remote catalogs is done using  ID, Version,
   * Distributions, Application Servers as the identifier. In case of duplication, the remote entry takes precedence
   */
  def "mergeCatalogs must implement [AM_CAT_07]"() {
    when:
    Addon addon1 = new Addon(
        id: "addon1", version: "42",
        supportedApplicationServers: [PlatformSettings.AppServerType.JBOSS, PlatformSettings.AppServerType.TOMCAT],
        supportedDistributions: [PlatformSettings.DistributionType.COMMUNITY, PlatformSettings.DistributionType.ENTERPRISE])
    Addon addon2 = new Addon(
        id: "addon2", version: "42",
        supportedApplicationServers: [PlatformSettings.AppServerType.JBOSS],
        supportedDistributions: [PlatformSettings.DistributionType.ENTERPRISE])
    Addon addon3 = new Addon(
        id: "addon3", version: "42",
        supportedApplicationServers: [PlatformSettings.AppServerType.TOMCAT],
        supportedDistributions: [PlatformSettings.DistributionType.COMMUNITY])
    Addon addon4 = new Addon(
        id: "addon4", version: "42",
        supportedApplicationServers: [PlatformSettings.AppServerType.TOMCAT],
        supportedDistributions: [PlatformSettings.DistributionType.ENTERPRISE])
    Addon addon4b = new Addon(
        id: "addon4", version: "43",
        supportedApplicationServers: [PlatformSettings.AppServerType.TOMCAT],
        supportedDistributions: [PlatformSettings.DistributionType.ENTERPRISE])
    Addon addon5 = new Addon(
        id: "addon5", version: "42",
        supportedApplicationServers: [PlatformSettings.AppServerType.TOMCAT],
        supportedDistributions: [PlatformSettings.DistributionType.ENTERPRISE])
    List<Addon> remoteCatalog = [addon1, addon2, addon3, addon4]
    List<Addon> localCatalog = [addon1, addon4b, addon5]
    then:
    addonService.mergeCatalogs(remoteCatalog, localCatalog, PlatformSettings.DistributionType.ENTERPRISE,
                               PlatformSettings.AppServerType.TOMCAT).sort() == [addon1, addon4, addon4b, addon5].sort()
  }

  def "filterAddonsByCompatibility must keep addons supporting a given application server and distribution type"() {
    when:
    Addon addon1 = new Addon(
        id: "addon1", version: "42",
        supportedApplicationServers: [PlatformSettings.AppServerType.JBOSS, PlatformSettings.AppServerType.TOMCAT],
        supportedDistributions: [PlatformSettings.DistributionType.COMMUNITY, PlatformSettings.DistributionType.ENTERPRISE])
    Addon addon2 = new Addon(
        id: "addon2", version: "42",
        supportedApplicationServers: [PlatformSettings.AppServerType.JBOSS],
        supportedDistributions: [PlatformSettings.DistributionType.ENTERPRISE])
    Addon addon3 = new Addon(
        id: "addon3", version: "42",
        supportedApplicationServers: [PlatformSettings.AppServerType.TOMCAT],
        supportedDistributions: [PlatformSettings.DistributionType.COMMUNITY])
    Addon addon4 = new Addon(
        id: "addon4", version: "42",
        supportedApplicationServers: [PlatformSettings.AppServerType.TOMCAT],
        supportedDistributions: [PlatformSettings.DistributionType.ENTERPRISE])
    List<Addon> addonsCatalog = [addon1, addon2, addon3, addon4]
    then:
    // addons 1 and 3 are supporting appsrv tomcat on community edition
    addonService.filterAddonsByCompatibility(addonsCatalog,
                                             PlatformSettings.DistributionType.COMMUNITY,
                                             PlatformSettings.AppServerType.TOMCAT).sort() == [addon1, addon3].sort()
    // addons 1 and 3 are supporting appsrv tomcat on enterprise edition
    addonService.filterAddonsByCompatibility(addonsCatalog,
                                             PlatformSettings.DistributionType.ENTERPRISE,
                                             PlatformSettings.AppServerType.TOMCAT).sort() == [addon1, addon4].sort()
    // addon 1 is supporting appsrv jboss on community edition
    // TODO : The current model doesn't let us know that it is an impossible combination
    addonService.filterAddonsByCompatibility(addonsCatalog,
                                             PlatformSettings.DistributionType.COMMUNITY,
                                             PlatformSettings.AppServerType.JBOSS).sort() == [addon1].sort()
    // addons 1 and 2 are supporting appsrv jboss on enterprise edition
    addonService.filterAddonsByCompatibility(addonsCatalog,
                                             PlatformSettings.DistributionType.ENTERPRISE,
                                             PlatformSettings.AppServerType.JBOSS).sort() == [addon1, addon2].sort()
  }

  def "filterAddonsByVersion must keep only stable versions"() {
    when:
    List<Addon> addons = [
        new Addon(id: "addon", version: "42-SNAPSHOT", unstable: true),
        new Addon(id: "addon", version: "42-alpha1", unstable: true),
        new Addon(id: "addon", version: "42", unstable: false)
    ]
    then:
    addonService.filterAddonsByVersion(addons, false, false).sort() == [new Addon(id: "addon", version: "42",
                                                                                  unstable: false)].sort()
  }

  def "filterAddonsByVersion must keep stable and snapshot versions"() {
    when:
    List<Addon> addons = [
        new Addon(id: "addon", version: "42-SNAPSHOT", unstable: true),
        new Addon(id: "addon", version: "42-alpha1", unstable: true),
        new Addon(id: "addon", version: "42", unstable: false)
    ]
    then:
    addonService.filterAddonsByVersion(addons, true, false).sort() == [
        new Addon(id: "addon", version: "42-SNAPSHOT", unstable: true),
        new Addon(id: "addon", version: "42", unstable: false)].sort()
  }

  def "filterAddonsByVersion must keep stable and unstable versions (but without snapshots)"() {
    when:
    List<Addon> addons = [
        new Addon(id: "addon", version: "42-SNAPSHOT", unstable: true),
        new Addon(id: "addon", version: "42-alpha1", unstable: true),
        new Addon(id: "addon", version: "42", unstable: false)
    ]
    then:
    addonService.filterAddonsByVersion(addons, false, true).sort() == [
        new Addon(id: "addon", version: "42-alpha1", unstable: true),
        new Addon(id: "addon", version: "42", unstable: false)].sort()
  }

  def "filterAddonsByVersion must keep stable, unstable and snapshot versions"() {
    when:
    List<Addon> addons = [
        new Addon(id: "addon", version: "42-SNAPSHOT", unstable: true),
        new Addon(id: "addon", version: "42-alpha1", unstable: true),
        new Addon(id: "addon", version: "42", unstable: false)
    ]
    then:
    addonService.filterAddonsByVersion(addons, true, true).sort() == [
        new Addon(id: "addon", version: "42-SNAPSHOT", unstable: true),
        new Addon(id: "addon", version: "42-alpha1", unstable: true),
        new Addon(id: "addon", version: "42", unstable: false)].sort()
  }

  def "getCacheFilename must always return the same value for a given URL"() {
    when:
    def filename1 = addonService.getCacheFilename(new URL("http://www.exoplatform.com"))
    def filename2 = addonService.getCacheFilename(new URL("http://www.exoplatform.com"))
    then:
    filename1 == filename2
  }

  def "findNewerAddons must use version numbers to order and extract newer addons"() {
    when:
    // Unordered list of addons with the same id and different versions
    List<Addon> addons = [
        new Addon(id: "addon", version: "42-beta-01"),
        new Addon(id: "addon", version: "42-SNAPSHOT"),
        new Addon(id: "addon", version: "42-M1"),
        new Addon(id: "addon", version: "42-SP"),
        new Addon(id: "addon", version: "43-alpha-01"),
        new Addon(id: "addon", version: "42-RC1"),
        new Addon(id: "addon", version: "42"),
        new Addon(id: "addon", version: "42-alpha-01"),
        new Addon(id: "addon", version: "41"),
        new Addon(id: "addon", version: "43-SNAPSHOT")
    ]
    then:
    addonService.findNewerAddons(new Addon(id: "addon", version: "42-M1"), addons).sort() == [
        new Addon(id: "addon", version: "42-RC1"),
        new Addon(id: "addon", version: "42-SNAPSHOT"),
        new Addon(id: "addon", version: "42"),
        new Addon(id: "addon", version: "42-SP"),
        new Addon(id: "addon", version: "43-alpha-01"),
        new Addon(id: "addon", version: "43-SNAPSHOT")].sort()
  }

  def "findNewestAddon must use version numbers to find the newest addon"() {
    when:
    // Unordered list of addons with the same id and different versions
    List<Addon> addons = [
        new Addon(id: "addon", version: "42-beta-01"),
        new Addon(id: "addon", version: "42-SNAPSHOT"),
        new Addon(id: "addon", version: "42-M1"),
        new Addon(id: "addon", version: "42-SP"),
        new Addon(id: "addon", version: "43-alpha-01"),
        new Addon(id: "addon", version: "42-RC1"),
        new Addon(id: "addon", version: "42"),
        new Addon(id: "addon", version: "42-alpha-01"),
        new Addon(id: "addon", version: "41"),
        new Addon(id: "addon", version: "43-SNAPSHOT")
    ]
    then:
    addonService.findNewestAddon("addon", addons) == new Addon(id: "addon", version: "43-SNAPSHOT")
  }

  def "findNewestAddon must return null if no addon with the given id is present in the list"() {
    when:
    // Unordered list of addons with the same id and different versions
    List<Addon> addons = [
        new Addon(id: "addon", version: "42-beta-01"),
        new Addon(id: "addon", version: "42-SNAPSHOT"),
        new Addon(id: "addon", version: "42-M1"),
        new Addon(id: "addon", version: "42-SP"),
        new Addon(id: "addon", version: "43-alpha-01"),
        new Addon(id: "addon", version: "42-RC1"),
        new Addon(id: "addon", version: "42"),
        new Addon(id: "addon", version: "42-alpha-01"),
        new Addon(id: "addon", version: "41"),
        new Addon(id: "addon", version: "43-SNAPSHOT")
    ]
    then:
    addonService.findNewestAddon("addon2", addons) == null
  }

}
