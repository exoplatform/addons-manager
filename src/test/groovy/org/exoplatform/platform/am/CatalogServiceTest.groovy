package org.exoplatform.platform.am

import spock.lang.Shared
import spock.lang.Specification

/**
 * @author Arnaud Héritier <aheritier@exoplatform.com>
 */
class CatalogServiceTest extends Specification {

  @Shared
  CatalogService catalogService = new CatalogService()

  /**
   * It is possible to place a local catalog under addons/local.json, this catalog will be merged with the central.
   * At merge, de-duplication of add-on entries of the local and remote catalogs is done using id:version as the identifier.
   * In case of duplication, the remote entry takes precedence
   */
  def "[AM_CAT_05] It is possible to place a local catalog under addons/local.json, this catalog will be merged with the central."() {

  }
}
