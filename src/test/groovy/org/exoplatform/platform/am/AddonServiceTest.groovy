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
import org.exoplatform.platform.am.ex.CompatibilityException
import org.exoplatform.platform.am.ex.InvalidJSONException
import org.exoplatform.platform.am.settings.PlatformSettings
import spock.lang.Shared
import spock.lang.Subject
import spock.lang.Unroll

import static org.exoplatform.platform.am.settings.PlatformSettings.AppServerType.JBOSS
import static org.exoplatform.platform.am.settings.PlatformSettings.AppServerType.TOMCAT
import static org.exoplatform.platform.am.settings.PlatformSettings.DistributionType.COMMUNITY
import static org.exoplatform.platform.am.settings.PlatformSettings.DistributionType.ENTERPRISE
/**
 * @author Arnaud Héritier <aheritier@exoplatform.com>
 */
class AddonServiceTest extends UnitTestsSpecification {

  @Shared
  @Subject
  AddonService addonService = AddonService.getInstance()

  @Shared
  PlatformSettings plfCommunityTomcat
  @Shared
  PlatformSettings plfEnterpriseTomcat
  @Shared
  PlatformSettings plfEnterpriseJboss

  def setupSpec() {
    plfCommunityTomcat = Mock()
    plfCommunityTomcat.appServerType >> TOMCAT
    plfCommunityTomcat.distributionType >> COMMUNITY
    plfCommunityTomcat.version >> "4.1.0"
    plfEnterpriseTomcat = Mock()
    plfEnterpriseTomcat.appServerType >> TOMCAT
    plfEnterpriseTomcat.distributionType >> ENTERPRISE
    plfEnterpriseTomcat.version >> "4.1.0"
    plfEnterpriseJboss = Mock()
    plfEnterpriseJboss.appServerType >> JBOSS
    plfEnterpriseJboss.distributionType >> ENTERPRISE
    plfEnterpriseJboss.version >> "4.1.0"
  }

  def "createAddonFromJsonText parse a valid JSON text"() {
    when:
    def addon = addonService.createAddonFromJsonText("""
    {
        "id": "my-addon",
        "version": "1.0.0",
        "name": "The super addon",
        "downloadUrl": "http://path/to/archive.zip",
        "vendor": "eXo platform",
        "license": "LGPLv3",
        "supportedDistributions": "community,enterprise",
        "supportedApplicationServers": "tomcat,jboss"
    }
""")
    then:
    // Mustn't be null nor throw an exception
    addon == new Addon(
        id: "my-addon",
        version: "1.0.0",
        name: "The super addon",
        downloadUrl: "http://path/to/archive.zip",
        vendor: "eXo platform",
        license: "LGPLv3",
        supportedApplicationServers: [JBOSS, TOMCAT],
        supportedDistributions: [COMMUNITY, ENTERPRISE])
  }

  def "createAddonFromJsonText parse an invalid valid JSON text"() {
    when:
    addonService.createAddonFromJsonText("""
    {
        "version": "1.0.0",
        "name": "The super addon",
        "downloadUrl": "http://path/to/archive.zip",
        "vendor": "eXo platform",
        "license": "LGPLv3",
        "supportedDistributions": "community,enterprise",
        "supportedApplicationServers": "tomcat,jboss"
    }
""")
    then:
    thrown(InvalidJSONException)
  }

