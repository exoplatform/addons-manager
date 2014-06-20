package org.exoplatform.platform.am.utils

import org.exoplatform.platform.am.IntegrationTestsSpecification

/**
 * @author Arnaud Héritier <aheritier@exoplatform.com>
 */
class FileUtilsIT extends IntegrationTestsSpecification {

  def "[AM_CAT_01] The download mechanism must follow permanent redirects"() {
    setup:
    def originalFile = new File(testDataDir(), "catalog.json")
    def downloadedFile = File.createTempFile("test", ".json")
    when:
    FileUtils.downloadFile("${webServerRootUrl()}/catalog-redirect-301.jsp", downloadedFile)
    then:
    originalFile.text.equals(downloadedFile.text)
    cleanup:
    downloadedFile.delete()
  }

  def "[AM_CAT_01] The download mechanism must follow temporary redirects"() {
    setup:
    def originalFile = new File(testDataDir(), "catalog.json")
    def downloadedFile = File.createTempFile("test", ".json")
    when:
    FileUtils.downloadFile("${webServerRootUrl()}/catalog-redirect-302.jsp", downloadedFile)
    then:
    originalFile.text.equals(downloadedFile.text)
    cleanup:
    downloadedFile.delete()
  }

}
