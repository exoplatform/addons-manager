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
package org.exoplatform.platform.am.utils

import org.exoplatform.platform.am.ex.AddonsManagerException
import org.exoplatform.platform.am.ex.UnknownErrorException

import java.nio.channels.FileChannel

/**
 * Miscellaneous utilities
 */
class FileUtils {

  /**
   * Logger
   */
  private static final Logger LOG = Logger.getInstance()

  /**
   * Downloads a file following redirects if required
   * @param message the logging message to display
   * @param url The URL from which to download
   * @param destFile The file to populate
   * @throws IOException If there is an IO error
   */
  static downloadFile(String message, URL url, File destFile) throws IOException {
    downloadFile(message, url.toString(), destFile)
  }

  /**
   * Downloads a file following redirects if required
   * @param message the logging message to display
   * @param url The URL from which to download
   * @param destFile The file to populate
   * @throws AddonsManagerException If there is an error while transferring the file
   */
  static downloadFile(String message, String url, File destFile) throws AddonsManagerException {
    String originalUrl = url
    // Let's do it for each redirection
    while (url) {
      new URL(url).openConnection().with { URLConnection conn ->
        if (conn instanceof HttpURLConnection) {
          conn.instanceFollowRedirects = true
        }
        url = conn.getHeaderField("Location")
        // No more Location, let's download
        if (!url) {
          if (destFile.exists()) {
            LOG.debug("remoteFile lastModified : ${conn.lastModified}")
            LOG.debug("remoteFile size : ${conn.contentLength}")
            LOG.debug("destFile lastModified : ${destFile.lastModified()}")
            LOG.debug("destFile size : ${destFile.size()}")
          }
          // Same size and date more recent locally, don't touch it.
          if (destFile.exists() && conn.contentLength == destFile.size() && conn.lastModified <= destFile.lastModified()) {
            LOG.withStatusOK("File ${destFile.name} already up-to-date. Skipping download.")
            return
          }
          if (!message) {
            message = "Downloading ${originalUrl} to ${destFile}"
          }
          LOG.withStatus(message) {
            try {
              destFile.withOutputStream { out ->
                conn.inputStream.with { inp ->
                  out << inp
                  inp?.close()
                }
              }
            } catch (FileNotFoundException fnfe) {
              // AM-95 : Don't keep an empty/corrupted downloaded file
              if (destFile.exists()) {
                destFile.delete()
              }
              throw new UnknownErrorException("File not found at URL ${originalUrl}", fnfe)
            } catch (IOException ioe) {
              // AM-95 : Don't keep an empty/corrupted downloaded file
              if (destFile.exists()) {
                destFile.delete()
              }
              throw new UnknownErrorException("I/O error while downloading ${originalUrl}", ioe)
            }
          }
        }
      }
    }
  }

  /**
   * Copy a local file to another location using NIO
   * @param message the logging message to display
   * @param sourceFile the source file to copy
   * @param destFile where the file should be copied
   * @throws IOException If there is an IO error
   */
  static void copyFile(String message, File sourceFile, File destFile) throws IOException {
    copyFile(message, sourceFile, destFile, true)
  }

  /**
   * Copy a local file to another location using NIO
   * @param message the logging message to display
   * @param sourceFile the source file to copy
   * @param destFile where the file should be copied
   * @param warnIfOverride Display a warning if destFile already exists
   * @throws IOException If there is an IO error
   */
  static void copyFile(String message, File sourceFile, File destFile, boolean warnIfOverride) throws IOException {
    if (!destFile.exists()) {
      destFile.createNewFile();
    } else {
      LOG.debug("sourceFile lastModified : ${sourceFile.lastModified()}")
      LOG.debug("sourceFile size : ${sourceFile.size()}")
      LOG.debug("destFile lastModified : ${destFile.lastModified()}")
      LOG.debug("destFile size : ${destFile.size()}")
      // Same size and destFile date more recent, don't touch it.
      if (sourceFile.size() == destFile.size() && sourceFile.lastModified() <= destFile.lastModified()) {
        LOG.withStatusOK("Skipping copy of ${sourceFile.name}. File ${destFile.name} already up-to-date.")
        return
      }
      if (warnIfOverride) {
        LOG.warn("File ${destFile.name} already exists. Replacing it.")
      }
    }
    if (!message) {
      message = "Copying ${sourceFile} to ${destFile}"
    }
    LOG.withStatus(message) {
      FileChannel source = null;
      FileChannel destination = null;

      try {
        source = new FileInputStream(sourceFile).getChannel();
        destination = new FileOutputStream(destFile).getChannel();
        destination.transferFrom(source, 0, source.size());
      }
      finally {
        if (source != null) {
          source.close();
        }
        if (destination != null) {
          destination.close();
        }
      }
    }
  }

  /**
   * Create a directory and its required parents.
   * @param dirToCreate The directory to create
   * @throws IOException If an error occurs
   */
  static void mkdirs(File dirToCreate) throws IOException {
    if (!dirToCreate.mkdirs()) {
      throw new IOException("Unable to create directory ${dirToCreate}")
    }
  }

  static String extractFilename(String fullpath) {
    return fullpath.substring(fullpath.lastIndexOf('/') + 1, fullpath.length())
  }

  static String extractDirPath(String fullpath) {
    return fullpath.substring(0, fullpath.lastIndexOf('/'))
  }

  static String extractParentAndFilename(String fullpath) {
    if (fullpath.lastIndexOf("/") < 0) return fullpath
    String subPath = fullpath.substring(0, fullpath.lastIndexOf("/") -1)
    if (subPath.indexOf("/") < 0) {
      return fullpath
    } else {
      return fullpath.substring(subPath.lastIndexOf("/") + 1)
    }
  }

}
