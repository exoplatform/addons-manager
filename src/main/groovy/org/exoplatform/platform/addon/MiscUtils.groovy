/**
 * Copyright (C) 2003-2014 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.exoplatform.platform.addon

import java.nio.channels.FileChannel
import java.util.zip.ZipInputStream

/**
 * Miscellaneous utilities
 */
class MiscUtils {
  /**
   * Downloads a file following redirects if required
   * @param url The URL from which to download
   * @param destFile The file to populate
   * @throws IOException If there is an IO error
   */
  static downloadFile(String url, File destFile) throws IOException {
    while (url) {
      new URL(url).openConnection().with { conn ->
        conn.instanceFollowRedirects = false
        url = conn.getHeaderField("Location")
        if (!url) {
          destFile.withOutputStream { out ->
            conn.inputStream.with { inp ->
              out << inp
              inp.close()
            }
          }
        }
      }
    }
  }

  /**
   * Copy a local file to another location using NIO
   * @param sourceFile the source file to copy
   * @param destFile where the file should be copied
   * @throws IOException If there is an IO error
   */
  static void copyFile(File sourceFile, File destFile) throws IOException {
    if (!destFile.exists()) {
      destFile.createNewFile();
    } else {
      Logging.displayMsgWarn("${destFile.name} already exists. Replacing it.")
    }

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

  /**
   * Extract from the zip all files matching the pattern into a given directory without taking care of the directories used in the zip
   * @param zipToExtract
   * @param destinationDir
   * @param pattern
   * @throws IOException
   */
  static List<String> flatExtractFromZip(File zipToExtract, File destinationDir, String pattern) throws IOException {
    def result = new ArrayList<String>()
    def zipInputStream = new ZipInputStream(new FileInputStream(zipToExtract))
    if (!destinationDir.exists()) {
      mkdirs(destinationDir)
    }
    zipInputStream.withStream {
      def entry
      while (entry = zipInputStream.nextEntry) {
        if (!entry.isDirectory() && entry.name =~ pattern) {
          String filename = extractFilename(entry.name)
          Logging.logWithStatus("Installing file ${filename} ...") {
            def output = new FileOutputStream(new File(destinationDir, filename))
            output.withStream {
              int len = 0;
              byte[] buffer = new byte[4096]
              while ((len = zipInputStream.read(buffer)) > 0) {
                output.write(buffer, 0, len);
              }
            }
          }
          result.add(filename)
        }
      }
    }
    result
  }

  static String extractFilename(String fullpath) {
    return fullpath.substring(fullpath.lastIndexOf('/') + 1, fullpath.length())
  }


}