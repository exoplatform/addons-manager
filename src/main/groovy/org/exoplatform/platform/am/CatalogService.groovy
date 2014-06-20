package org.exoplatform.platform.am

/**
 * @author Arnaud Héritier <aheritier@exoplatform.com>
 */
class CatalogService {
  List<Addon> mergeCatalogs(final List<Addon> centralCatalog, final List<Addon> localCatalog) {
    // [AM_CAT_07] At merge, de-duplication of add-on entries of the local and remote catalogs is
    // done using ID, Version, Distributions, Application Servers as the identifier.
    // In case of duplication, the remote entry takes precedence
    List<Addon> mergedCatalog = centralCatalog.clone()
    localCatalog.findAll { !centralCatalog.contains(it) }.each { mergedCatalog.add(it) }
    return mergedCatalog
  }
}
