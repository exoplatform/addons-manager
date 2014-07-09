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

/**
 * Misc constants
 * @author Arnaud Héritier <aheritier@exoplatform.com>
 */
class AddonsManagerConstants {
  /**
   * Command line return code when the program succeeded
   */
  static final int RETURN_CODE_OK = 0
  /**
   * Command line return code when the program failed without a known reason
   */
  static final int RETURN_CODE_UNKNOWN_ERROR = 1

  /**
   * Command line return code when command line parameters are invalid
   */
  static final int RETURN_CODE_INVALID_COMMAND_LINE_PARAMS = 2

  /**
   * Command line return code when an action is asked on an add-on not found in any catalog
   */
  static final int RETURN_CODE_ADDON_NOT_FOUND = 3

  /**
   * Command line return code when an invalid action is asked on an add-on not installed locally
   */
  static final int RETURN_CODE_ADDON_NOT_INSTALLED = 4

  /**
   * Command line return code when an invalid action is asked on an add-on already installed locally
   */
  static final int RETURN_CODE_ADDON_ALREADY_INSTALLED = 5

  /**
   * Command line return code when trying to install an add-on on a PLF version incompatible
   */
  static final int RETURN_CODE_INCOMPATIBILITY_ERROR = 6

  /**
   * Command line return code when we don't trap an error while reading a JSON file
   */
  static final int RETURN_CODE_INVALID_JSON = 7

  /**
   * Command line return code when the user doesn't accept the license
   */
  static final int RETURN_CODE_LICENSE_NOT_ACCEPTED = 8

  /**
   * Command line return code when there is a setup issue
   */
  static final int RETURN_CODE_ERRONEOUS_SETUP = 9

  static final String STATUS_OK = "OK"
  static final String STATUS_KO = "KO"
}