  def "createAddonsFromJsonText must silently ignore all invalid entries"() {
    when:
    def addons = addonService.createAddonsFromJsonText("""
[
    {
        "id": "my-addon",
        "version": "1.0.0",
        "name": "The super addon",
        "downloadUrl": "http://path/to/archive.zip",
        "vendor": "eXo platform",
        "license": "LGPLv3",
        "supportedDistributions": "community,enterprise",
        "supportedApplicationServers": "tomcat,jboss"
    },
    {
        "version": "1.0.0",
        "name": "The super addon",
        "downloadUrl": "http://path/to/archive.zip",
        "vendor": "eXo platform",
        "license": "LGPLv3",
        "supportedDistributions": "community,enterprise",
        "supportedApplicationServers": "tomcat,jboss"
    },
    {
        "id": "my-addon",
        "name": "The super addon",
        "downloadUrl": "http://path/to/archive.zip",
        "vendor": "eXo platform",
        "license": "LGPLv3",
        "supportedDistributions": "community,enterprise",
        "supportedApplicationServers": "tomcat,jboss"
    },
    {
        "id": "my-addon",
        "version": "1.0.0",
        "downloadUrl": "http://path/to/archive.zip",
        "vendor": "eXo platform",
        "license": "LGPLv3",
        "supportedDistributions": "community,enterprise",
        "supportedApplicationServers": "tomcat,jboss"
    },
    {
        "id": "my-addon",
        "version": "1.0.0",
        "name": "The super addon",
        "vendor": "eXo platform",
        "license": "LGPLv3",
        "supportedDistributions": "community,enterprise",
        "supportedApplicationServers": "tomcat,jboss"
    },
    {
        "id": "my-addon",
        "version": "1.0.0",
        "name": "The super addon",
        "downloadUrl": "http://path/to/archive.zip",
        "license": "LGPLv3",
        "supportedDistributions": "community,enterprise",
        "supportedApplicationServers": "tomcat,jboss"
    },
    {
        "id": "my-addon",
        "version": "1.0.0",
        "name": "The super addon",
        "downloadUrl": "http://path/to/archive.zip",
        "vendor": "eXo platform",
        "supportedDistributions": "community,enterprise",
        "supportedApplicationServers": "tomcat,jboss"
    },
    {
        "id": "my-addon",
        "version": "1.0.0",
        "name": "The super addon",
        "downloadUrl": "http://path/to/archive.zip",
        "vendor": "eXo platform",
        "license": "LGPLv3",
        "supportedApplicationServers": "tomcat,jboss"
    },
    {
        "id": "my-addon",
        "version": "1.0.0",
        "name": "The super addon",
        "downloadUrl": "http://path/to/archive.zip",
        "vendor": "eXo platform",
        "license": "LGPLv3",
        "supportedDistributions": "community,enterprise",
    }
]
""")
    then:
    // Mustn't be null nor throw an exception
    addons != null
    addons.size() == 1
    addons[0] == new Addon(
        id: "my-addon",
        version: "1.0.0",
        name: "The super addon",
        downloadUrl: "http://path/to/archive.zip",
        vendor: "eXo platform",
        license: "LGPLv3",
        supportedApplicationServers: [JBOSS, TOMCAT],
        supportedDistributions: [COMMUNITY, ENTERPRISE])
  }

  /**
   * [AM_CAT_07] At merge, de-duplication of add-on entries of the local and remote catalogs is done using  ID, Version,
   * Distributions, Application Servers as the identifier. In case of duplication, the remote entry takes precedence
   * TODO : Only ID+Version are used in comparison. It should take care of Distributions, Application Servers.
   */
  def "mergeCatalogs must implement [AM_CAT_07]"() {
    when:
    Addon remoteAddon1 = new Addon(id: "addon1", version: "41", name: "Remote #1")
    Addon remoteAddon2 = new Addon(id: "addon1", version: "42", name: "Remote #2")
    Addon remoteAddon3 = new Addon(id: "addon2", version: "42", name: "Remote #3")
    Addon localAddon1 = new Addon(id: "addon1", version: "41", name: "Local #1")
    Addon localAddon2 = new Addon(id: "addon1", version: "44", name: "Local #2")
    Addon localAddon3 = new Addon(id: "addon3", version: "42", name: "Local #3")
    List<Addon> remoteCatalog = [remoteAddon1, remoteAddon2, remoteAddon3]
    List<Addon> localCatalog = [localAddon1, localAddon2, localAddon3]
    then:
    addonService.mergeCatalogs(remoteCatalog,
                               localCatalog).sort() == [remoteAddon1, remoteAddon2, remoteAddon3, localAddon2, localAddon3].sort()
  }

