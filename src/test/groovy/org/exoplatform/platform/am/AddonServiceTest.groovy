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
import spock.lang.Issue
import spock.lang.Shared
import spock.lang.Subject
import spock.lang.Unroll

import static java.lang.Boolean.FALSE
import static java.lang.Boolean.TRUE
import static org.exoplatform.platform.am.settings.PlatformSettings.AppServerType.JBOSS
import static org.exoplatform.platform.am.settings.PlatformSettings.AppServerType.TOMCAT
import static org.exoplatform.platform.am.settings.PlatformSettings.AppServerType.BITNAMI
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
  PlatformSettings plfCommunityTomcat41
  @Shared
  PlatformSettings plfEnterpriseTomcat41
  @Shared
  PlatformSettings plfEnterpriseJboss41
  @Shared
  PlatformSettings plfCommunityBitnami41
  @Shared
  PlatformSettings plfEnterpriseBitnami41
  @Shared
  Addon addon1 = new Addon(id: "addon1", version: "42", supportedApplicationServers: [TOMCAT],
                           supportedDistributions: [COMMUNITY],
                           compatibility: "[4.1,)")
  @Shared
  Addon addon2 = new Addon(id: "addon2", version: "42", supportedApplicationServers: [TOMCAT],
                           supportedDistributions: [ENTERPRISE],
                           compatibility: "[4.1,)")
  @Shared
  Addon addon3 = new Addon(id: "addon3", version: "42", supportedApplicationServers: [JBOSS],
                           supportedDistributions: [ENTERPRISE],
                           compatibility: "[4.1,)")
  @Shared
  Addon addon4 = new Addon(id: "addon4", version: "42", supportedApplicationServers: [TOMCAT],
                           supportedDistributions: [COMMUNITY],
                           compatibility: "[4.2,)")
  @Shared
  Addon addon5 = new Addon(id: "addon5", version: "42", supportedApplicationServers: [TOMCAT],
                           supportedDistributions: [ENTERPRISE],
                           compatibility: "[4.2,)")
  @Shared
  Addon addon6 = new Addon(id: "addon6", version: "42", supportedApplicationServers: [JBOSS],
                           supportedDistributions: [ENTERPRISE],
                           compatibility: "[4.2,)")
  @Shared
  Addon addon7 = new Addon(id: "addon7", version: "42", supportedApplicationServers: [TOMCAT, JBOSS],
                           supportedDistributions: [ENTERPRISE, COMMUNITY],
                           compatibility: "[4.1,)")
  @Shared
  Addon addon_42_beta_01 = new Addon(id: "addon", version: "42-beta-01", unstable: true)
  @Shared
  Addon addon_42_SNAPSHOT = new Addon(id: "addon", version: "42-SNAPSHOT", unstable: true)
  @Shared
  Addon addon_42_M1 = new Addon(id: "addon", version: "42-M1", unstable: true)
  @Shared
  Addon addon_42_SP = new Addon(id: "addon", version: "42-SP", unstable: true)
  @Shared
  Addon addon_43_alpha_01 = new Addon(id: "addon", version: "43-alpha-01", unstable: true)
  @Shared
  Addon addon_42_RC1 = new Addon(id: "addon", version: "42-RC1", unstable: true)
  @Shared
  Addon addon_42 = new Addon(id: "addon", version: "42")
  @Shared
  Addon addon_42_alpha_01 = new Addon(id: "addon", version: "42-alpha-01", unstable: true)
  @Shared
  Addon addon_41 = new Addon(id: "addon", version: "41")
  @Shared
  Addon addon_43_SNAPSHOT = new Addon(id: "addon", version: "43-SNAPSHOT", unstable: true)

  def setupSpec() {
    plfCommunityTomcat41 = Mock()
    plfCommunityTomcat41.appServerType >> TOMCAT
    plfCommunityTomcat41.distributionType >> COMMUNITY
    plfCommunityTomcat41.version >> "4.1.0"
    plfEnterpriseTomcat41 = Mock()
    plfEnterpriseTomcat41.appServerType >> TOMCAT
    plfEnterpriseTomcat41.distributionType >> ENTERPRISE
    plfEnterpriseTomcat41.version >> "4.1.0"
    plfEnterpriseJboss41 = Mock()
    plfEnterpriseJboss41.appServerType >> JBOSS
    plfEnterpriseJboss41.distributionType >> ENTERPRISE
    plfEnterpriseJboss41.version >> "4.1.0"
    
    plfCommunityBitnami41 = Mock()
    plfCommunityBitnami41.appServerType >> BITNAMI
    plfCommunityBitnami41.distributionType >> COMMUNITY
    plfCommunityBitnami41.version >> "4.1.0"
    plfEnterpriseBitnami41 = Mock()
    plfEnterpriseBitnami41.appServerType >> BITNAMI
    plfEnterpriseBitnami41.distributionType >> ENTERPRISE
    plfEnterpriseBitnami41.version >> "4.1.0"    
  }

  def "createAddonFromJsonText parse a valid JSON text"() {
    when:
    def addon = addonService.createAddonFromJsonText("""
    {
        "id": "my-addon",
        "version": "1.0.0",
        "name": "The super add-on",
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
        version: "1.0.0")
  }

  def "createAddonFromJsonText parse a valid JSON text with warnings"() {
    when:
    def addon = addonService.createAddonFromJsonText("""
    {
        "id": "my-addon",
        "version": "1.0.0",
        "name": "The super add-on",
        "downloadUrl": "http://path/to/archive.zip",
        "vendor": "eXo platform",
        "license": "LGPLv3",
        "supportedDistributions": "community,enterprise,foo",
        "supportedApplicationServers": "tomcat,jboss,bar",
        "sourceUrl":"foo",
        "screenshotUrl":"foo",
        "thumbnailUrl":"foo",
        "documentationUrl":"foo"
    }
""")
    then:
    // Mustn't be null nor throw an exception
    addon == new Addon(
        id: "my-addon",
        version: "1.0.0")
  }

  def "createAddonFromJsonText parse an invalid JSON text"() {
    when:
    addonService.createAddonFromJsonText("""
    {
        "version": "1.0.0",
        "name": "The super add-on",
        "downloadUrl": "http://path/to/archive.zip",
        "vendor": "eXo platform",
        "license": "LGPLv3",
        "supportedDistributions": "foo",
        "supportedApplicationServers": "bar"
    }
""")
    then:
    thrown(InvalidJSONException)
  }

  def "createAddonFromJsonText parse an invalid JSON text containing İ in Turkish"() {
    when:
    addonService.createAddonFromJsonText("""
    {
        "id": "my-addon",
        "version": "1.0.0",
        "name": "The super add-on Turkish",
        "downloadUrl": "http://path/to/archive.zip",
        "vendor": "eXo platform",
        "license": "LGPLv3",
        "supportedDistributions": "ENTERPRİSE,COMMUNİTY",
        "supportedApplicationServers": "TOMCAT,JBOSS"
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
        "name": "The super add-on",
        "downloadUrl": "http://path/to/archive.zip",
        "vendor": "eXo platform",
        "license": "LGPLv3",
        "supportedDistributions": "community,enterprise",
        "supportedApplicationServers": "tomcat,jboss"
    },
    {
        "version": "1.0.0",
        "name": "The super add-on",
        "downloadUrl": "http://path/to/archive.zip",
        "vendor": "eXo platform",
        "license": "LGPLv3",
        "supportedDistributions": "community,enterprise",
        "supportedApplicationServers": "tomcat,jboss"
    },
    {
        "id": "my-addon",
        "name": "The super add-on",
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
        "name": "The super add-on",
        "vendor": "eXo platform",
        "license": "LGPLv3",
        "supportedDistributions": "community,enterprise",
        "supportedApplicationServers": "tomcat,jboss"
    },
    {
        "id": "my-addon",
        "version": "1.0.0",
        "name": "The super add-on",
        "downloadUrl": "http://path/to/archive.zip",
        "license": "LGPLv3",
        "supportedDistributions": "community,enterprise",
        "supportedApplicationServers": "tomcat,jboss"
    },
    {
        "id": "my-addon",
        "version": "1.0.0",
        "name": "The super add-on",
        "downloadUrl": "http://path/to/archive.zip",
        "vendor": "eXo platform",
        "supportedDistributions": "community,enterprise",
        "supportedApplicationServers": "tomcat,jboss"
    },
    {
        "id": "my-addon",
        "version": "1.0.0",
        "name": "The super add-on",
        "downloadUrl": "http://path/to/archive.zip",
        "vendor": "eXo platform",
        "license": "LGPLv3",
        "supportedApplicationServers": "tomcat,jboss"
    },
    {
        "id": "my-addon",
        "version": "1.0.0",
        "name": "The super add-on",
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
        version: "1.0.0")
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
    expect:
    addonService.filterAddonsByVersion(
        [addon_42_SNAPSHOT, addon_42_alpha_01, addon_42], true, false, false) == [addon_42]
  }

  def "filterAddonsByVersion must keep stable and snapshot versions"() {
    expect:
    addonService.filterAddonsByVersion(
        [addon_42_SNAPSHOT, addon_42_alpha_01, addon_42], true, false, true).sort() == [addon_42_SNAPSHOT, addon_42].sort()
  }

  def "filterAddonsByVersion must keep stable and unstable versions (but without snapshots)"() {
    expect:
    addonService.filterAddonsByVersion(
        [addon_42_SNAPSHOT, addon_42_alpha_01, addon_42], true, true, false).sort() == [addon_42_alpha_01, addon_42].sort()
  }

  def "filterAddonsByVersion must keep stable, unstable and snapshot versions"() {
    expect:
    addonService.filterAddonsByVersion(
        [addon_42_SNAPSHOT, addon_42_alpha_01, addon_42
        ], true, true, true).sort() == [addon_42_SNAPSHOT, addon_42_alpha_01, addon_42].sort()
  }

  def "findAddonsNewerThan must use version numbers to order and extract newer add-ons"() {
    expect:
    // Unordered list of add-ons with the same id and different versions
    addonService.findAddonsNewerThan(addon_42_M1, [
        addon_42_beta_01,
        addon_42_SNAPSHOT,
        addon_42_M1,
        addon_42_SP,
        addon_43_alpha_01,
        addon_42_RC1,
        addon_42,
        addon_42_alpha_01,
        addon_41,
        addon_43_SNAPSHOT
    ]).sort() == [
        addon_42_RC1,
        addon_42_SNAPSHOT,
        addon_42,
        addon_42_SP,
        addon_43_alpha_01,
        addon_43_SNAPSHOT].sort()
  }

  def "findNewestAddon must use version numbers to find the newest addon"() {
    expect:
    // Unordered list of add-ons with the same id and different versions
    addonService.findNewestAddon("addon", [
        addon_42_beta_01,
        addon_42_SNAPSHOT,
        addon_42_M1,
        addon_42_SP,
        addon_43_alpha_01,
        addon_42_RC1,
        addon_42,
        addon_42_alpha_01,
        addon_41,
        addon_43_SNAPSHOT
    ]) == addon_43_SNAPSHOT
  }

  def "findNewestAddon must return null if no addon with the given id is present in the list"() {
    expect:
    // Unordered list of add-ons with the same id and different versions
    addonService.findNewestAddon("addon2", [
        addon_42_beta_01,
        addon_42_SNAPSHOT,
        addon_42_M1,
        addon_42_SP,
        addon_43_alpha_01,
        addon_42_RC1,
        addon_42,
        addon_42_alpha_01,
        addon_41,
        addon_43_SNAPSHOT
    ]) == null
  }

  def "convertUrlToFilename must always return the same value for a given URL"() {
    when:
    def filename1 = addonService.convertUrlToFilename(new URL("http://www.exoplatform.com"))
    def filename2 = addonService.convertUrlToFilename(new URL("http://www.exoplatform.com"))
    then:
    filename1 == filename2
  }

  @Unroll
  def "validateCompatibility must throw an error for the addon #addon.id, #addon.supportedApplicationServers,#addon.supportedDistributions,#addon.compatibility on PLF #platformSettings"(PlatformSettings platformSettings, Addon addon) {
    when:
    addonService.validateCompatibility(addon, platformSettings)
    then:
    thrown CompatibilityException
    where:
    platformSettings      | addon
    plfCommunityTomcat41  | addon2
    plfCommunityTomcat41  | addon3
    plfCommunityTomcat41  | addon4
    plfCommunityTomcat41  | addon5
    plfCommunityTomcat41  | addon6
    plfEnterpriseTomcat41 | addon1
    plfEnterpriseTomcat41 | addon3
    plfEnterpriseTomcat41 | addon4
    plfEnterpriseTomcat41 | addon5
    plfEnterpriseTomcat41 | addon6
    plfEnterpriseJboss41  | addon1
    plfEnterpriseJboss41  | addon2
    plfEnterpriseJboss41  | addon4
    plfEnterpriseJboss41  | addon5
    plfEnterpriseJboss41  | addon6
    plfCommunityBitnami41 | addon2
    plfCommunityBitnami41 | addon3
    plfCommunityBitnami41 | addon4
    plfCommunityBitnami41 | addon5
    plfCommunityBitnami41 | addon6
    plfEnterpriseBitnami41 | addon1
    plfEnterpriseBitnami41 | addon3
    plfEnterpriseBitnami41 | addon4
    plfEnterpriseBitnami41 | addon5
    plfEnterpriseBitnami41 | addon6
  }

  @Unroll
  def "validateCompatibility must be ok for the addon #addon.id, #addon.supportedApplicationServers,#addon.supportedDistributions,#addon.compatibility on PLF #platformSettings"(PlatformSettings platformSettings, Addon addon) {
    when:
    addonService.validateCompatibility(addon, platformSettings)
    then:
    notThrown CompatibilityException
    where:
    platformSettings      | addon
    plfCommunityTomcat41  | addon1
    plfEnterpriseTomcat41 | addon2
    plfEnterpriseJboss41  | addon3
    plfCommunityBitnami41 | addon1
    plfEnterpriseBitnami41 | addon2
    plfCommunityTomcat41  | addon7
    plfEnterpriseTomcat41 | addon7
    plfEnterpriseJboss41  | addon7
    plfCommunityBitnami41 | addon7
    plfEnterpriseBitnami41 | addon7
  }

  def "filterCompatibleAddons for PLF Community Tomcat"() {
    expect:
    addonService.filterCompatibleAddons([addon1, addon2, addon3, addon4, addon5, addon6], plfCommunityTomcat41) == [addon1]
  }

  def "filterCompatibleAddons for PLF Enterprise Tomcat"() {
    expect:
    addonService.filterCompatibleAddons([addon1, addon2, addon3, addon4, addon5, addon6], plfEnterpriseTomcat41) == [addon2]
  }

  def "filterCompatibleAddons for PLF Enterprise Jboss"() {
    expect:
    addonService.filterCompatibleAddons([addon1, addon2, addon3, addon4, addon5, addon6], plfEnterpriseJboss41) == [addon3]
  }

  def "filterCompatibleAddons for PLF Community Bitnami"() {
    expect:
    addonService.filterCompatibleAddons([addon1, addon2, addon3, addon4, addon5, addon6], plfCommunityBitnami41) == [addon1]
  }

  def "filterCompatibleAddons for PLF Enterprise Bitnami"() {
    expect:
    addonService.filterCompatibleAddons([addon1, addon2, addon3, addon4, addon5, addon6], plfEnterpriseBitnami41) == [addon2]
  }

  def "filterCompatibleAddons for PLF Community Tomcat with 2 results"() {
    expect:
    !addonService.filterCompatibleAddons([addon1, addon2, addon3, addon4, addon5, addon6, addon7], plfCommunityTomcat41).disjoint([addon1, addon7])
  }

  def "filterCompatibleAddons for PLF Enterprise Tomcat with 2 results"() {
    expect:
    !addonService.filterCompatibleAddons([addon1, addon2, addon3, addon4, addon5, addon6, addon7], plfEnterpriseTomcat41).disjoint([addon2, addon7])
  }

  def "filterCompatibleAddons for PLF Enterprise Jboss with 2 results"() {
    expect:
    !addonService.filterCompatibleAddons([addon1, addon2, addon3, addon4, addon5, addon6, addon7], plfEnterpriseJboss41).disjoint([addon3, addon7])
  }

  def "filterCompatibleAddons for PLF Community Bitnami with 2 results"() {
    expect:
    !addonService.filterCompatibleAddons([addon1, addon2, addon3, addon4, addon5, addon6, addon7], plfCommunityBitnami41).disjoint([addon1, addon7])
  }

  def "filterCompatibleAddons for PLF Enterprise Bitnami with 2 results"() {
    expect:
    !addonService.filterCompatibleAddons([addon1, addon2, addon3, addon4, addon5, addon6, addon7], plfEnterpriseBitnami41).disjoint([addon2, addon7])
  }

  @Unroll
  @Issue("https://jira.exoplatform.org/browse/AM-105")
  def "testVersionCompatibility"(String version, String constraint, Boolean expectedResult) {
    expect:
    addonService.testVersionCompatibility(version, constraint) == expectedResult
    where:
    version          | constraint          | expectedResult
    "4.0.7"          | "[4.1.x,)"          | FALSE // 4.0 < 4.1
    "4.1-M2"         | "[4.1.x,)"          | FALSE // 4.1.x > 4.1 because .x is a String. 4.1 > 4.1-M2 (Milestone)
    "4.0.7"          | "(4.0.999,)"        | FALSE // We exclude everything under 4.0.999
    "4.1-M2"         | "(4.0.999,)"        | TRUE  // We exclude everything under 4.0.999 and thus include 4.1 betas, milestones..
    "4.1.x-SNAPSHOT" | "[4.1.x,)"          | FALSE // A SNAPSHOT is < than its release
    "4.1.x-SNAPSHOT" | "[4.1,)"            | TRUE  // 4.1.x > 4.1 thus its SNAPSHOT too
    "4.0.7"          | "[4.0.0,)"          | TRUE  // 4.0.7 is in [4.0.0,)
    "4.0.7"          | "[4.0.0,),[4.1-M2]" | TRUE  // 4.0.7 is in [4.0.0,)
    "4.1-M2"         | "[4.0.0,)"          | TRUE  // 4.1-M2 is in [4.0.0,)
    "4.1-M2"         | "[4.0.0,),[4.1-M2]" | TRUE  // 4.1-M2 is in [4.0.0,) and [4.1-M2]
    "4.1.x-SNAPSHOT" | "[4.0.0,)"          | TRUE  // 4.1.x-SNAPSHOT is in [4.0.0,)
    "4.1.x-SNAPSHOT" | "[4.0.0,),[4.1-M2]" | TRUE  // 4.1.x-SNAPSHOT is in [4.0.0,)
  }
}
