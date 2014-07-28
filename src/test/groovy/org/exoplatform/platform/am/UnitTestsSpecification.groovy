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

import org.exoplatform.platform.am.utils.Logger
import spock.lang.Specification

/**
 * This class offers various helpers methods to write unit tests
 * @author Arnaud Héritier <aheritier@exoplatform.com>
 */
abstract class UnitTestsSpecification extends Specification {

  public static final String UT_SYSPROP_VERBOSE = "unit.tests.verbose"

  /**
   * @return true if verbose mode must be used while executing tests
   */
  boolean isVerbose() {
    Boolean.getBoolean(UT_SYSPROP_VERBOSE)
  }

  def setup() {
    if (verbose) {
      Logger.getInstance().enableDebug()
    } else {
      Logger.getInstance().disableDebug()
    }
  }

  def cleanup() {
    Logger.getInstance().disableDebug()
  }

}