  def "filterAddonsByVersion must keep only stable versions"() {
    when:
    List<Addon> addons = [
        new Addon(id: "addon", version: "42-SNAPSHOT", unstable: true),
        new Addon(id: "addon", version: "42-alpha1", unstable: true),
        new Addon(id: "addon", version: "42", unstable: false)
    ]
    then:
    addonService.filterAddonsByVersion(addons, true, false, false).sort() == [new Addon(id: "addon", version: "42",
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
    addonService.filterAddonsByVersion(addons, true, false, true).sort() == [
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
    addonService.filterAddonsByVersion(addons, true, true, false).sort() == [
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
    addonService.filterAddonsByVersion(addons, true, true, true).sort() == [
        new Addon(id: "addon", version: "42-SNAPSHOT", unstable: true),
        new Addon(id: "addon", version: "42-alpha1", unstable: true),
        new Addon(id: "addon", version: "42", unstable: false)].sort()
  }

  def "findAddonsNewerThan must use version numbers to order and extract newer add-ons"() {
    when:
    // Unordered list of add-ons with the same id and different versions
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
    addonService.findAddonsNewerThan(new Addon(id: "addon", version: "42-M1"), addons).sort() == [
        new Addon(id: "addon", version: "42-RC1"),
        new Addon(id: "addon", version: "42-SNAPSHOT"),
        new Addon(id: "addon", version: "42"),
        new Addon(id: "addon", version: "42-SP"),
        new Addon(id: "addon", version: "43-alpha-01"),
        new Addon(id: "addon", version: "43-SNAPSHOT")].sort()
  }

  def "findNewestAddon must use version numbers to find the newest addon"() {
    when:
    // Unordered list of add-ons with the same id and different versions
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
    // Unordered list of add-ons with the same id and different versions
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

  def "convertUrlToFilename must always return the same value for a given URL"() {
    when:
    def filename1 = addonService.convertUrlToFilename(new URL("http://www.exoplatform.com"))
    def filename2 = addonService.convertUrlToFilename(new URL("http://www.exoplatform.com"))
    then:
    filename1 == filename2
  }

  @Unroll
  def "validateCompatibility must throw an error for the addon #addon.supportedApplicationServers,#addon.supportedDistributions,#addon.compatibility on PLF #platformSettings"(PlatformSettings platformSettings, Addon addon) {
    when:
    addonService.validateCompatibility(addon, platformSettings)
    then:
    thrown CompatibilityException
    where:
    platformSettings    | addon
    plfCommunityTomcat  | new Addon(supportedApplicationServers: [JBOSS],
                                    supportedDistributions: [COMMUNITY],
                                    compatibility: "[4.1,)")// Wrong application Server
    plfEnterpriseTomcat | new Addon(supportedApplicationServers: [JBOSS],
                                    supportedDistributions: [ENTERPRISE],
                                    compatibility: "[4.1,)")// Wrong application Server
    plfEnterpriseJboss  | new Addon(supportedApplicationServers: [TOMCAT],
                                    supportedDistributions: [ENTERPRISE],
                                    compatibility: "[4.1,)")// Wrong application Server
    plfCommunityTomcat  | new Addon(supportedApplicationServers: [TOMCAT],
                                    supportedDistributions: [ENTERPRISE],
                                    compatibility: "[4.1,)")// Wrong distribution
    plfEnterpriseTomcat | new Addon(supportedApplicationServers: [TOMCAT],
                                    supportedDistributions: [COMMUNITY],
                                    compatibility: "[4.1,)")// Wrong distribution
    plfEnterpriseJboss  | new Addon(supportedApplicationServers: [JBOSS],
                                    supportedDistributions: [COMMUNITY],
                                    compatibility: "[4.1,)")// Wrong distribution
    plfCommunityTomcat  | new Addon(supportedApplicationServers: [TOMCAT],
                                    supportedDistributions: [COMMUNITY],
                                    compatibility: "[4.2,)")// Wrong version
    plfEnterpriseTomcat | new Addon(supportedApplicationServers: [TOMCAT],
                                    supportedDistributions: [ENTERPRISE],
                                    compatibility: "[4.2,)")// Wrong version
    plfEnterpriseJboss  | new Addon(supportedApplicationServers: [JBOSS],
                                    supportedDistributions: [ENTERPRISE],
                                    compatibility: "[4.2,)")// Wrong version
  }

  @Unroll
  def "validateCompatibility must be ok for the addon #addon.supportedApplicationServers,#addon.supportedDistributions,#addon.compatibility on PLF #platformSettings"(PlatformSettings platformSettings, Addon addon) {
    when:
    addonService.validateCompatibility(addon, platformSettings)
    then:
    notThrown CompatibilityException
    where:
    platformSettings    | addon
    plfCommunityTomcat  | new Addon(supportedApplicationServers: [TOMCAT],
                                    supportedDistributions: [COMMUNITY],
                                    compatibility: "[4.1,)")
    plfEnterpriseTomcat | new Addon(supportedApplicationServers: [TOMCAT],
                                    supportedDistributions: [ENTERPRISE],
                                    compatibility: "[4.1,)")
    plfEnterpriseJboss  | new Addon(supportedApplicationServers: [JBOSS],
                                    supportedDistributions: [ENTERPRISE],
                                    compatibility: "[4.1,)")
  }
}
